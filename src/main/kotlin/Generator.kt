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
import java.io.File
import java.io.InputStream
import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet

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
        val firstFollow = getFirstFollow(grammar)

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

    var firstLexeme: Lexeme? = null

    root.accept(object : UserGrammarBaseVisitor<Unit>() {
        override fun visitRuleSpec(ctx: UserGrammarParser.RuleSpecContext) {
            val leftPart = Lexeme(ctx.ID().getText())
            firstLexeme = firstLexeme ?: leftPart

            val rules = rightParts(leftPart)

            for (b in ctx.body()) {
                rules add Rule(leftPart, when {
                    b.literal() != null -> b.literal().let {
                        listOf(if (it is RangeLiteralContext)
                            FromToChars(it.strChar(0).getText()[0], it.strChar(1).getText()[0]) else
                            Literal(it.getText())
                        )
                    }
                    b.ID() != null -> b.ID() map { Lexeme(it.getText()) }
                    else -> emptyList<Lexeme>()
                })
            }
        }
    })

    val terms = rightPartsByLeft.keySet().filter {
        rightPartsByLeft[it].all { it.rightPart.all { it is Literal || it is FromToChars } }
    }.toSet()

    val nonTerms = rightPartsByLeft.keySet().subtract(terms)

    return UserGrammar(grammarName, firstLexeme!!, nonTerms, terms, rightPartsByLeft)
}

public data class FirstFollow(val first: Map<Lexeme, Set<Lexeme>>,
                              val follow: Map<Lexeme, Set<Lexeme>>)

fun getFirstFollow(g: UserGrammar): FirstFollow {
    val nonTermRules = g.rules filterKeys { it in g.nonTerminals } flatMap { it.getValue() }

    var changed = true

    val first = (g.nonTerminals  map { it to HashSet<Lexeme>() }).toMap()
    fun addToFirst(to: Lexeme, l: Lexeme) = changed after { changed = first[to]!! add l }

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

    val follow = (g.nonTerminals map { it to HashSet<Lexeme>() }).toMap()
    fun addToFollow(to: Lexeme, l: Lexeme) = changed after { changed = follow[to]!! add l }

    addToFollow(g.start, EOF)

    while (changed) {
        for (r in nonTermRules) {
            for (i in r.rightPart.indices) {
                val b = r.rightPart[i]
                if (b in g.terminals)
                    continue

                if (i + 1 in r.rightPart.indices) {
                    val gamma = r.rightPart[i + 1]
                    if (gamma in g.nonTerminals) {
                        first[gamma]?.forEach { if (it != EPSILON) addToFollow(b, it) }
                    } else if (gamma in g.terminals) {
                        addToFollow(b, gamma)
                    }
                }

                if (i == r.rightPart.indices.end || first[r.rightPart[i + 1]]?.contains(EPSILON) ?: false) {
                    follow[r.leftPart]!!.forEach { addToFollow(b, it) }
                }
            }
        }
    }

    return FirstFollow(first, follow)
}

fun parserCode(g: UserGrammar, first: Map<Lexeme, Set<Lexeme>>, follow: Map<Lexeme, Set<Lexeme>>): String {
    val result = StringBuilder()
    fun writeln(s: String) = result.append("$s\n")

    val nodeClassName = "${g.name}Node"

    writeln("""public data class $nodeClassName(val name: String) {
                   public val children: List<$nodeClassName> = ArrayList()
                   fun addChild(n: $nodeClassName) { children add n }
               }
           """)

    val nonTermRules = g.rules filterKeys { it in g.nonTerminals } flatMap { it.getValue() }

    writeln("public class ${g.name}Translator(val lexer: ${g.name}Lexer) {")
    for (nt in g.nonTerminals) {

    }
    writeln("}")

    return result.toString()
}