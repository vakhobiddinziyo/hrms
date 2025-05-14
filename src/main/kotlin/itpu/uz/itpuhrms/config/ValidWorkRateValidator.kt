package itpu.uz.itpuhrms.config

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [ValidWorkRateValidator::class])
annotation class ValidWorkRate(
    val message: String = "INVALID_WORK_RATE",
    val groups: Array<KClass<out Any>> = [],
    val payload: Array<KClass<out Any>> = []
)

@Component
class ValidWorkRateValidator : ConstraintValidator<ValidWorkRate, Double> {
    override fun isValid(value: Double?, context: ConstraintValidatorContext?): Boolean {
        return value?.let {
            value == 0.25 || value == 0.5 || value == 0.75 || value == 1.0
                    || value == 1.25 || value == 1.5 || value == 1.75 || value == 2.0
        } ?: true
    }
}
