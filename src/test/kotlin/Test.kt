import org.antlr.v4.runtime.ANTLRInputStream
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test as test

/**
 * Created by igushs on 6/18/2015.
 */

public class GeneratedCodeTests {
    test fun regex() {
        val lexer = RegexLexer(ANTLRInputStream("ab*c|de(fg*h)xx*|s"))
        val translator = RegexTranslator(lexer)
        val t = translator.start()
        assertEquals(18, t.attrs["a"] as Int)
    }

    test fun arithmetic() {
        val lexer = ArithmLexer(ANTLRInputStream("20+2*2+(3*3*3)+10"))
        val translator = ArithmTranslator(lexer)
        assertEquals("20", lexer._token.getText())
        val t = translator.start()
        assertEquals(61, t.attrs["v"])
    }
}