package njanma.validata

import kotlin.reflect.KFunction1
import kotlin.reflect.KProperty1

abstract class ValidationBuilder<T, E> {
    abstract val self: T

    abstract operator fun <R> KProperty1<T, R>.invoke(init: ValidationBuilder<R, E>.() -> Unit)
    abstract operator fun <R> KFunction1<T, R>.invoke(init: ValidationBuilder<R, E>.() -> Unit)

    abstract infix fun <R> KProperty1<T, R>.checkedBy(validator: Validation<R, E>)
    abstract infix fun <R> KFunction1<T, R>.checkedBy(validator: Validation<R, E>)

    abstract fun <R> KProperty1<T, R>.checkedIf(condition: Boolean, init: ValidationBuilder<R, E>.() -> Unit)

    abstract infix fun <R> KProperty1<T, R>.should(arg: Matcher<R, E>)
    abstract infix fun <R> KFunction1<T, R>.should(arg: Matcher<R, E>)
    abstract infix fun <R> KProperty1<T, R>.shouldNot(arg: Matcher<R, E>)
    abstract infix fun <R> KFunction1<T, R>.shouldNot(arg: Matcher<R, E>)
    abstract infix fun <R> KProperty1<T, R>.shouldBe(arg: R)
    abstract infix fun <R> KProperty1<T, R>.shouldNotBe(arg: R)

    abstract fun <R> KProperty1<T, R>.shouldBe(arg: R, err: (R) -> E = { throw InvalidData() }, doAfter: () -> Unit = {})
    abstract fun <R> KProperty1<T, R>.shouldBe(arg: R, err: E, doAfter: () -> Unit = {})
    abstract fun <R> KProperty1<T, R>.shouldNotBe(arg: R, err: (R) -> E = { throw InvalidData() }, doAfter: () -> Unit = {})

    abstract infix fun <R> KFunction1<T, R>.shouldBe(arg: R)
    abstract fun <R> KFunction1<T, R>.shouldBe(arg: R, err: E, doAfter: () -> Unit = {})
    abstract fun <R> KFunction1<T, R>.shouldBe(arg: R, err: (R) -> E = { throw InvalidData() }, doAfter: () -> Unit = {})

    abstract infix fun <R> KProperty1<T, R?>.ifPresent(init: ValidationBuilder<R, E>.() -> Unit)
    abstract infix fun <R> KFunction1<T, R?>.ifPresent(init: ValidationBuilder<R, E>.() -> Unit)

    abstract fun ValidationBuilder<T, E>.checkedBy(validator: Validation<T, E>)

    abstract fun ValidationBuilder<String, E>.notBlank(errFunc: (T) -> E = { throw InvalidData() })
    abstract fun ValidationBuilder<String, E>.notBlank(err: E)

    abstract fun <N : Number> ValidationBuilder<N, E>.moreThan(min: Int, errFunc: (T) -> E = { throw InvalidData() })
    abstract fun <N : Number> ValidationBuilder<N, E>.moreThan(min: Int, err: E)

    abstract fun <N : Number> ValidationBuilder<N, E>.lessThan(max: Int, errFunc: (T) -> E = { throw InvalidData() })
    abstract fun <N : Number> ValidationBuilder<N, E>.lessThan(max: Int, err: E)
}

class DefaultValidationBuilder<T, E>(override val self: T,
                                     private val validators: MutableList<Validator<T, E>> = mutableListOf()
) : ValidationBuilder<T, E>() {

    override fun <R> KProperty1<T, R>.invoke(init: ValidationBuilder<R, E>.() -> Unit) {
        validators.add(PropertyValidator(this, Validator(init)))
    }

    override fun <R> KFunction1<T, R>.invoke(init: ValidationBuilder<R, E>.() -> Unit) {
        validators.add(FunctionValidator(this, Validator(init)))
    }

    override fun <R> KProperty1<T, R>.checkedBy(validator: Validation<R, E>) {
        validators.add(PropertyValidator(this, validator))
    }

    override fun <R> KFunction1<T, R>.checkedBy(validator: Validation<R, E>) {
        validators.add(FunctionValidator(this, validator))
    }

    override fun <R> KProperty1<T, R>.checkedIf(condition: Boolean, init: ValidationBuilder<R, E>.() -> Unit) {
        if (condition) {
            validators.add(PropertyValidator(this, Validator(init)))
        }
    }

    override fun <R> KProperty1<T, R?>.ifPresent(init: ValidationBuilder<R, E>.() -> Unit) {
        validators.add(IfPresentValidator(this, Validator(init)))
    }

    override fun <R> KFunction1<T, R>.shouldBe(arg: R) {
        shouldBe(arg, { throw InvalidData() })
    }

    override fun <R> KFunction1<T, R>.shouldBe(arg: R, err: E, doAfter: () -> Unit) {
        shouldBe(arg, { err })
    }

    override fun <R> KFunction1<T, R>.shouldBe(arg: R, err: (R) -> E, doAfter: () -> Unit) {
        validators.add(FunctionValidator(this, Validator(EqValidator(arg, err), doAfter) {}))
    }

    override fun <R> KProperty1<T, R>.should(arg: Matcher<R, E>) {
        validators.add(PropertyValidator(this, Validator(MatcherValidator(arg)) {}))
    }

    override fun <R> KProperty1<T, R>.shouldNot(arg: Matcher<R, E>) {
        validators.add(PropertyValidator(this, Validator(MatcherValidator(!arg)) {}))
    }

    override fun <R> KFunction1<T, R>.should(arg: Matcher<R, E>) {
        validators.add(FunctionValidator(this, Validator(MatcherValidator(arg)) {}))
    }

    override fun <R> KFunction1<T, R>.shouldNot(arg: Matcher<R, E>) {
        validators.add(FunctionValidator(this, Validator(MatcherValidator(!arg)) {}))
    }

    override fun ValidationBuilder<T, E>.checkedBy(validator: Validation<T, E>) {
        validators.add(ExternalValidator(validator))
    }

    override fun <R> KFunction1<T, R?>.ifPresent(init: ValidationBuilder<R, E>.() -> Unit) {
        validators.add(FunctionIfPresentValidator(this, Validator(init)))
    }

    override fun <R> KProperty1<T, R>.shouldBe(arg: R) {
        shouldBe(arg, { throw InvalidData() })
    }

    override fun <R> KProperty1<T, R>.shouldBe(arg: R, err: E, doAfter: () -> Unit) {
        shouldBe(arg, { err })
    }

    override fun <R> KProperty1<T, R>.shouldBe(arg: R, err: (R) -> E, doAfter: () -> Unit) {
        validators.add(PropertyValidator(this, Validator(EqValidator(arg, err), doAfter) {}))
    }

    override fun <R> KProperty1<T, R>.shouldNotBe(arg: R) {
        shouldNotBe(arg, { throw InvalidData() })
    }

    override fun <R> KProperty1<T, R>.shouldNotBe(arg: R, err: (R) -> E, doAfter: () -> Unit) {
        validators.add(PropertyValidator(this, Validator(EqValidator(arg, err, reverse = true), doAfter) {}))
    }

    override fun ValidationBuilder<String, E>.notBlank(err: E) {
        notBlank { err }
    }

    override fun ValidationBuilder<String, E>.notBlank(errFunc: (T) -> E) {
        validators.add(NotBlankValidator(self, errFunc))
    }

    override fun <N : Number> ValidationBuilder<N, E>.moreThan(min: Int, err: E) {
        moreThan(min) { err }
    }

    override fun <N : Number> ValidationBuilder<N, E>.moreThan(min: Int, errFunc: (T) -> E) {
        validators.add(HasLengthValidator(self, min, null, errFunc))
    }

    override fun <N : Number> ValidationBuilder<N, E>.lessThan(max: Int, err: E) {
        lessThan(max) { err }
    }

    override fun <N : Number> ValidationBuilder<N, E>.lessThan(max: Int, errFunc: (T) -> E) {
        validators.add(HasLengthValidator(self, null, max, errFunc))
    }

    fun build(): Validator<T, E> = SeqValidator(validators)
}