package itpu.uz.itpuhrms.config

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [ValidEmailValidator::class])
annotation class ValidEmail(
    val message: String = "INVALID_EMAIL_ADDRESS",
    val groups: Array<KClass<out Any>> = [],
    val payload: Array<KClass<out Any>> = []
)

@Component
class ValidEmailValidator : ConstraintValidator<ValidEmail, String> {
    private val localPartRegex = Regex("^[A-Za-z0-9#\\-.~!$&'()*+,;=:%20]+\$")
    private val domainRegex = Regex("^[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\$")
    private val topLevelDomainRegex = Regex("^[A-Za-z]+\$")

    override fun isValid(value: String?, context: ConstraintValidatorContext?): Boolean {
        value?.let {
            if (value.length > 320) return false

            val parts = value.split("@")
            if (parts.size != 2) return false

            val localPart = parts[0]
            val domainPart = parts[1]

            if (localPart.length > 64 || localPart.isEmpty()) return false
            if (localPart.startsWith(".") || localPart.endsWith(".")) return false
            if (!localPartRegex.matches(localPart)) return false
            if (".." in localPart) return false

            if (domainPart.length > 253 || domainPart.isEmpty()) return false
            if (!domainRegex.matches(domainPart)) return false
            val domainParts = domainPart.split(".")
            if (domainParts.size < 2) return false

            if (!topLevelDomainRegex.matches(domainParts.last())) return false
        }

        return true
    }
}