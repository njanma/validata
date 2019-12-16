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

fun <E> hasLength(size: Int, failure: E, negative: E) =
        hasLength(size).transform(failure, negative)

fun hasLength(size: Int) = Matcher<String, String> {
    MatcherResult(it.length == size, "Length should be $size!", "Length shouldn't be $size!")
}

fun <E> minLength(size: Int, failure: E, negative: E) =
        minLength(size).transform(failure, negative)

fun minLength(size: Int) = Matcher<String, String> {
    MatcherResult(it.length >= size, "Length should be more than $size!", "Length should be less than $size!")
}

fun <E> maxLength(size: Int, failure: E, negative: E) = maxLength(size).transform(failure, negative)

fun maxLength(size: Int) = !minLength(size)

fun <T> beEmpty() = Matcher<Optional<T>, String> { MatcherResult(it.isPresent, "Optional isn't empty!", "Optional is empty!") }