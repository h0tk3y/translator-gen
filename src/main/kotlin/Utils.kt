/**
 * Some useful things
 *
 * Created by igushs on 6/14/2015.
 */

public inline fun <T, R> T.after(f: (T) -> R): T {
    f(this)
    return this
}