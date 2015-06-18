import Attribute
import EOF
import EPSILON
import FirstFollow
import FromToChars
import Generator
import GeneratorException
import Lexeme
import Literal
import Rule
import UserGrammar
import UserGrammarBaseVisitor
import UserGrammarLexer
import UserGrammarParser
import UserGrammarParser.RangeLiteralContext
import after
import extractUserGrammar
import getFirstFollow
import org.antlr.v4.analysis.AnalysisPipeline
import org.antlr.v4.codegen.CodeGenerator
import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.tool.Grammar
import parserCode
import java.io.File
import java.io.InputStream
import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet
import kotlin.text.Regex

/**
 * Reads user grammar, issues lexer and translator for it.
 *
 * Created by igushs on 6/13/2015.
 */

fun main(args: Array<String>) {
    if (args.size() < 1) {
        println("Usage: program Grammar.g4")
    } else {
        val filename = args[0]
        val inputStream = File(filename).inputStream()
        Generator(inputStream).process()
    }
}

public class GeneratorException(msg: String, val grammar: UserGrammar) : Exception(msg)

public class Generator(val input: InputStream) {
    val stringInput = input.bufferedReader().readText()

    val grammar = extractUserGrammar(stringInput)
    val antlrGrammar = Grammar(stringInput)

    fun process() {
        AnalysisPipeline(antlrGrammar).process()
        if (antlrGrammar.tool.errMgr.errors > 0)
            throw GeneratorException("Bad grammar.", grammar)
        val lexer = antlrGrammar.implicitLexer
        CodeGenerator(lexer).generateLexer().write(File("${grammar.name}Lexer.java"), null)
        val (first, follow) = getFirstFollow(grammar)
        File("${grammar.name}Translator.kt").writer().use { it.write(parserCode(grammar, first, follow)) }
    }
}

fun extractUserGrammar(input: String): UserGrammar {
    val antlrInput = ANTLRInputStream(input)
    val lexer = UserGrammarLexer(antlrInput)
    val tokens = CommonTokenStream(lexer)
    val parser = UserGrammarParser(tokens)
    val root = parser.grammarSpec()

    val grammarName = root.ID().getText()

    val rightPartsByLeft = HashMap<Lexeme, ArrayList<Rule>>()
    fun rightParts(s: Lexeme) = rightPartsByLeft[s] ?: ArrayList<Rule>() after { rightPartsByLeft[s] = it }

    val attrs = HashMap<Lexeme, ArrayList<Attribute>>()

    var firstLexeme: Lexeme? = null

    root.accept(object : UserGrammarBaseVisitor<Unit>() {
        override fun visitRuleSpec(ctx: UserGrammarParser.RuleSpecContext) {
            val leftPart = Lexeme(ctx.ID().getText())
            firstLexeme = firstLexeme ?: leftPart

            val returnsSpecContext = ctx.returnsSpec()
            if (returnsSpecContext != null) {
                for (r in returnsSpecContext.oneReturn()) {
                    (attrs.getOrPut(leftPart) { ArrayList<Attribute>() }) add Attribute(r.name.getText(), r.type.getText())
                }
            }

            val rules = rightParts(leftPart)

            for (b in ctx.body()) {
                rules add
                        Rule(leftPart,
                                when {
                                    b.literal() != null -> b.literal().let {
                                        listOf(if (it is RangeLiteralContext)
                                            FromToChars(it.strChar(0).getText()[0], it.strChar(1).getText()[0]) else
                                            Literal(it.getText())
                                        )
                                    }
                                    b.ID() != null -> b.ID() map { Lexeme(it.getText()) }
                                    else -> emptyList<Lexeme>()
                                },
                                b.block()?.str()?.getText())
            }
        }
    })

    val terms = rightPartsByLeft.keySet().filter {
        rightPartsByLeft[it].all { it.rightPart.all { it is Literal || it is FromToChars } }
    }.toSet()

    val nonTerms = rightPartsByLeft.keySet().subtract(terms)

    return UserGrammar(grammarName, firstLexeme!!, nonTerms, attrs, terms, rightPartsByLeft)
}

public data class FirstFollow(val first: Map<Lexeme, Set<Lexeme>>,
                              val follow: Map<Lexeme, Set<Lexeme>>)

fun getFirstFollow(g: UserGrammar): FirstFollow {
    val nonTermRules = g.rules filterKeys { it in g.nonTerminals } flatMap { it.getValue() }

    var changed = true

    val first = (g.nonTerminals  map { it to HashSet<Lexeme>() }).toMap()
    fun addToFirst(to: Lexeme, l: Lexeme) = changed after {
        changed = first[to]!! add l || changed
    }

    while (changed) {
        changed = false
        for (r in nonTermRules) {
            val a = r.leftPart

            if (r.isEpsilonRule) {
                addToFirst(a, EPSILON)
            } else {
                if (r.rightPart.first() in g.terminals) {
                    addToFirst(a, r.rightPart[0])
                } else {
                    val firstNonEpsilon = r.rightPart.indexOfFirst { it in g.terminals || EPSILON !in first[it]!! }
                    for (i in 0..firstNonEpsilon) {
                        val p = r.rightPart[i]
                        if (p in g.terminals)
                            addToFirst(a, p)
                        else
                            for (f in first[p]!!)
                                addToFirst(a, f)
                    }
                }
            }
        }
    }

    val follow = ((g.nonTerminals + g.terminals) map { it to HashSet<Lexeme>() }).toMap()
    fun addToFollow(to: Lexeme, l: Lexeme) = changed after {
        changed = (follow[to]?.add(l) ?: false) || changed
    }

    fun effectiveFirst(l: Lexeme) = (if (l in g.terminals) hashSetOf(l) else first[l]!!)

    addToFollow(g.start, EOF)

    while (changed) {
        changed = false
        for (r in nonTermRules) {
            for (i in r.rightPart.indices) {
                val b = r.rightPart[i]

                for (j in i + 1..r.rightPart.indices.end) {
                    val gamma = r.rightPart[j]
                    val eFirst = effectiveFirst(gamma)
                    eFirst.forEach { if (it != EPSILON) addToFollow(b, it) }
                    if (EPSILON !in eFirst)
                        break
                    if (j == r.rightPart.indices.end)
                        follow[r.leftPart]!!.forEach { addToFollow(b, it) }
                }

                if (i == r.rightPart.indices.end || EPSILON in effectiveFirst(r.rightPart[i + 1])) {
                    follow[r.leftPart]!!.forEach { if (it != EPSILON) addToFollow(b, it) }
                }
            }
        }
    }

    return FirstFollow(first, follow)
}

fun parserCode(g: UserGrammar, first: Map<Lexeme, Set<Lexeme>>, follow: Map<Lexeme, Set<Lexeme>>): String {
    val result = StringBuilder()
    var indent = 0
    fun writeln(s: String) {
        for (i in 0..indent - 1) result.append("    ");
        result.append("$s\n")
    }

    fun indented(block: () -> Unit) {
        indent++; block(); indent--
    }

    val firstByRule = g.rules
            .flatMap { it.value }
            .filter { it.leftPart in g.nonTerminals }
            .map {
                val f = HashSet<Lexeme>()
                val firstNonEpsilon = it.rightPart.indexOfFirst { it in g.terminals || EPSILON !in first[it]!! }
                for (i in 0..firstNonEpsilon) {
                    val l = it.rightPart[i]
                    if (l in g.terminals)
                        f add l
                    else
                        f addAll first[l]!!
                }
                it to f
            }.toMap()

    val nodeClassName = "${g.name}Node"
    val lexerClassName = "${g.name}Lexer"

    writeln("import $lexerClassName.*")
    writeln("import java.util.*")
    writeln("import org.antlr.v4.runtime.*")
    writeln("")

    writeln("public data class $nodeClassName(val name: String) {")
    indented {
        writeln("public val children: MutableList<$nodeClassName> = ArrayList()")
        writeln("public val attrs: HashMap<String, Any?> = HashMap()")
        writeln("public fun addChild(n: $nodeClassName) { children add n }")
    }
    writeln("}")
    writeln("")

    writeln("public class ${g.name}Translator(val lexer: $lexerClassName) {")
    indented {
        writeln("fun nextToken(): Token { curToken = lexer.nextToken(); return curToken }")
        writeln("var curToken: Token = nextToken()")
        writeln("fun consume(type: Int): String { ")
        indented {
            writeln("if (curToken.getType() != type) throw RuntimeException()")
            writeln("val result = curToken.getText()")
            writeln("nextToken()")
            writeln("return result")
        }
        writeln("}")
        writeln("")

        fun injectedCode(r: Rule) =
                r.code?.replace(Regex("([0-9]+)#(\\w+)(\\b|\\\\w)"),
                        {
                            val index = it.groups[1]!!.value.toInt()
                            val lex = r.rightPart[index]
                            val attr = it.groups[2]!!.value

                            var type = g.attributes[lex]?.firstOrNull { it.name == attr }?.type
                            if (type == null && lex in g.terminals && attr == "text")
                                type = "String"
                            if (type != null)
                                "(result.children[$index].attrs[\"$attr\"] as $type)"
                            else
                                it.value
                        })


        for (nt in g.nonTerminals) {
            writeln("fun ${nt.string}(): $nodeClassName {")
            indented {
                writeln("val result = $nodeClassName(\"${nt.string}\")")
                g.attributes[nt]?.forEach {
                    val (name, type) = it
                    writeln("var $name: $type? = null")
                }
                writeln("when (curToken.getType()) {")
                indented {
                    for (r in g.rules[nt]!! filter { !it.isEpsilonRule }) {
                        writeln("${firstByRule[r]!!.map { it.string }.joinToString(", ")} -> {")
                        indented {
                            var i = 0
                            for (p in r.rightPart) when (p) {
                                in g.terminals -> {
                                    writeln("val child$i = $nodeClassName(consume($lexerClassName.${p.string}))")
                                    writeln("child$i.attrs[\"text\"] = child$i.name")
                                    writeln("result addChild child$i")
                                    i++
                                }
                                else -> writeln("result.children add ${p.string}()")
                            }
                            if (r.code != null) {
                                writeln(injectedCode(r)!!)
                            }
                        }
                        writeln("}")
                    }
                    if (firstByRule filterKeys { it.leftPart == nt } any { it.value.isEmpty() }) {
                        writeln("${follow[nt]!!.joinToString(", ")} -> {")
                        val rule = g.rules[nt]!!.first { it.isEpsilonRule}
                        if (rule.code != null)
                            indented { writeln(rule.code) }
                        writeln("}")
                    }
                    writeln("else -> throw RuntimeException(\"Unexpected token.\")")
                }
                writeln("}")
                g.attributes[nt]?.forEach {
                    val (name, type) = it
                    writeln("result.attrs[\"$name\"] = $name")
                }
                writeln("return result")
            }
            writeln("}")
            writeln("")
        }
    }
    writeln("}")

    return result.toString()
}