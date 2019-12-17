package njanma.validata

typealias Validation<T, E> = (T) -> ValidationResult<E>

sealed class ValidationResult<T>(val errors: List<T>) {

    object Valid : ValidationResult<Nothing>(emptyList())

    data class Invalid<E>(private val errs: List<E>) : ValidationResult<E>(errs) {
        constructor(err: E) : this(listOf(err))
    }

    companion object {

        fun <E> invalid(err: E): ValidationResult<E> = Invalid(err)

        @Suppress("UNCHECKED_CAST")
        fun <E> valid(): ValidationResult<E> = Valid as ValidationResult<E>

        operator fun <E> invoke(invalidCondition: Boolean, errorDefinition: E): ValidationResult<E> =
                if (invalidCondition)
                    invalid(errorDefinition)
                else
                    valid()
    }
}