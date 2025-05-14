package itpu.uz.itpuhrms.config

import itpu.uz.itpuhrms.Status
import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
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
