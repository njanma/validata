package njanma.validata

import kotlin.reflect.KFunction1
import kotlin.reflect.KProperty1

interface Validator<T, E> {

    fun validate(value: T): ValidationResult<E>

    operator fun invoke(value: T) = validate(value)

    companion object {
        operator fun <T, E> invoke(init: ValidationBuilder<T, E>.() -> Unit): Validation<T, E> =
                { DefaultValidationBuilder<T, E>(it).apply(init).build()(it) }

        operator fun <T, E> invoke(default: Validator<T, E>,
                                   actionOnComplete: () -> Unit = {},
                                   init: ValidationBuilder<T, E>.() -> Unit): Validation<T, E> =
                {
                    val result = DefaultValidationBuilder(it, mutableListOf(default)).apply(init).build().invoke(it)
                    result.ifPresent { actionOnComplete() }
                    result
                }
    }
}

internal class PropertyValidator<T, R, E>(
        private val property: KProperty1<T, R>,
        private val validation: (R) -> ValidationResult<E>
) : Validator<T, E> {
    override fun validate(value: T): ValidationResult<E> =
            validation(property(value))
}

internal class IfPresentValidator<T, R, E>(
        private val property: KProperty1<T, R?>,
        private val validation: (R) -> ValidationResult<E>
) : Validator<T, E> {
    override fun validate(value: T): ValidationResult<E> {
        val propertyValue: R? = property(value)
        return if (propertyValue != null) {
            validation(propertyValue)
        } else ValidationResult.valid()
    }
}

internal class FunctionValidator<T, R, E>(
        private val func: KFunction1<T, R>,
        private val validation: Validation<R, E>
) : Validator<T, E> {
    override fun validate(value: T): ValidationResult<E> =
            validation(func(value))
}

internal class FunctionIfPresentValidator<T, R, E>(
        private val func: KFunction1<T, R?>,
        private val validation: Validation<R, E>
) : Validator<T, E> {
    override fun validate(value: T): ValidationResult<E> {
        val res = func(value)
        return if (res != null) {
            validation(res)
        } else ValidationResult.valid()
    }
}

internal class EqValidator<T, R, E>(private val fst: R,
                                    private val errFunc: (T) -> E = { throw InvalidData() },
                                    private val reverse: Boolean = false
) : Validator<T, E> {
    override fun validate(value: T): ValidationResult<E> =
            ValidationResult(if (reverse) fst == value else fst != value, errFunc(value))
}

internal class NotBlankValidator<T, E>(private val arg: String,
                                       private val errFunc: (T) -> E = { throw InvalidData() }
) : Validator<T, E> {
    override fun validate(value: T): ValidationResult<E> =
            ValidationResult(arg.isBlank(), errFunc(value))
}

internal class HasLengthValidator<T, E, N : Number>(private val arg: N,
                                                    private val min: N?,
                                                    private val max: N?,
                                                    private val minErrFunc: (T) -> E = { throw InvalidData() },
                                                    private val maxErrFunc: (T) -> E = { throw InvalidData() }
) : Validator<T, E> {
    override fun validate(value: T): ValidationResult<E> =
            when {
                min?.let { arg.toInt() < it.toInt() } == true ->
                    ValidationResult.Invalid(minErrFunc(value))
                max?.let { arg.toInt() > it.toInt() } == true ->
                    ValidationResult.Invalid(maxErrFunc(value))
                else -> ValidationResult.valid()
            }
}

internal class MatcherValidator<T, E>(private val matcher: Matcher<T, E>) : Validator<T, E> {
    override fun validate(value: T): ValidationResult<E> {
        val matcherResult = matcher.test(value)
        return ValidationResult(!matcherResult.passed, matcherResult.failureExtractor(value))
    }
}

internal class ExternalValidator<T, E>(private val validation: Validation<T, E>) : Validator<T, E> {
    override fun validate(value: T): ValidationResult<E> = validation(value)
}

internal class SeqValidator<T, E>(private val validations: List<Validator<T, E>>) : Validator<T, E> {
    override fun validate(value: T): ValidationResult<E> =
            validations.map { it.validate(value) }.flatten()
}

