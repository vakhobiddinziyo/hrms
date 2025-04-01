package itpu.uz.itpuhrms

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [StatusTypeSubsetValidator::class])
annotation class StatusType(
    val message: String = "must be any of {anyOf}",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

class StatusTypeSubsetValidator(
    private var subset: Array<Status>
) : ConstraintValidator<StatusType, Status> {

    override fun initialize(constraintAnnotation: StatusType) {
        this.subset = Status.values()
    }

    override fun isValid(value: Status?, context: ConstraintValidatorContext?): Boolean {
        return value != null && subset.toMutableList().contains(value)
    }
}

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