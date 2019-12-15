package njanma.validata

interface Matcher<T, E> {

    fun test(value: T): MatcherResult<E>

    fun invert(): Matcher<T, E> = object : Matcher<T, E> {
        override fun test(value: T): MatcherResult<E> {
            val result = this@Matcher.test(value)
            return MatcherResult(!result.passed, result.negatedFailureMessage, result.failureMessage)
        }
    }

    companion object {
        operator fun <T, E> invoke(test: (T) -> MatcherResult<E>): Matcher<T, E> =
                object : Matcher<T, E> {
                    override fun test(value: T): MatcherResult<E> =
                            test(value)
                }
    }
}

data class MatcherResult<E>(
        /**
         * Should be true if the result was valid
         */
        val passed: Boolean,
        /**
         * A message which describes why the evaluation failed
         */
        val failureMessage: E,
        /**
         * A message which describes why the evaluation failed
         * when matcher is used in the negative sense
         */
        val negatedFailureMessage: E)