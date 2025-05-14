package itpu.uz.itpuhrms


import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import itpu.uz.itpuhrms.base.BaseMessage
import itpu.uz.itpuhrms.bot.ErrorBot
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.context.support.ResourceBundleMessageSource
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.core.AuthenticationException
import org.springframework.validation.BindException
import org.springframework.web.bind.MissingPathVariableException
import org.springframework.web.bind.MissingRequestValueException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.NoHandlerFoundException
import java.lang.reflect.InvocationTargetException
import java.util.*


class CustomFeignException(
    val errorCode: Int,
    val errorMsg: String,
    val statusCode: Int,
    val statusString: String,
    val subStatusCode: String,
    val httpStatus: Int,
    override val message: String,
) : RuntimeException(errorMsg)


@ControllerAdvice
class GlobalExceptionHandler(
    private val errorMessageSource: ResourceBundleMessageSource,
    private val errorBot: ErrorBot
) {

    @ExceptionHandler(NoHandlerFoundException::class)
    fun handleNotFoundException(ex: NoHandlerFoundException): ResponseEntity<BaseMessage> {
        val message = "Resource not found: ${ex.requestURL}"
        return ResponseEntity(BaseMessage(404, message), HttpStatus.NOT_FOUND)
    }

    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthException(ex: AuthenticationException): ResponseEntity<BaseMessage> {
        return ResponseEntity(BaseMessage(401, ex.message), HttpStatus.UNAUTHORIZED)
    }

    @ExceptionHandler(BindException::class)
    fun handleBindException(ex: BindException): ResponseEntity<ValidationErrorMessage> {
        val fields = ex.bindingResult.fieldErrors.associate { it.field to it.defaultMessage }
        return ResponseEntity.badRequest().body(createValidationError(fields))
    }

    @ExceptionHandler(MissingRequestValueException::class)
    fun handleMissingParams(ex: MissingRequestValueException): ResponseEntity<String> {
        val message = when (ex) {
            is MissingServletRequestParameterException -> "Missing request parameter: ${ex.parameterName}"
            is MissingPathVariableException -> "Missing path variable: ${ex.variableName}"
            else -> ex.localizedMessage ?: "Missing request value"
        }
        return ResponseEntity(message, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(RuntimeException::class)
    fun handleRuntimeExceptions(ex: RuntimeException): ResponseEntity<Any> {
        return when (ex) {
            is HrmException -> {
                ResponseEntity.badRequest().body(ex.getErrorMessage(errorMessageSource))
            }

            is MethodArgumentTypeMismatchException -> {
                val fields = mapOf(ex.name to ex.value)
                ResponseEntity.badRequest().body(createValidationError(fields))
            }

            is IllegalArgumentException -> {
                val target = extractTargetException(ex)
                if (target is HrmException) {
                    return ResponseEntity.badRequest().body(target.getErrorMessage(errorMessageSource))
                }
                ex.printStackTrace()
                errorBot.sendLog(ex)
                ResponseEntity.badRequest().body(BaseMessage(100, ex.localizedMessage))
            }

            is HttpMessageNotReadableException -> {
                val fields = mutableMapOf<String, Any?>()
                val cause = ex.cause
                if (cause is JsonMappingException) {
                    cause.path.forEach { ref ->
                        fields[ref.fieldName ?: ""] = if (cause is InvalidFormatException) cause.value else null
                    }
                }
                ResponseEntity.badRequest().body(createValidationError(fields))
            }

            is CustomFeignException -> {
                val body = mapOf(
                    "errorCode" to ex.errorCode,
                    "errorMsg" to ex.errorMsg,
                    "statusCode" to ex.statusCode,
                    "statusString" to ex.statusString,
                    "subStatusCode" to ex.subStatusCode,
                    "message" to ex.message
                )
                ResponseEntity(body, HttpStatus.valueOf(ex.httpStatus))
            }

            is AccessDeniedException -> {
                ResponseEntity.status(HttpStatus.FORBIDDEN).body(BaseMessage(403, ex.message))
            }

            else -> {
                ex.printStackTrace()
                errorBot.sendLog(ex)
                ResponseEntity.badRequest().body(BaseMessage(100, ex.localizedMessage))
            }
        }
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneralException(ex: Exception): ResponseEntity<BaseMessage> {
        ex.printStackTrace()
        errorBot.sendLog(ex)
        return ResponseEntity(BaseMessage(500, "Unexpected error occurred: ${ex.localizedMessage}"), HttpStatus.INTERNAL_SERVER_ERROR)
    }

    private fun createValidationError(fields: Map<String, Any?>): ValidationErrorMessage {
        val errorCode = ErrorCode.VALIDATION_ERROR
        val message = errorMessageSource.getMessage(
            errorCode.toString(),
            null,
            Locale(LocaleContextHolder.getLocale().language)
        )
        return ValidationErrorMessage(errorCode.code, message, fields)
    }

    private fun extractTargetException(ex: IllegalArgumentException): Throwable? {
        return try {
            val cause = ex.cause?.cause?.cause
            if (cause is InvocationTargetException) cause.targetException else null
        } catch (_: Exception) {
            null
        }
    }
}



data class ValidationErrorMessage(val code: Int, val message: String, val fields: Map<String, Any?>)

sealed class HrmException(message: String? = null) : RuntimeException(message) {
    abstract fun errorType(): ErrorCode
    protected open fun getErrorMessageArguments(): Array<Any?>? = null
    fun getErrorMessage(errorMessageSource: ResourceBundleMessageSource): BaseMessage {
        return BaseMessage(
            errorType().code, errorMessageSource.getMessage(
                errorType().toString(), getErrorMessageArguments(), Locale(LocaleContextHolder.getLocale().language)
            )
        )
    }
}

class ObjectNotFoundException(private val objName: String? = null, private val identifier: Any? = null) :
    HrmException() {
    override fun errorType() = ErrorCode.OBJECT_NOT_FOUND
    override fun getErrorMessageArguments(): Array<Any?> = arrayOf(objName, identifier)
}


class UserNotFoundException : HrmException() {
    override fun errorType() = ErrorCode.USER_NOT_FOUND
}

class UserSessionNotFoundException : HrmException() {
    override fun errorType() = ErrorCode.USER_SESSION_NOT_FOUND
}

class PastDateException : HrmException() {
    override fun errorType() = ErrorCode.PAST_TABLE_DATE
}

class ProjectEmployeeAlreadyExistsException : HrmException() {
    override fun errorType() = ErrorCode.PROJECT_EMPLOYEE_ALREADY_EXIST
}

class EmployeeNotFoundException : HrmException() {
    override fun errorType() = ErrorCode.EMPLOYEE_NOT_FOUND
}

class OrganizationAlreadyExistException : HrmException() {
    override fun errorType() = ErrorCode.ORGANIZATION_ALREADY_EXIST
}

class OrganizationNotFoundException : HrmException() {
    override fun errorType() = ErrorCode.ORGANIZATION_NOT_FOUND
}

class PositionNotFoundException : HrmException() {
    override fun errorType() = ErrorCode.POSITION_NOT_FOUND
}

class DepartmentNotFoundException : HrmException() {
    override fun errorType() = ErrorCode.DEPARTMENT_NOT_FOUND
}

class TourniquetClientNotFoundException : HrmException() {
    override fun errorType() = ErrorCode.TOURNIQUET_CLIENT_NOT_FOUND
}

class UsernameAlreadyExistException : HrmException() {
    override fun errorType() = ErrorCode.USER_ALREADY_EXISTS_WITH_USERNAME
}

class UsernameOrPasswordIncorrectException : HrmException() {
    override fun errorType() = ErrorCode.USERNAME_OR_PASSWORD_INCORRECT
}

class TourniquetClientUsernameAlreadyExistException : HrmException() {
    override fun errorType() = ErrorCode.TOURNIQUET_CLIENT_ALREADY_EXISTS_WITH_USERNAME
}

class InvalidAuthorizationTypeException : HrmException() {
    override fun errorType() = ErrorCode.INVALID_AUTHORIZATION_TYPE
}

class NotAllowedException : HrmException() {
    override fun errorType() = ErrorCode.NOT_ALLOWED
}

class PinflAlreadyExistException : HrmException() {
    override fun errorType() = ErrorCode.USER_ALREADY_EXISTS_WITH_PINFL
}

class UserCredentialsNotFoundException : HrmException() {
    override fun errorType() = ErrorCode.USER_CREDENTIAL_NOT_FOUND
}

class BadCredentialsException : HrmException() {
    override fun errorType() = ErrorCode.BAD_CREDENTIALS
}

class FileNotFoundException : HrmException() {
    override fun errorType() = ErrorCode.FILE_NOT_FOUND
}

class PermissionNotFoundException : HrmException() {
    override fun errorType() = ErrorCode.PERMISSION_NOT_FOUND
}

class UserOrgStoreNotFoundException : HrmException() {
    override fun errorType() = ErrorCode.USER_ORG_STORE_NOT_FOUND
}

class UserOrgStoreAlreadyExistException : HrmException() {
    override fun errorType() = ErrorCode.USER_ORG_STORE_ALREADY_EXIST
}

class TourniquetNotFoundException : HrmException() {
    override fun errorType() = ErrorCode.TOURNIQUET_NOT_FOUND
}

class InvalidDayOfWeekException(private val day: Short) : HrmException() {
    override fun errorType() = ErrorCode.INVALID_DAY_OF_WEEK
    override fun getErrorMessageArguments(): Array<Any?> = arrayOf(day)
}

class DayOfWeekAlreadyExistsException(private val day: Short) : HrmException() {
    override fun errorType() = ErrorCode.DAY_OF_WEEK_ALREADY_EXISTS
    override fun getErrorMessageArguments(): Array<Any?> = arrayOf(day)
}

class WorkingDateConfigNotFoundException : HrmException() {
    override fun errorType() = ErrorCode.WORKING_DATE_CONFIG_NOT_FOUND
}

class InvalidStartTimeException : HrmException() {
    override fun errorType() = ErrorCode.INVALID_START_TIME
}

class InvalidDateRangeException : HrmException() {
    override fun errorType() = ErrorCode.INVALID_DATE_RANGE
}

class InvalidRequiredHourException : HrmException() {
    override fun errorType() = ErrorCode.INVALID_REQUIRED_HOUR
}

class EmployeeIsBusyException : HrmException() {
    override fun errorType() = ErrorCode.EMPLOYEE_IS_ALREADY_BUSY
}

class EmployeeIsVacantException : HrmException() {
    override fun errorType() = ErrorCode.EMPLOYEE_IS_ALREADY_VACANT
}

class EmployeeExistsWithinOrganizationException : HrmException() {
    override fun errorType() = ErrorCode.EMPLOYEE_ALREADY_EXISTS_IN_ORGANIZATION
}

class EmploymentHistoryNotFoundException : HrmException() {
    override fun errorType() = ErrorCode.EMPLOYMENT_HISTORY_NOT_FOUND
}

class ProjectNotFoundException : HrmException() {
    override fun errorType() = ErrorCode.PROJECT_NOT_FOUND
}

class BoardNotFoundException : HrmException() {
    override fun errorType() = ErrorCode.BOARD_NOT_FOUND
}

class ProjectEmployeeNotFoundException : HrmException() {
    override fun errorType() = ErrorCode.PROJECT_EMPLOYEE_NOT_FOUND
}

class StateNotFoundException : HrmException() {
    override fun errorType() = ErrorCode.STATE_NOT_FOUND
}

class CommentNotFoundException : HrmException() {
    override fun errorType() = ErrorCode.COMMENT_NOT_FOUND
}

class OrderNumberAlreadyExistException : HrmException() {
    override fun errorType() = ErrorCode.ORDER_NUMBER_ALREADY_EXIST
}

class ForeignEmployeeException(private val employeeId: Long? = null) : HrmException() {
    override fun errorType() = ErrorCode.FOREIGN_EMPLOYEE_ERROR
    override fun getErrorMessageArguments(): Array<Any?> = arrayOf(employeeId)
}

class TaskNotFoundException : HrmException() {
    override fun errorType() = ErrorCode.TASK_NOT_FOUND
}

class TaskActionHistoryNotFoundException : HrmException() {
    override fun errorType() = ErrorCode.TASK_ACTION_HISTORY_NOT_FOUND
}

class DifferentOrganizationsException : HrmException() {
    override fun errorType() = ErrorCode.DIFFERENT_ORGANIZATIONS_ERROR
}

class OtpProhibitedException : HrmException() {
    override fun errorType() = ErrorCode.OTP_IS_PROHIBITED_ERROR
}

class SubscriberNotFoundException : HrmException() {
    override fun errorType() = ErrorCode.SUBSCRIBER_NOT_FOUND
}

class TaskSubscriberNotFoundException : HrmException() {
    override fun errorType() = ErrorCode.TASK_SUBSCRIBER_NOT_FOUND
}

class TaskSubscriberAlreadyExistException : HrmException() {
    override fun errorType() = ErrorCode.TASK_SUBSCRIBER_ALREADY_EXIST
}

class DifferentBoardOwnerException : HrmException() {
    override fun errorType() = ErrorCode.DIFFERENT_BOARD_OWNER
}

class OtpMessageLimitException() : HrmException() {
    override fun errorType() = ErrorCode.OTP_MESSAGE_LIMIT_ERROR
}

class DifferentProjectOwnerException : HrmException() {
    override fun errorType() = ErrorCode.DIFFERENT_PROJECT_OWNER
}

class DifferentBoardTaskException : HrmException() {
    override fun errorType() = ErrorCode.DIFFERENT_BOARD_TASK
}

class IncorrectStateOrderException : HrmException() {
    override fun errorType() = ErrorCode.INCORRECT_STATE_ORDER
}

class IncorrectTaskOrderException : HrmException() {
    override fun errorType() = ErrorCode.INCORRECT_TASK_ORDER
}

class InvalidSubTaskException : HrmException() {
    override fun errorType() = ErrorCode.INVALID_SUB_TASK_ERROR
}

class InvalidTaskException : HrmException() {
    override fun errorType() = ErrorCode.INVALID_TASK_ERROR
}

class TourniquetNameAlreadyExistsException : HrmException() {
    override fun errorType() = ErrorCode.TOURNIQUET_NAME_ALREADY_EXISTS
}

class TableDateNotFoundException : HrmException() {
    override fun errorType() = ErrorCode.TABLE_DATE_NOT_FOUND
}

class TimeTrackingNotFoundException : HrmException() {
    override fun errorType() = ErrorCode.TIME_TRACKING_NOT_FOUND
}

class TimeTrackingAlreadyExistsException : HrmException() {
    override fun errorType() = ErrorCode.TIME_TRACKING_ALREADY_EXISTS
}

class StateTemplateNotFoundException : HrmException() {
    override fun errorType() = ErrorCode.STATE_TEMPLATE_NOT_FOUND
}

class TableDateAlreadyExistException : HrmException() {
    override fun errorType() = ErrorCode.TABLE_DATE_ALREADY_EXCEPTION
}

class EmployeeConnectedToTaskException : HrmException() {
    override fun errorType() = ErrorCode.EMPLOYEE_CONNECTED_TO_TASK
}

class DeletingProjectOwnerException : HrmException() {
    override fun errorType() = ErrorCode.DELETING_PROJECT_OWNER_ERROR
}

class NotValidImageTypeException : HrmException() {
    override fun errorType() = ErrorCode.NOT_VALID_IMAGE_TYPE
}

class ImageSizeLimitException : HrmException() {
    override fun errorType() = ErrorCode.IMAGE_SIZE_LIMIT
}

class InvalidPhoneNumberException : HrmException() {
    override fun errorType() = ErrorCode.INVALID_PHONE_NUMBER
}

class SerialNumberAlreadyExistsException : HrmException() {
    override fun errorType() = ErrorCode.SERIAL_NUMBER_ALREADY_EXISTS
}

class EmployeeImageAlreadyUsedException : HrmException() {
    override fun errorType() = ErrorCode.EMPLOYEE_IMAGE_ALREADY_USED
}

class NotValidEmployeeTypeException : HrmException() {
    override fun errorType() = ErrorCode.NOT_VALID_EMPLOYEE_TYPE
}

class EmployeeDataNotFoundException : HrmException() {
    override fun errorType() = ErrorCode.EMPLOYEE_DATA_NOT_FOUND
}

class TimeNotCompatibleException : HrmException() {
    override fun errorType() = ErrorCode.TIME_NOT_COMPATIBLE
}

class AccessDeniedException : HrmException() {
    override fun errorType() = ErrorCode.ACCESS_DENIED_ERROR
}

class TimeTrackingLimitException(private val date: Date) : HrmException() {
    override fun errorType() = ErrorCode.TIME_TRACKING_LIMIT

    override fun getErrorMessageArguments(): Array<Any?> = arrayOf(date)
}

class DifferentParentTaskException : HrmException() {
    override fun errorType() = ErrorCode.DIFFERENT_PARENT_TASK_ERROR
}

class NonWorkingDayException : HrmException() {
    override fun errorType() = ErrorCode.NON_WORKING_DAY
}


class PermissionDeniedException : HrmException() {
    override fun errorType() = ErrorCode.PERMISSION_DENIED_ERROR
}

class DeactivatedUserException : HrmException() {
    override fun errorType() = ErrorCode.USER_DEACTIVATED
}

class BoardSettingsAlreadyExistException : HrmException() {
    override fun errorType() = ErrorCode.BOARD_SETTINGS_ALREADY_EXIST
}

class BoardSettingsNotFoundException : HrmException() {
    override fun errorType() = ErrorCode.BOARD_SETTINGS_NOT_FOUND
}

class AssignDeactivatedEmployeeException : HrmException() {
    override fun errorType(): ErrorCode = ErrorCode.ASSIGN_DEACTIVATED_EMPLOYEE
}

class DifferentBoardProjectException : HrmException() {
    override fun errorType(): ErrorCode = ErrorCode.DIFFERENT_BOARD_PROJECT
}
class TableDateNotWorkDayException : HrmException() {
    override fun errorType(): ErrorCode = ErrorCode.TABLE_DATE_NOT_WORK_DAY
}
class UserAbsenceTrackerNotFoundException : HrmException() {
    override fun errorType(): ErrorCode = ErrorCode.USER_ABSENCE_TRACKER_NOT_FOUND
}
class UserAbsenceTrackerAlreadyExistsException : HrmException(){
    override fun errorType(): ErrorCode = ErrorCode.USER_ABSENCE_TRACKER_ALREADY_EXISTS
}
class NotSameMonthOfYearException : HrmException(){
    override fun errorType(): ErrorCode = ErrorCode.NOT_SAME_MONTH_OF_YEAR
}


