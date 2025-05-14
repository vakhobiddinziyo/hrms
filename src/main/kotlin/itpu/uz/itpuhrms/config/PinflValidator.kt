package itpu.uz.itpuhrms.config

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import kotlin.reflect.KClass

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY_GETTER)
@Constraint(validatedBy = [PinflValidator::class])
annotation class CheckPinfl(
    val message: String = "Invalid pinfl",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)

class PinflValidator : ConstraintValidator<CheckPinfl, String> {
    override fun initialize(constraintAnnotation: CheckPinfl) {}

    override fun isValid(value: String?, context: ConstraintValidatorContext) =
        value != null && value.length == 14 && value.matches(Regex("^[0-9]+$"))
}