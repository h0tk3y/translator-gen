import org.antlr.v4.runtime.ANTLRInputStream
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test as test

/**
 * Created by igushs on 6/18/2015.
 */

public class GeneratedCodeTests {
    test fun regex() {
        val lexer = RegexLexer(ANTLRInputStream("ab*c|de(fg*h)xx*"))
        val translator = RegexTranslator(lexer)
        val t = translator.start()
        assertEquals(16, t.attrs["a"] as Int)
    }

    test fun arithmetic() {
        val lexer = ArithmLexer(ANTLRInputStream("2+2*2+(3*3*3)+1"))
        val translator = ArithmTranslator(lexer)
        val t = translator.start()
        assertEquals(34, t.attrs["v"])
    }
}