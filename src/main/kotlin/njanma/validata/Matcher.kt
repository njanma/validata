package njanma.validata

interface Matcher<T, E> {

    fun test(value: T): MatcherResult<T, E>

    operator fun not(): Matcher<T, E> = object : Matcher<T, E> {
        override fun test(value: T): MatcherResult<T, E> {
            val result = this@Matcher.test(value)
            return MatcherResult(!result.passed, result.negativeExtractor, result.failureExtractor)
        }
    }

    fun <R> transform(failure: R, negative: R): Matcher<T, R> = object : Matcher<T, R> {
        override fun test(value: T): MatcherResult<T, R> {
            val result = this@Matcher.test(value)
            return MatcherResult(result.passed, { failure }, { negative })
        }
    }

    companion object {
        operator fun <T, E> invoke(test: (T) -> MatcherResult<T, E>): Matcher<T, E> = object : Matcher<T, E> {
            override fun test(value: T): MatcherResult<T, E> = test(value)
        }
    }
}

data class MatcherResult<T, E>(
        /**
         * Should be true if the result was valid
         */
        val passed: Boolean,
        /**
         * An error which describes why the evaluation failed
         */
        val failureExtractor: (T) -> E,
        /**
         * An error which describes why the evaluation failed
         * when matcher is used in the negative sense
         */
        val negativeExtractor: (T) -> E) {
    constructor(passed: Boolean, failureMessage: E, negatedFailureMessage: E) : this(passed, { failureMessage }, { negatedFailureMessage })
    constructor(passed: Boolean, failureMessage: E) : this(passed, { failureMessage }, { throw MatherError() })
}

class MatherError : RuntimeException("Matcher shouldn't used as negated!")