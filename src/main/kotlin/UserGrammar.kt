import java.util.*

/**
 * Represents user grammar as a set of rules.
 *
 * Created by igushs on 6/13/2015.
 */

public open data class Lexeme(val string: String) {
    override fun toString() = string
}

public class Literal(value: String) : Lexeme(value)
public class FromToChars(val from: Char, val to: Char) : Lexeme("'$from'..'$to'")

public val EPSILON: Lexeme = Lexeme("ε")
public val EOF: Lexeme = Lexeme("Token.EOF")

public open data class Rule(val leftPart: Lexeme,
                            val rightPart: List<Lexeme>,
                            val code: String?
) {
    override fun toString() = "$leftPart -> ${rightPart.joinToString(" ")}"

    public val isEpsilonRule: Boolean get() = rightPart.isEmpty()
}

public data class Attribute(val name: String, val type: String)

public class UserGrammar(val name: String,
                         val start: Lexeme,
                         val nonTerminals: Set<Lexeme>,
                         val attributes: Map<Lexeme, List<Attribute>>,
                         val terminals: Set<Lexeme>,
                         val rules: Map<Lexeme, List<Rule>>
) {

}