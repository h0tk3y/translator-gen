import ArithmLexer.*
import java.util.*
import org.antlr.v4.runtime.*

public data class ArithmNode(val name: String) {
    public val children: MutableList<ArithmNode> = ArrayList()
    public val attrs: HashMap<String, Any?> = HashMap()
    public fun addChild(n: ArithmNode) { children add n }
}

public class ArithmTranslator(val lexer: ArithmLexer) {
    fun nextToken(): Token { curToken = lexer.nextToken(); return curToken }
    var curToken: Token = nextToken()
    fun consume(type: Int): String { 
        if (curToken.getType() != type) throw RuntimeException()
        val result = curToken.getText()
        nextToken()
        return result
    }
    
    fun t(): ArithmNode {
        val result = ArithmNode("t")
        var v: Int? = null
        when (curToken.getType()) {
            LBR, N -> {
                result.children add f()
                result.children add d()
                 v = (result.children[0].attrs["v"] as Int) * ((result.children[1].attrs["v"] as Int?) ?: 1);
            }
            else -> throw RuntimeException("Unexpected token.")
        }
        result.attrs["v"] = v
        return result
    }
    
    fun d(): ArithmNode {
        val result = ArithmNode("d")
        var v: Int?? = null
        when (curToken.getType()) {
            MULT -> {
                val child0 = ArithmNode(consume(ArithmLexer.MULT))
                child0.attrs["text"] = child0.name
                result addChild child0
                result.children add f()
                result.children add d()
                 v = (result.children[1].attrs["v"] as Int) * ((result.children[2].attrs["v"] as Int?) ?: 1);
            }
            RBR, Token.EOF, PLUS -> {
                 v = 1;
            }
            else -> throw RuntimeException("Unexpected token.")
        }
        result.attrs["v"] = v
        return result
    }
    
    fun e(): ArithmNode {
        val result = ArithmNode("e")
        var v: Int?? = null
        when (curToken.getType()) {
            PLUS -> {
                val child0 = ArithmNode(consume(ArithmLexer.PLUS))
                child0.attrs["text"] = child0.name
                result addChild child0
                result.children add t()
                result.children add e()
                 v = (result.children[1].attrs["v"] as Int) + ((result.children[2].attrs["v"] as Int?) ?: 0);
            }
            RBR, Token.EOF -> {
            }
            else -> throw RuntimeException("Unexpected token.")
        }
        result.attrs["v"] = v
        return result
    }
    
    fun f(): ArithmNode {
        val result = ArithmNode("f")
        var v: Int? = null
        when (curToken.getType()) {
            N -> {
                val child0 = ArithmNode(consume(ArithmLexer.N))
                child0.attrs["text"] = child0.name
                result addChild child0
                 v = (result.children[0].attrs["text"] as String).toInt();
            }
            LBR -> {
                val child0 = ArithmNode(consume(ArithmLexer.LBR))
                child0.attrs["text"] = child0.name
                result addChild child0
                result.children add start()
                val child1 = ArithmNode(consume(ArithmLexer.RBR))
                child1.attrs["text"] = child1.name
                result addChild child1
                 v = (result.children[1].attrs["v"] as Int);
            }
            else -> throw RuntimeException("Unexpected token.")
        }
        result.attrs["v"] = v
        return result
    }
    
    fun start(): ArithmNode {
        val result = ArithmNode("start")
        var v: Int? = null
        when (curToken.getType()) {
            LBR, N -> {
                result.children add t()
                result.children add e()
                 v = (result.children[0].attrs["v"] as Int) + ((result.children[1].attrs["v"] as Int?) ?: 0);
            }
            else -> throw RuntimeException("Unexpected token.")
        }
        result.attrs["v"] = v
        return result
    }
    
}
