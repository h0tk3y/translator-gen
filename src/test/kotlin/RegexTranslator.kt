import RegexLexer.*
import java.util.*
import org.antlr.v4.runtime.*

public data class RegexNode(val name: String) {
    public val children: MutableList<RegexNode> = ArrayList()
    public val attrs: HashMap<String, Any?> = HashMap()
    public fun addChild(n: RegexNode) { children add n }
}

public class RegexTranslator(val lexer: RegexLexer) {
    fun nextToken(): Token { curToken = lexer.nextToken(); return curToken }
    var curToken: Token = nextToken()
    fun consume(type: Int): String { 
        if (curToken.getType() != type) throw RuntimeException()
        val result = curToken.getText()
        nextToken()
        return result
    }
    
    fun t(): RegexNode {
        val result = RegexNode("t")
        var a: Int? = null
        when (curToken.getType()) {
            C, LBR -> {
                result.children add n()
                result.children add x()
                result.children add d()
                a=(result.children[0].attrs["a"] as Int??:0)+(result.children[1].attrs["a"] as Int??:0)+(result.children[2].attrs["a"] as Int??:0)
            }
            else -> throw RuntimeException("Unexpected token.")
        }
        result.attrs["a"] = a
        return result
    }
    
    fun d(): RegexNode {
        val result = RegexNode("d")
        var a: Int? = null
        when (curToken.getType()) {
            C, LBR -> {
                result.children add t()
                a=result.children[0].attrs["a"] as Int??:0
            }
            OR, RBR, Token.EOF -> {}
            else -> throw RuntimeException("Unexpected token.")
        }
        result.attrs["a"] = a
        return result
    }
    
    fun start(): RegexNode {
        val result = RegexNode("start")
        var a: Int? = null
        when (curToken.getType()) {
            C, LBR -> {
                result.children add t()
                result.children add z()
                a=(result.children[0].attrs["a"] as Int??:0)+(result.children[1].attrs["a"] as Int??:0)
            }
            else -> throw RuntimeException("Unexpected token.")
        }
        result.attrs["a"] = a
        return result
    }
    
    fun x(): RegexNode {
        val result = RegexNode("x")
        var a: Int? = null
        when (curToken.getType()) {
            AST -> {
                val child0 = RegexNode(consume(RegexLexer.AST))
                child0.attrs["text"] = child0.name
                result addChild child0
                a=1
            }
            C, OR, RBR, Token.EOF, LBR -> {}
            else -> throw RuntimeException("Unexpected token.")
        }
        result.attrs["a"] = a
        return result
    }
    
    fun z(): RegexNode {
        val result = RegexNode("z")
        var a: Int? = null
        when (curToken.getType()) {
            OR -> {
                val child0 = RegexNode(consume(RegexLexer.OR))
                child0.attrs["text"] = child0.name
                result addChild child0
                result.children add start()
                a=(result.children[1].attrs["a"] as Int??:0)+1
            }
            RBR, Token.EOF -> {}
            else -> throw RuntimeException("Unexpected token.")
        }
        result.attrs["a"] = a
        return result
    }
    
    fun n(): RegexNode {
        val result = RegexNode("n")
        var a: Int? = null
        when (curToken.getType()) {
            C -> {
                val child0 = RegexNode(consume(RegexLexer.C))
                child0.attrs["text"] = child0.name
                result addChild child0
                a=1
            }
            LBR -> {
                val child0 = RegexNode(consume(RegexLexer.LBR))
                child0.attrs["text"] = child0.name
                result addChild child0
                result.children add start()
                val child1 = RegexNode(consume(RegexLexer.RBR))
                child1.attrs["text"] = child1.name
                result addChild child1
                a=2+(result.children[1].attrs["a"] as Int??:0)
            }
            else -> throw RuntimeException("Unexpected token.")
        }
        result.attrs["a"] = a
        return result
    }
    
}
