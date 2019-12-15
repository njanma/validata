package njanma.validata

import java.util.*

fun <E> Iterable<ValidationResult<E>>.flatten(): ValidationResult<E> =
        if (!iterator().hasNext())
            ValidationResult.valid()
        else reduce { acc, res ->
            when (res) {
                is ValidationResult.Invalid -> ValidationResult.Invalid(acc.errors + res.errors)
                is ValidationResult.Valid -> acc
            }
        }

fun <E> ValidationResult<E>.ifPresent(f: (List<E>) -> Unit) {
    when (this) {
        is ValidationResult.Invalid -> f(errors)
    }
}

infix operator fun <E> ValidationResult<E>.plus(other: ValidationResult<E>): ValidationResult<E> =
        (listOf(this) + listOf(other)).flatten()

fun <E> hasLength(size: Int, err: E) = Matcher<String, E> { MatcherResult(it.length == size, err, err) }
fun <T> beEmpty() = Matcher<Optional<T>, String> { MatcherResult(it.isPresent, "Optional isn't empty!", "Optional is empty!") }