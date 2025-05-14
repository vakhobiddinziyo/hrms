package itpu.uz.itpuhrms.config

import itpu.uz.itpuhrms.isNumeric
import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [ValidIPValidator::class])
annotation class ValidIP(
    val message: String = "INVALID_IP_ADDRESS",
    val groups: Array<KClass<out Any>> = [],
    val payload: Array<KClass<out Any>> = []
)

@Component
class ValidIPValidator : ConstraintValidator<ValidIP, String> {
    override fun isValid(value: String?, context: ConstraintValidatorContext?): Boolean {
        value?.let {
            val parts = value.split(".")
            if (parts.size != 4) return false
            for (i in 0..3) {
                if (!parts[i].isNumeric()) return false
                if (parts[i].toLong() !in 0..255) return false
            }
        }
        return true
    }
}