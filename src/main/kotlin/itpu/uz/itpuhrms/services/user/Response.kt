package itpu.uz.itpuhrms.services.user

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include
import itpu.uz.itpuhrms.*
import itpu.uz.itpuhrms.services.employee.DepartmentShortResponse
import itpu.uz.itpuhrms.services.permission.PermissionResponse
import itpu.uz.itpuhrms.services.userOrgSession.UserSessionResponse

data class UserResponse(
    val id: Long,
    val fullName: String,
    val username: String,
    val role: Role,
    val status: Status,
    val phoneNumber: String,
    val mail: String,
    val avatarPhoto: String? = null,
    @JsonInclude(Include.NON_NULL)
    val credentials: UserCredentialsResponse? = null
) {
    companion object {
        fun toDto(user: User, credentials: UserCredentialsResponse?) = UserResponse(
            user.id!!,
            user.fullName,
            user.username,
            user.role,
            user.status,
            user.phoneNumber,
            user.mail,
            user.avatarPhoto?.hashId,
            credentials
        )
    }
}


data class UserMeResponse(
    val userId: Long,
    val username: String,
    val fullName: String,
    val role: Role,
    val isEmployee: Boolean = false,
    val granted: Boolean = false,
    val userSession: UserSessionResponse? = null,
    val employee: UserMeEmployeeResponse? = null,
    val avatarHashId: String? = null
)


data class UserCredentialsResponse(
    val id: Long,
    val pinfl: String,
    val fio: String,
    val cardGivenDate: Long,
    val cardExpireDate: Long,
    val cardSerialNumber: String,
    val gender: Gender,
    val birthday: Long,
    val userId: Long
) {
    companion object {
        fun toDto(credentials: UserCredentials) = UserCredentialsResponse(
            credentials.id!!,
            credentials.pinfl,
            credentials.fio,
            credentials.cardGivenDate.time,
            credentials.cardExpireDate.time,
            credentials.cardSerialNumber,
            credentials.gender,
            credentials.birthday.time,
            credentials.user.id!!
        )
    }
}



data class UserMeEmployeeResponse(
    val employeeId: Long? = null,
    val position: String? = null,
    val permissions: List<PermissionResponse>,
    val department: DepartmentShortResponse? = null
) {
    companion object {
        fun toDto(employee: Employee?): UserMeEmployeeResponse {
            return UserMeEmployeeResponse(
                employee?.id,
                employee?.position?.name,
                employee?.let {
                    it.permissions.map { permission -> PermissionResponse(permission.id!!, permission.permissionData) }
                } ?: run { listOf() },
                employee?.let {
                    DepartmentShortResponse.toDto(it.department)
                }
            )
        }
    }
}


data class UserDto(
    val id: Long,
    val fullName: String,
    val username: String,
    val role: Role,
    val status: Status,
    val phoneNumber: String,
    val mail: String,
    val avatarPhoto: String? = null,
) {
    companion object {
        fun toResponse(user: User) = user.run {
            UserDto(
                id!!,
                fullName,
                username,
                role,
                status,
                phoneNumber,
                mail
            )
        }
    }
}
