package itpu.uz.itpuhrms

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import jakarta.validation.Valid
import jakarta.validation.constraints.*
import org.springframework.data.domain.PageImpl
import org.springframework.security.crypto.password.PasswordEncoder
import itpu.uz.itpuhrms.ValidRequest.Companion.validTimeRequest
import java.math.BigDecimal
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.*

data class BaseMessage(
    val code: Int? = null,
    val message: String? = null,
) {
    companion object {
        var OK = BaseMessage(0, "OK")
    }
}

data class ProjectCreateRequest(
    @field:NotNull(message = "Project name must not be null!")
    @field:Size(message = "Project name length must be 1-50 letters", max = 50, min = 1)
    val name: String? = null,
    @field:Size(message = "Project description length must be maximum 175", max = 175)
    val description: String? = null,
) {
    fun toEntity(employee: Employee) =
        Project(name!!, employee, employee.department, description, ProjectStatus.ACTIVE)
}

data class ProjectUpdateRequest(
    @field:NotNull(message = "Project name must not be null!")
    @field:Size(message = "Project name length must be 1-50 letters", max = 50, min = 1)
    val name: String? = null,
    @field:Size(message = "Project description length must be maximum 175", max = 175)
    val description: String? = null,
    val status: ProjectStatus
)

data class ProjectResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val ownerName: String? = null,
    val department: String,
    val employeeAmount: Long,
    val status: ProjectStatus,
    val ownerId: Long? = null
) {
    companion object {
        fun toDto(project: Project, employeeAmount: Long) = project.run {
            ProjectResponse(
                id!!,
                name,
                description,
                owner.user?.fullName,
                owner.department.name,
                employeeAmount,
                status,
                project.owner.id
            )
        }
    }
}

data class BoardStateResponse(
    val pageable: PageImpl<StateResponse>
) {
    companion object {
        fun toResponse(board: Board, pageable: PageImpl<StateResponse>) = board.run {
            BoardStateResponse(
                pageable
            )
        }
    }
}

data class Structure(
    val id: Long,
    val name: String,
    val type: StructureType,
    val hasChild: Boolean = false
)

data class ProjectBoardResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val department: String,
    val pageable: PageImpl<BoardAdminResponse>
) {
    companion object {
        fun toResponse(project: Project, pageable: PageImpl<BoardAdminResponse>) = project.run {
            ProjectBoardResponse(
                id!!,
                name,
                description,
                project.department.name,
                pageable
            )
        }
    }
}

data class OrgRequest(
    @field:Size(min = 1, max = 255, message = "name length should be between 1 and 255")
    @field:NotBlank
    @field:NotNull
    val name: String,
    @field:Size(max = 255, message = "description length should be maximum 255")
    val description: String?,
    val status: Status,
    @field:Size(min = 9, max = 9, message = "TIN length should be 9")
    @field:NotBlank
    @field:NotNull
    val tin: String,
)

data class OrgResponse(
    val id: Long,
    val name: String,
    val status: Status,
    val isActive: Boolean
) {
    companion object {
        fun toDto(org: Organization): OrgResponse {
            return org.let {
                OrgResponse(
                    it.id!!,
                    it.name,
                    it.status,
                    it.isActive
                )
            }
        }
    }
}

data class OrgAdminResponse(
    var id: Long,
    val name: String,
    val description: String?,
    val status: Status,
    val tin: String,
    val isActive: Boolean,
    val granted: Boolean?
) {
    companion object {
        fun toDto(org: Organization, store: UserOrgStore? = null): OrgAdminResponse {
            return org.let {
                OrgAdminResponse(
                    it.id!!,
                    it.name,
                    it.description,
                    it.status,
                    it.tin,
                    it.isActive,
                    store?.granted
                )
            }
        }
    }
}

data class HeadDepartmentRequest(
    val name: String,
    val description: String?,
    val departmentType: DepartmentType
)

data class DepartmentRequest(
    @field:Size(min = 1, max = 255, message = "name length should be between 1 and 255")
    @field:NotBlank
    @field:NotNull
    val name: String,
    @field:Size(min = 1, max = 255, message = "description length should be between 1 and 255")
    val description: String?,
    val departmentType: DepartmentType,
    val parentDepartmentId: Long? = null
)

data class DepartmentShortResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val departmentType: DepartmentType
) {
    companion object {
        fun toDto(department: Department) = DepartmentShortResponse(
            department.id!!,
            department.name,
            department.description,
            department.departmentType
        )
    }
}

data class DepartmentContentResponse(
    val structureTree: List<Structure>,
    var content: List<DepartmentAdminResponse> = mutableListOf()
)

data class DepartmentAdminResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val departmentType: DepartmentType,
    var totalEmployee: Long,
    var vacantEmployee: Long,
    var busyEmployee: Long,
    var hasChild: Boolean,
)

data class DepartmentAllContentResponse(
    val structureTree: List<Structure>,
    var content: List<DepartmentAllAdminResponse> = mutableListOf()
)

data class DepartmentAllAdminResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val departmentType: DepartmentType,
    var totalEmployee: Long,
    var vacantEmployee: Long,
    var busyEmployee: Long,
    var hasChild: Boolean,
    var childDepartmentIds: MutableList<Long> = mutableListOf(),
    val parentId: Long?
)

data class DepartmentResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val departmentType: DepartmentType,
    var organization: OrgResponse,
    var status: Status,
    var headDepartment: ParentDepartmentResponse? = null,
    var parentDepartment: ParentDepartmentResponse? = null
) {
    companion object {
        fun toDto(department: Department) = department.run {
            DepartmentResponse(
                id!!,
                name,
                description,
                departmentType,
                OrgResponse.toDto(organization),
                status,
                headDepartment?.let { ParentDepartmentResponse.toResponse(it) },
                parentDepartment?.let { ParentDepartmentResponse.toResponse(it) },
            )
        }
    }

    data class ParentDepartmentResponse(
        val id: Long,
        val name: String,
        val description: String?,
        val departmentType: DepartmentType,
        var status: Status,
    ) {
        companion object {
            fun toResponse(department: Department) = department.run {
                ParentDepartmentResponse(
                    id!!,
                    name,
                    description,
                    departmentType,
                    status
                )
            }
        }
    }
}

data class EmployeeRequest(
    val userId: Long,
    val status: Status,
    val positionId: Long,
    val departmentId: Long,
    @field:Positive(message = "workRate must be positive")
    @field:ValidWorkRate
    val workRate: Double,
    @field:Positive(message = "laborRate must be positive")
    val laborRate: Short,
    val code: String? = null,
    val imageHashId: String? = null,
)

data class EmployeeVacantRequest(
    val status: Status,
    val positionId: Long,
    val departmentId: Long,
    @field:Positive(message = "workRate must be positive")
    @field:ValidWorkRate
    val workRate: Double,
    @field:Positive(message = "laborRate must be positive")
    val laborRate: Short,
    val imageHashId: String? = null,
)

data class EmployeeUpdateRequest(
    val positionId: Long?,
    val departmentId: Long?,
    @field:ValidWorkRate
    val workRate: Double?,
    val laborRate: Short?,
    val imageHashId: String?,
    val code: String?,
    val status: Status?
)

data class EmployeeImageUpdateRequest(
    val id: Long,
    val imageHashId: String,
)

data class EmployeeHireRequest(
    val status: Status,
    val positionId: Long,
    val departmentId: Long,
    @field:Positive(message = "workRate must be positive")
    @field:ValidWorkRate
    val workRate: Double,
    @field:Positive(message = "laborRate must be positive")
    val laborRate: Short
)

data class EmploymentHistoryRequest(
    @CheckPinfl
    val userPinfl: String,
    val imageHashId: String,
    val employeeId: Long
)

data class EmploymentHistoryResponse(
    val id: Long,
    val employeeDto: EmployeeResponse,
    val userResponse: UserResponse,
    val positionResponse: PositionResponse,
    val department: DepartmentShortResponse,
    val hiredDate: Date?,
    val dismissedDate: Date?,
) {
    companion object {
        fun toResponse(history: UserEmploymentHistory) = history.run {
            EmploymentHistoryResponse(
                history.id!!,
                EmployeeResponse.toDto(employee),
                UserResponse.toDto(user, null),
                PositionResponse.toDto(position, OrgResponse.toDto(position.organization)),
                DepartmentShortResponse.toDto(department),
                hiredDate, dismissedDate
            )
        }
    }
}

data class EmployeeBirthdateResponse(
    val id: Long,
    val imageHashId: String?,
    val birthday: Date,
    val fullName: String,
    val departmentName: String
)

data class EmployeeStatisticsResponse(
    val id: Long,
    val fullName: String,
    val countProjects: Short,
    val totalTasksTime: Int,
    val workedTasksTime: Int,
    val requiredOfficeMinutes: Int,
    val inOfficeMinutes: Int,
    val countCompleteTasks: Short,
    val countIncompleteTasks: Short,
    val countAbsentWorkdays: Short,
    val countLateWorkdays: Short,
    val countEarlyWorkdays: Short

)

data class AmountEmployeesResponse(
    val amountOfficeEmployees: Long = 0L,
    val amountNotOfficeEmployees: Long = 0L

)

data class PositionRequest(
    val name: String,
    var level: Level,
    val permissions: MutableSet<Long>
)

data class PositionAdminRequest(
    val name: String,
    val level: Level,
    val permission: MutableSet<Long>,
    val organizationId: Long
)

@JsonInclude(Include.NON_NULL)
data class PositionResponse(
    val id: Long,
    val name: String,
    var level: Level,
    val permissions: List<PermissionResponse>,
    var organization: OrgResponse? = null,
) {
    companion object {
        fun toDto(position: Position, organization: OrgResponse?) = PositionResponse(
            position.id!!,
            position.name,
            position.level,
            position.permission.map { PermissionResponse(it.id!!, it.permissionData) },
            organization
        )
    }
}

data class PositionDto(
    val id: Long,
    val name: String,
    val level: Level,
    val totalEmployee: Long,
    val vacantEmployee: Long,
    val busyEmployee: Long,
)

data class PositionContentResponse(
    val structureTree: List<Structure>,
    val content: List<PositionDto>
)

@JsonInclude(Include.NON_NULL)
data class PositionAdminResponse(
    val id: Long,
    val name: String,
    var level: Level,
    val employeeAmount: Long,
    val permissions: List<PermissionResponse>,
    var organization: OrgResponse? = null,
) {
    companion object {
        fun toDto(position: Position, amount: Long, organization: OrgResponse?) = PositionAdminResponse(
            position.id!!,
            position.name,
            position.level,
            amount,
            position.permission.map { PermissionResponse(it.id!!, it.permissionData) },
            organization
        )
    }
}

data class PositionEmployeesResponse(
    val id: Long,
    val name: String,
    val employeeAmount: Int
)

data class EmployeesResponse(
    val id: Long,
    val user: UserResponse?,
    val imageHashId: String?,
    val status: Status,
    var phStatus: PositionHolderStatus,
    var workRate: Double,
    var laborRate: Short,
    var atOffice: Boolean,
    val position: PositionResponse,
    val department: DepartmentShortResponse,
    val code: String? = null
) {
    companion object {
        fun toDto(employee: Employee, credentials: UserCredentials?) = EmployeesResponse(
            employee.id!!,
            employee.user?.let {
                UserResponse.toDto(
                    it,
                    credentials?.let { credential -> UserCredentialsResponse.toDto(credential) })
            },
            employee.imageAsset?.hashId,
            employee.status,
            employee.phStatus,
            employee.workRate,
            employee.laborRate,
            employee.atOffice,
            PositionResponse.toDto(employee.position, OrgResponse.toDto(employee.organization)),
            DepartmentShortResponse.toDto(employee.department),
            employee.code
        )
    }
}


@JsonInclude(Include.NON_NULL)
data class EmployeeContentResponse(
    val structureTree: List<Structure>? = null,
    val pageable: PageImpl<EmployeesResponseDto>
) {
    companion object {
        fun toResponse(
            structureTree: List<Structure>? = null,
            pageable: PageImpl<EmployeesResponseDto>
        ): EmployeeContentResponse {
            return EmployeeContentResponse(
                structureTree, pageable
            )
        }
    }
}

data class EmployeesResponseDto(
    val id: Long,
    val atOffice: Boolean,
    val status: Status,
    val laborRate: Short,
    val workRate: Double,
    val phStatus: PositionHolderStatus,
    val imageHashId: String?,
    val user: UserResponseDto?,
    val userCredentials: UserCredentialsResponse?,
    val position: PositionResponseDto,
    val department: DepartmentShortResponse
) {
    data class UserResponseDto(
        val id: Long,
        val fullName: String,
        val mail: String,
        val role: Role,
        val phoneNumber: String,
        val username: String,
        val userStatus: Status
    )

    data class UserCredentialsResponse(
        val gender: Gender,
        val cardSerialNumber: String,
        val pinfl: String
    )

    data class PositionResponseDto(
        val id: Long,
        val name: String
    )
}

data class LateEmployeeResponse(
    val id: Long,
    val userId: Long,
    val name: String,
    val lateDate: Long,
    val imageHashId: String?
)

data class EmployeeResponse(
    val id: Long,
    val user: UserResponse?,
    val imageHashId: String?,
    val status: Status,
    val fullName: String?,
    var phStatus: PositionHolderStatus,
    var workRate: Double,
    var laborRate: Short,
    var atOffice: Boolean
) {
    companion object {
        fun toDto(employee: Employee) = EmployeeResponse(
            employee.id!!,
            employee.user?.let { UserResponse.toDto(it, null) },
            employee.imageAsset?.hashId,
            employee.status,
            employee.user?.fullName ?: employee.position.name,
            employee.phStatus,
            employee.workRate,
            employee.laborRate,
            employee.atOffice
        )
    }
}

data class EmployeeAdminResponse(
    val employeeId: Long,
    val user: UserEmployeeResponse?,
    val department: DepartmentShortResponse,
    val imageHashId: String?,
    val status: Status,
    var phStatus: PositionHolderStatus,
    var empRole: Role?,
    var permissions: List<PermissionResponse>
) {
    data class UserEmployeeResponse(
        val userId: Long,
        val fullName: String,
        val username: String,
        val status: Status,
        val phoneNumber: String
    )

    data class DepartmentShortResponse(
        val id: Long,
        val name: String,
        val description: String?,
        val departmentType: DepartmentType
    )

    data class PermissionResponse(
        val id: Long?,
        val name: String?
    )

}

data class EmployeeDto(
    val id: Long,
    val userId: Long?,
    val imageHashId: String?,
    var code: String?,
    var status: Status,
    var position: PositionResponse,
    var department: DepartmentResponse,
    var organization: OrgResponse,
    var phStatus: PositionHolderStatus,
    var workRate: Double,
    var laborRate: Short,
    var atOffice: Boolean
) {
    companion object {
        fun toDto(employee: Employee) = EmployeeDto(
            employee.id!!,
            employee.user?.id,
            employee.imageAsset?.hashId,
            employee.code,
            employee.status,
            PositionResponse.toDto(employee.position, OrgResponse.toDto(employee.organization)),
            DepartmentResponse.toDto(employee.department),
            OrgResponse.toDto(employee.organization),
            employee.phStatus,
            employee.workRate,
            employee.laborRate,
            employee.atOffice
        )
    }
}

data class EmployeeAttendanceResponse(
    val employeeId: Long,
    val userId: Long,
    val fullName: String,
    val positionName: String,
    val imageHashId: String?,
    @JsonSerialize(using = DateToLongSerializer::class)
    val time: Date?,
    val state: AttendanceStatus
)

data class LaborActivityResponse(
    val firstHiredDate: Long,
    val currentHiredDate: Long,
    val lastFiredDate: Long
)

data class UserRequest(
    @field:Size(min = 1, max = 255, message = "fullName length should be between 1 and 255")
    @field:NotBlank
    val fullName: String,
    @field:Size(min = 1, max = 12, message = "phone number length should be between 1 and 12")
    @field:NotBlank
    @field:Pattern(regexp = "^[0-9]+\$", message = "phoneNumber should be only numbers")
    val phoneNumber: String,
    @field:Size(min = 1, max = 255, message = "username length should be between 1 and 255")
    @field:NotBlank
    val username: String,
    @field:Size(min = 1, max = 255, message = "password length should be between 1 and 255")
    @field:NotBlank
    val password: String,
    val status: Status,
    @field:Size(min = 1, max = 255, message = "mail length should be between 1 and 255")
    @field:NotBlank
    @field:ValidEmail(message = "must send valid mail")
    val mail: String,
    val avatarPhoto: String? = null,
    @field:Valid val credentials: UserCredentialsRequest
) {
    fun toEntity(phoneNumber: String, passwordEncoder: PasswordEncoder, avatarPhoto: FileAsset?) = User(
        fullName,
        phoneNumber,
        username,
        passwordEncoder.encode(password),
        status,
        mail,
        Role.USER,
        avatarPhoto
    )
}

data class UserAdminRequest(
    @field:Size(min = 1, max = 255, message = "fullName length should be between 1 and 255")
    @field:NotBlank
    val fullName: String,
    @field:Size(min = 1, max = 12, message = "phone number length should be between 1 and 12")
    @field:NotBlank
    @field:Pattern(regexp = "^[0-9]+\$", message = "phoneNumber should be only numbers")
    val phoneNumber: String,
    @field:Size(min = 1, max = 255, message = "username length should be between 1 and 255")
    @field:NotBlank
    val username: String,
    @field:Size(min = 1, max = 255, message = "password length should be between 1 and 255")
    @field:NotBlank
    val password: String,
    val status: Status,
    @field:Size(min = 1, max = 255, message = "mail length should be between 1 and 255")
    @field:NotBlank
    @field:ValidEmail(message = "must send valid mail")
    val mail: String,
    val role: Role,
    val orgStores: Set<OrgStoreRequest>,
    val avatarPhoto: String? = null,
    @field:Valid val credentials: UserCredentialsRequest? = null
)

data class UserUpdateRequest(
    @field:Size(min = 1, max = 255, message = "fullName length should be between 1 and 255")
    val fullName: String?,
    @field:Size(min = 1, max = 255, message = "username length should be between 1 and 255")
    val username: String?,
    @field:Size(min = 1, max = 12, message = "phone number length should be between 1 and 12")
    @field:Pattern(regexp = "^[0-9]+\$", message = "phoneNumber should be only numbers")
    val phoneNumber: String?,
    @field:Size(min = 1, max = 255, message = "mail length should be between 1 and 255")
    @field:ValidEmail(message = "must send valid mail")
    val mail: String?,
    @field:Size(min = 1, max = 255, message = "password length should be between 1 and 255")
    val password: String? = null,
    val avatarPhoto: String? = null,
    @field:Valid val credentials: UserCredentialsRequest? = null
)

data class UserAdminUpdateRequest(
    @field:Size(min = 1, max = 255, message = "fullName length should be between 1 and 255")
    @field:NotBlank
    val fullName: String,
    @field:Size(min = 1, max = 255, message = "phoneNumber length should be between 1 and 255")
    @field:NotBlank
    val phoneNumber: String,
    @field:Size(min = 1, max = 255, message = "username length should be between 1 and 255")
    @field:NotBlank
    val username: String,
    @field:Size(min = 1, max = 255, message = "mail length should be between 1 and 255")
    @field:NotBlank
    @field:ValidEmail(message = "must send valid mail")
    val mail: String,
    val status: Status,
    val password: String? = null,
    val avatarPhoto: String? = null,
    val orgStores: Set<OrgStoreRequest>,
    val role: Role? = null,
    @field:Valid val credentials: UserCredentialsRequest? = null
)

data class UserCredentialsRequest(
    @CheckPinfl
    val pinfl: String,
    @field:Size(min = 1, max = 255, message = "fio length should be between 1 and 255")
    @field:NotBlank
    val fio: String,
    val cardGivenDate: Long,
    val cardExpireDate: Long,
    val cardSerialNumber: String,
    val gender: Gender,
    val birthday: Long,
) {
    fun toEntity(user: User) = UserCredentials(
        pinfl,
        fio,
        Date(cardGivenDate),
        Date(cardExpireDate),
        cardSerialNumber,
        gender,
        Date(birthday),
        user
    )
}

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

data class UserAdminResponse(
    val id: Long,
    val fullName: String,
    val username: String,
    val role: Role,
    val status: Status,
    val phoneNumber: String,
    val mail: String,
    val avatarPhoto: String? = null,
    val organization: List<OrgAdminResponse>,
    @JsonInclude(Include.NON_NULL)
    val credentials: UserCredentialsResponse? = null
) {
    companion object {
        fun toDto(user: User, organization: List<OrgAdminResponse>, credentials: UserCredentials?) = UserAdminResponse(
            user.id!!,
            user.fullName,
            user.username,
            user.role,
            user.status,
            user.phoneNumber,
            user.mail,
            user.avatarPhoto?.hashId,
            organization,
            credentials?.let { UserCredentialsResponse.toDto(it) }
        )
    }
}

data class PinflRequest(
    @CheckPinfl
    val pinfl: String
)

data class FileResponse(
    val hashId: String
)

data class UserOrgStoreResponse(
    val id: Long,
    val orgResponse: OrgResponse,
    val granted: Boolean,
    val role: Role
) {
    companion object {
        fun toResponse(userOrgStore: UserOrgStore) =
            UserOrgStoreResponse(
                userOrgStore.id!!,
                OrgResponse.toDto(userOrgStore.organization),
                userOrgStore.granted,
                userOrgStore.role
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

data class PermissionRequest(
    @field:Size(min = 1, max = 255, message = "permissionData length should be between 1 and 255")
    @field:NotBlank
    @field:NotNull
    val permissionData: String,
    @field:Size(min = 1, max = 255, message = "description length should be between 1 and 255")
    val description: String?
)

data class PermissionAdminResponse(
    val id: Long,
    val permissionData: String,
    val description: String?
) {
    companion object {
        fun toDto(permission: Permission): PermissionAdminResponse {
            return permission.let {
                PermissionAdminResponse(
                    it.id!!,
                    it.permissionData,
                    it.description
                )
            }
        }
    }
}

data class PermissionResponse(
    val id: Long,
    val name: String,
)

data class TourniquetRequest(
    @field:Size(min = 1, max = 15, message = "ip length should be between 1 and 15")
    @field:NotBlank
    @field:ValidIP("Not valid ip address")
    val ip: String,
    @field:Size(min = 1, max = 255, message = "name length should be between 1 and 255")
    @field:NotBlank
    val name: String,
    @field:Size(min = 1, max = 255, message = "username length should be between 1 and 255")
    @field:NotBlank
    val username: String,
    @field:Size(min = 1, max = 255, message = "password length should be between 1 and 255")
    @field:NotBlank
    val password: String,
    val type: TourniquetType,
    @field:Size(min = 1, max = 255, message = "description length should be between 1 and 255")
    val description: String?
)

data class TourniquetUpdateRequest(
    @field:Size(min = 1, max = 15, message = "ip length should be between 1 and 15")
    @field:NotBlank
    @field:ValidIP("Not valid ip address")
    val ip: String,
    @field:Size(min = 1, max = 255, message = "name length should be between 1 and 255")
    @field:NotBlank
    val name: String,
    @field:Size(min = 1, max = 255, message = "username length should be between 1 and 255")
    @field:NotBlank
    val username: String,
    @field:Size(min = 1, max = 255, message = "password length should be between 1 and 255")
    @field:NotBlank
    val password: String? = null,
    @field:Size(min = 1, max = 255, message = "description length should be between 1 and 255")
    val description: String?
)

data class TourniquetDto(
    val id: Long,
    val ip: String,
    val name: String,
    val username: String,
    val password: String,
) {
    companion object {
        fun toDto(tourniquet: Tourniquet, password: String) = tourniquet.run {
            TourniquetDto(
                id!!,
                ip,
                name,
                username,
                password
            )
        }
    }
}

data class TourniquetResponse(
    val id: Long,
    val ip: String,
    val name: String,
    val username: String,
    val description: String?,
    val type: TourniquetType,
    val organization: OrgAdminResponse
) {
    companion object {
        fun toResponse(tourniquet: Tourniquet) = tourniquet.run {
            TourniquetResponse(
                id!!,
                ip,
                name,
                username,
                description,
                type,
                OrgAdminResponse.toDto(tourniquet.organization)
            )
        }
    }
}

data class DailyAttendanceResponse(
    val pinfl: String,
    val fullName: String,
    val positionName: String,
    val startDate: LocalDateTime?,
    val endDate: LocalDateTime?,
    val givenDate: LocalDate
)

data class WorkingDateConfigRequest(
    var startHour: LocalTime,
    var endHour: LocalTime,
    val requiredMinutes: Int,
    var day: Short,
)

data class WorkingDateUpdateRequest(
    var startHour: LocalTime,
    var endHour: LocalTime,
    var requiredMinutes: Int
)

data class WorkingDateConfigResponse(
    val id: Long,
    val startHour: String,
    val endHour: String,
    val day: DayOfWeek,
    val organization: OrgAdminResponse,
    val requiredMinutes: Int
) {
    companion object {
        fun toResponse(config: WorkingDateConfig) = config.run {
            WorkingDateConfigResponse(
                id!!,
                startHour.toString(),
                endHour.toString(),
                day,
                OrgAdminResponse.toDto(organization),
                requiredMinutes
            )
        }
    }
}

data class BoardRequest(
    @field:Size(min = 1, max = 255, message = "name length should be between 1 and 255")
    val name: String,
    val projectId: Long
)

data class BoardUpdateRequest(
    @field:Size(min = 1, max = 255, message = "name length should be between 1 and 255")
    val name: String,
    val status: BoardStatus
)

data class BoardResponse(
    val id: Long,
    val name: String,
    val projectId: Long,
    val ownerId: Long? = null,
    val ownerName: String? = null,
    val status: BoardStatus
) {
    companion object {
        fun toDto(board: Board) = BoardResponse(
            board.id!!,
            board.name,
            board.project.id!!,
            board.owner.id,
            board.owner.user?.fullName,
            board.status
        )
    }
}

data class BoardAdminResponse(
    val id: Long,
    val name: String,
    val projectId: Long,
    val status: ProjectStatus,
    val ownerId: Long? = null,
    val ownerName: String? = null,
    val taskAmount: Long,
    val projectOpenTaskAmount: Long,
    val employeeOpenTaskAmount: Long? = null
)

data class ProjectEmployeeRequest(
    val projectId: Long,
    val employeeIds: MutableSet<Long>,
    val role: ProjectEmployeeRole
)

data class ProjectEmployeeEditRequest(
    val role: ProjectEmployeeRole
)

data class ProjectEmployeeResponse(
    val id: Long,
    val projectId: Long,
    val employee: EmployeeResponse,
    val role: ProjectEmployeeRole
) {
    companion object {
        fun toDto(entity: ProjectEmployee) = ProjectEmployeeResponse(
            entity.id!!,
            entity.project.id!!,
            EmployeeResponse.toDto(entity.employee),
            entity.projectEmployeeRole
        )
    }
}


data class StateRequest(
    @field:Size(min = 1, max = 128, message = "name length should be between 1 and 128")
    val name: String,
    val boardId: Long
)

data class StateUpdateRequest(
    @field:Size(min = 1, max = 128, message = "name length should be between 1 and 128")
    val name: String?,
    val order: Short?
)

data class StateSearch(
    val boardId: Long,
    val search: String?,
    val meMode: Boolean,
    val employeeIds: List<Long>?
)

data class StateResponse(
    val id: Long,
    val name: String,
    val order: Short,
    val boardId: Long,
    val tasks: List<TaskResponse>?
) {
    companion object {
        fun toDto(state: State, tasks: List<TaskResponse>? = mutableListOf()) = StateResponse(
            state.id!!,
            state.name,
            state.order,
            state.board.id!!,
            tasks
        )
    }
}


data class CommentRequest(
    val taskId: Long,
    val text: String,
    val files: List<String>? = null
)

data class CommentResponse(
    val id: Long,
    val taskId: Long,
    val text: String,
    val owner: UserDto,
    val files: List<String>? = mutableListOf()
) {
    companion object {
        fun toDto(entity: Comment) = entity.run {
            CommentResponse(
                id!!,
                task.id!!,
                text,
                UserDto.toResponse(owner),
                files?.map { it.hashId }
            )
        }
    }
}


data class TaskCreateRequest(
    @field:Size(min = 1, max = 225, message = "title length should be between 1 and 225")
    val title: String,
    val employeeIds: List<Long>,
    val stateId: Long,
    val priority: TaskPriority,
    val startDate: Long?,
    val endDate: Long?,
    val filesHashIds: List<String>,
    val description: String? = null,
    val timeEstimateAmount: Int?,
    val parentTaskId: Long? = null
)

data class TaskUpdateRequest(
    @field:Size(min = 1, max = 225, message = "title length should be between 1 and 225")
    val title: String?,
    val priority: TaskPriority?,
    val filesHashIds: List<String>?,
    val employeeIds: List<Long>?,
    val startDate: Long?,
    val endDate: Long?,
    val description: String? = null,
    val timeEstimateAmount: Int?
)

data class TaskDeleteRequest(
    val isSubtask: Boolean,
    val taskIds: MutableList<Long>
)

data class TaskAdminResponse(
    val id: Long,
    val title: String,
    val employees: List<ProjectEmployeeResponse> = mutableListOf(),
    val state: StateResponse,
    val board: BoardResponse,
    var priority: PriorityResponse,
    var files: List<String> = mutableListOf(),
    var order: Short,
    var startDate: Date? = null,
    var endDate: Date? = null,
    var description: String? = null,
    var parentTaskId: Long? = null,
    val timeEstimateAmount: Int? = null,
    val subTasks: List<TaskAdminResponse>? = null
) {
    companion object {
        fun toDto(task: Task, subtasks: List<Task>? = null): TaskAdminResponse {
            return TaskAdminResponse(
                task.id!!,
                task.title,
                task.employees.map { ProjectEmployeeResponse.toDto(it) }.sortedBy { it.id },
                StateResponse.toDto(task.state),
                BoardResponse.toDto(task.board),
                PriorityResponse.toResponse(task.priority),
                task.files.map { it.hashId },
                task.order,
                task.startDate,
                task.endDate,
                task.description,
                task.parentTask?.id,
                task.timeEstimateAmount,
                subtasks?.map { toDto(it) }
            )
        }
    }
}

data class TaskStatResponse(
    val id: Long,
    val taskTitle: String,
    val taskOrder: Short,
    val projectId: Long,
    val taskPriority: TaskPriority?,
    val projectName: String?,
    val boardId: Long,
    val boardName: String,
    val stateId: Long,
    val stateName: String,
    val stateOrder: Short?,
    val startDate: Date?,
    val endDate: Date?
) {
    companion object {
        fun toDto(task: Task) =
            TaskStatResponse(
                task.id!!,
                task.title,
                task.order,
                task.board.project.id!!,
                task.priority,
                task.board.project.name,
                task.board.id!!,
                task.board.name,
                task.state.id!!,
                task.state.name,
                task.state.order,
                task.startDate,
                task.endDate
            )
    }
}

data class TypedTaskResponse(
    var openedTasks: List<TaskStatResponse> = mutableListOf(),
    var closedTasks: List<TaskStatResponse> = mutableListOf(),
    var upcomingTasks: List<TaskStatResponse> = mutableListOf()
)

data class TaskResponse(
    val id: Long,
    val title: String,
    var priority: TaskPriority,
    var order: Short,
    var stateId: Long?,
    var stateOrder: Short?,
    var boardId: Long?,
    var boardName: String?,
    var files: List<FileDataResponse>? = null,
    val ownerName: String? = null,
    val ownerId: Long? = null,
    val ownerPhotoHashId: String? = null,
    var startDate: Date? = null,
    var endDate: Date? = null,
    var description: String? = null,
    var parentTaskId: Long? = null,
    val employees: List<TaskEmployeeResponse>? = null,
    val timeEstimateAmount: Int? = null,
    val subTasks: List<TaskResponse>? = null,
) {
    companion object {
        fun toDto(
            task: Task,
            fileResponse: List<FileDataResponse>? = null,
            subtasks: List<TaskResponse>? = null,
            ownerPhotoHashId: String? = null
        ) =
            TaskResponse(
                task.id!!,
                task.title,
                task.priority,
                task.order,
                task.state.id,
                task.state.order,
                task.board.id,
                task.board.name,
                fileResponse,
                task.owner.fullName,
                task.owner.id,
                ownerPhotoHashId,
                task.startDate,
                task.endDate,
                task.description,
                task.parentTask?.id,
                task.employees.map { TaskEmployeeResponse.toResponse(it) },
                task.timeEstimateAmount,
                subtasks,
            )
    }
}

data class PriorityResponse(
    val priority: TaskPriority,
    val localizedName: String
) {
    companion object {
        fun toResponse(priority: TaskPriority) = priority.run {
            PriorityResponse(this, localizedName.localized())
        }
    }
}

data class FileDataResponse(
    val hashId: String,
    val fileSize: Long,
    val fileContentType: String,
    val fileName: String
) {
    companion object {
        fun toResponse(file: FileAsset) = FileDataResponse(
            hashId = file.hashId,
            fileSize = file.fileSize,
            fileContentType = file.fileContentType,
            fileName = file.fileName
        )
    }
}

data class TaskEmployeeResponse(
    val id: Long? = null,
    val projectEmployeeId: Long? = null,
    val fullName: String? = null,
    val imageHashId: String? = null
) {
    companion object {
        fun toResponse(projectEmployee: ProjectEmployee) = projectEmployee.run {
            TaskEmployeeResponse(
                employee.id,
                id,
                employee.user?.fullName,
                employee.imageAsset?.hashId
            )
        }
    }
}

interface StatisticResponse {
    val departmentAmount: Long
    val positionAmount: Long
    val totalEmployeeAmount: Long
    val busyEmployeeAmount: Long
    val vacantEmployeeAmount: Long
    val maleEmployeesAmount: Long
    val femaleEmployeesAmount: Long
}

interface EmployeeAgeStatisticResponse {
    val underThirty: Long
    val betweenThirtyAndForty: Long
    val overForty: Long
}

interface UserWorkingStatisticResponse {
    val lateDaysAmount: Long
    val earlyDaysAmount: Long
    val absentDaysAmount: Long
}


data class TaskActionHistoryRequest(
    val taskId: Long,
    val ownerId: Long,
    val action: TaskAction,
    val fileHashIds: List<String>? = null,
    val fromStateId: Long? = null,
    val toStateId: Long? = null,
    val subjectEmployeeIds: List<Long>? = null,
    val taskPriority: TaskPriority? = null,
    val dueDate: Date? = null,
    val startDate: Date? = null,
    val description: String? = null,
    val title: String? = null,
    val order: Short? = null,
    val commentId: Long? = null,
    val timeTrackingIds: List<Long>? = null,
    val timeEstimateAmount: Int? = null,
    val subtasks: MutableList<Long>? = null
)

data class TaskActionHistoryResponse(
    val id: Long,
    val taskId: Long,
    val owner: UserDto,
    val action: TaskAction,
    val fileHashIds: List<String>? = null,
    val fromState: StateResponse? = null,
    val toState: StateResponse? = null,
    val subjectEmployee: EmployeeResponse? = null,
    val createdAt: Long,
    val taskPriority: PriorityResponse? = null,
    val dueDate: Date? = null,
    val startDate: Date? = null,
    val title: String? = null,
    val comment: CommentResponse? = null,
    val timeTracking: TimeTrackingResponse? = null,
    val timeEstimateAmount: Int? = null,
) {
    companion object {
        fun toDto(entity: TaskActionHistory) = entity.run {
            TaskActionHistoryResponse(
                id!!,
                task.id!!,
                UserDto.toResponse(owner),
                action,
                files?.map { it.hashId },
                fromState?.let { StateResponse.toDto(it) },
                toState?.let { StateResponse.toDto(it) },
                subjectEmployee?.let { EmployeeResponse.toDto(it) },
                createdDate!!.time,
                priority?.let { PriorityResponse.toResponse(it) },
                dueDate,
                startDate,
                title,
                comment?.let { CommentResponse.toDto(it) },
                timeTracking?.let { TimeTrackingResponse.toDto(it) },
                timeEstimateAmount
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

data class EventResponse(
    @JsonProperty("dateTime") val dateTime: Date,
    @JsonProperty("macAddress") val macAddress: String? = null,
    @JsonProperty("portNo") val portNo: Int? = null,
    @JsonProperty("protocol") val protocol: String? = null,
    @JsonProperty("eventState") val eventState: String? = null,
    @JsonProperty("eventDescription") val eventDescription: String? = null,
    @JsonProperty("ipAddress") val ipAddress: String? = null,
    @JsonProperty("activePostCount") val activePostCount: Int? = null,
    @JsonProperty("eventType") val eventType: String? = null,
    @JsonProperty("channelID") val channelID: Int? = null,
    @JsonProperty("AccessControllerEvent") val accessControllerEvent: AccessControllerEvent? = null
) {
    override fun toString(): String {
        return "EventResponse(dateTime=$dateTime," +
                "\n macAddress=$macAddress," +
                "\n portNo=$portNo," +
                "\n protocol=$protocol," +
                "\n eventState=$eventState," +
                "\n eventDescription=$eventDescription," +
                "\n ipAddress=$ipAddress," +
                "\n activePostCount=$activePostCount," +
                "\n eventType=$eventType," +
                "\n channelID=$channelID," +
                "\n accessControllerEvent=$accessControllerEvent)"
    }
}

data class FaceRect(
    @JsonProperty("width") val width: Any? = null,
    @JsonProperty("x") val x: Any? = null,
    @JsonProperty("y") val y: Any? = null,
    @JsonProperty("height") val height: Any? = null
) {
    override fun toString(): String {
        return "FaceRect(width=$width, x=$x, y=$y, height=$height)"
    }
}

data class AccessControllerEvent(
    @JsonProperty("cardReaderKind") val cardReaderKind: Int? = null,
    @JsonProperty("frontSerialNo") val frontSerialNo: Int? = null,
    @JsonProperty("cardReaderNo") val cardReaderNo: Int? = null,
    @JsonProperty("statusValue") val statusValue: Int? = null,
    @JsonProperty("majorEventType") val majorEventType: Int? = null,
    @JsonProperty("employeeNoString") val employeeNoString: String? = null,
    @JsonProperty("label") val label: String? = null,
    @JsonProperty("deviceName") val deviceName: String,
    @JsonProperty("currentVerifyMode") val currentVerifyMode: String? = null,
    @JsonProperty("serialNo") val serialNo: Int? = null,
    @JsonProperty("purePwdVerifyEnable") val purePwdVerifyEnable: Boolean? = null,
    @JsonProperty("FaceRect") val faceRect: FaceRect? = null,
    @JsonProperty("subEventType") val subEventType: Int? = null,
    @JsonProperty("name") val name: String? = null,
    @JsonProperty("verifyNo") val verifyNo: Int? = null,
    @JsonProperty("userType") val userType: String? = null,
    @JsonProperty("picturesNumber") val picturesNumber: Int? = null,
    @JsonProperty("attendanceStatus") val attendanceStatus: String? = null,
    @JsonProperty("mask") val mask: String? = null
) {
    override fun toString(): String {
        return "AccessControllerEvent(cardReaderKind=$cardReaderKind," +
                "\n frontSerialNo=$frontSerialNo," +
                "\n cardReaderNo=$cardReaderNo," +
                "\n statusValue=$statusValue," +
                "\n majorEventType=$majorEventType," +
                "\n employeeNoString=$employeeNoString," +
                "\n label=$label," +
                "\n deviceName='$deviceName'," +
                "\n currentVerifyMode=$currentVerifyMode," +
                "\n serialNo=$serialNo," +
                "\n purePwdVerifyEnable=$purePwdVerifyEnable," +
                "\n faceRect=$faceRect," +
                "\n subEventType=$subEventType," +
                "\n name=$name," +
                "\n verifyNo=$verifyNo," +
                "\n userType=$userType," +
                "\n picturesNumber=$picturesNumber," +
                "\n attendanceStatus=$attendanceStatus," +
                "\n mask=$mask)"
    }
}


data class TourniquetEmployeeUpdateRequest(
    val eventId: Long,
    val success: Boolean,
    val error: String? = null
)

data class SynchronizationData(
    val tourniquetId: Long,
    val employees: MutableList<EmployeeSyncRequest>
)

data class EmployeeSyncRequest(
    val id: Long,
    val name: String,
)


data class VisitorDeleteRequest(
    val organizationId: Long,
    val visitors: List<String>
)


data class UserTourniquetRequest(
    val name: String,
    val employeeId: String,
    val dateTime: Date,
    val userType: String,
)

@JsonInclude(Include.NON_NULL)
data class FeignRequest<Payload>(
    var url: String,
    var password: String,
    var username: String,
    var payload: Payload,
)

data class FeignResponse(
    val statusCode: String?,
    val statusString: String?,
    val subStatusCode: String?,
    @JsonProperty("error_code")
    val errorCode: Int?,
    val status_code: Int?,
    val data: Data?
)

data class Data(
    val errorCode: Int?,
    val errorMsg: String?,
    val statusCode: Int?,
    val statusString: String?,
    val subStatusCode: String?,
    val status_code: Int?,
    val message: String?,
)

data class PayloadAdd(
    @JsonProperty("UserInfo")
    val addInfo: UserInfoAddRequest
)

data class PayloadEdit(
    @JsonProperty("UserInfo")
    val editInfo: UserInfoEditRequest
)

data class PayloadSearch(
    @JsonProperty("UserInfoSearchCond")
    val userInfoSearchCond: UserInfoSearchCond? = null
)

data class UserInfoSearchCond(
    val searchID: String,
    val searchResultPosition: Long,
    val maxResults: Int
)

data class UserInfoCount(
    val bindCardUserNumber: Long,
    val bindFaceUserNumber: Long,
    val userNumber: Long,
)

data class TourniquetEmployeeResponse(
    var name: String,
    var userType: String,
    var gender: String,
    var employeeResponse: EmployeeResponse? = null,
    val employeeNo: String,
    @JsonProperty("Valid")
    var valid: ValidResponse,
    val password: String? = null
) {
    companion object {
        fun toDto(userInfo: UserInfoResponse, employee: Employee? = null) = userInfo.run {
            TourniquetEmployeeResponse(
                name,
                userType,
                gender,
                employee?.let {
                    EmployeeResponse.toDto(it)
                },
                employeeNo,
                valid,
                password
            )
        }
    }
}

data class Root(
    @JsonProperty("UserInfoSearch")
    val userInfoSearch: UserInfoSearch,
)

data class UserInfoSearch(
    @JsonProperty("UserInfo")
    val userInfo: MutableList<UserInfoResponse> = mutableListOf(),
    val numOfMatches: Long,
    val responseStatusStrg: String,
    @JsonProperty("searchID")
    val searchId: String,
    val totalMatches: Long,
)

data class UserInfoAddRequest(
    val employeeNo: String,
    var name: String,
    @JsonProperty("Valid")
    var valid: ValidRequest,
    var userType: String,
    val gender: String,
) {
    companion object {
        fun userInfo(
            hashPinfl: String,
            credentials: UserCredentials,
            type: TourniquetEmployeeType
        ) = UserInfoAddRequest(
            hashPinfl,
            credentials.user.fullName,
            validTimeRequest(),
            type.toLower(),
            credentials.gender.toLower(),
        )
    }
}

data class UserInfoEditRequest(
    val employeeNo: String,
    val name: String,
    @JsonProperty("Valid")
    val valid: ValidRequest,
    val userType: String
)

data class UserInfoResponse(
    val employeeNo: String,
    val name: String,
    @JsonProperty("Valid")
    val valid: ValidResponse,
    val userType: String,
    val gender: String,
    val password: String,
)

data class ValidRequest(
    val beginTime: String,
    val enable: Boolean,
    val endTime: String,
) {
    companion object {
        fun validTimeRequest(): ValidRequest {
            val now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)
            val tenYearsLater = now.plusYears(10)
            return ValidRequest(now.toString(), true, tenYearsLater.toString())
        }
    }
}


data class TimeTrackingCreateRequest(
    val taskId: Long,
    val startDate: Long?,
    val endDate: Long?,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val note: String?
)


data class TimeTrackingUpdateRequest(
    val startTime: LocalTime,
    val endTime: LocalTime,
    val note: String?
)

data class TimeTrackingResponse(
    val id: Long,
    val taskId: Long,
    val duration: Long,
    val endTime: String,
    val startTime: String,
    val tableDate: TableDateResponse,
    val note: String?
) {
    companion object {
        fun toDto(entity: TimeTracking) = entity.run {
            TimeTrackingResponse(
                id!!,
                task.id!!,
                duration,
                endTime.prettyString(),
                startTime.prettyString(),
                TableDateResponse.toDto(tableDate),
                note
            )
        }
    }
}

data class OwnerShortResponse(
    val id: Long,
    val name: String
) {
    companion object {
        fun toDto(entity: User) = entity.run { OwnerShortResponse(id!!, fullName) }
    }
}

data class TimeTrackingListResponse(
    val owner: OwnerShortResponse,
    val timeList: List<TimeTrackingResponse>
) {
    companion object {
        fun toDto(owner: User, list: List<TimeTracking>) = TimeTrackingListResponse(
            OwnerShortResponse.toDto(owner),
            list.map { TimeTrackingResponse.toDto(it) }
        )
    }
}

data class StateTemplateRequest(
    @field:Size(min = 1, max = 128, message = "name length should be between 1 and 128")
    val name: String,
    val status: Status
)

data class StateTemplateResponse(
    val id: Long,
    val name: String,
    val status: Status
) {
    companion object {
        fun toDto(stateTemplate: StateTemplate) = StateTemplateResponse(
            stateTemplate.id!!,
            stateTemplate.name,
            stateTemplate.status
        )
    }
}

data class TableDateRequest(
    val date: Long,
    val type: TableDateType,
    val organizationId: Long = 0
)

data class TableDateUpdateRequest(
    val type: TableDateType
)

data class TableDateResponse(
    val id: Long,
    val date: Long,
    val type: TableDateType,
    val organizationId: Long,
    val countAbsenceTracker: Int? = null
) {
    companion object {
        fun toDto(tableDate: TableDate, countAbsenceTracker: Int? = null) = TableDateResponse(
            tableDate.id!!,
            tableDate.date.time,
            tableDate.type,
            tableDate.organization.id!!,
            countAbsenceTracker
        )
    }
}

data class ValidResponse(
    val beginTime: String,
    val enable: Boolean,
    val endTime: String,
    val timeType: String,
)


data class FaceResponse(
    @JsonProperty("MatchList")
    val matchList: List<MatchList>? = null,
    val numOfMatches: Long?,
    val responseStatusStrg: String?,
    val statusCode: Long?,
    val statusString: String?,
    val subStatusCode: String?,
    val totalMatches: Long?,
    val data: Data?,
    @JsonProperty("error_code")
    val errorCode: Int?,
    val status_code: Int?,
)

data class MatchList(
    @JsonProperty("FPID")
    val employeeId: String,
    val faceURL: String,
    val modelData: String
)

data class UserOrgStoreRequest(
    val userId: Long,
    val orgId: Long,
    val granted: Boolean
)

data class OrgStoreRequest(
    val orgId: Long,
    val granted: Boolean
)

data class UserSessionResponse(
    val sessionId: String,
    val userId: Long,
    val orgResponse: OrgResponse? = null,
    val role: Role,
    @JsonFormat(shape = JsonFormat.Shape.NUMBER) val expiresAt: Date
) {
    companion object {
        fun toResponse(session: UserOrgSession, organization: Organization?) = session.run {
            UserSessionResponse(
                id!!,
                userId,
                organization?.let { OrgResponse.toDto(it) },
                role,
                Date(session.createdDate.time + Duration.ofMinutes(2).toMillis())
            )
        }
    }
}


data class UserTourniquetResponse(
    val userId: Long,
    val fullName: String,
    val userPinfl: String,
    val position: String,
    val events: List<EventData>
)

data class EventData(
    val type: UserTourniquetType,
    val eventTime: Date,
)

data class EventPairData(
    val eventIn: Date?,
    val eventInHashId: String?,
    val eventOut: Date?,
    val eventOutHashId: String?,
)

data class WorkingMinutesResponse(
    val userId: Long,
    val fullName: String,
    val dataList: List<DataResponse>
)

data class DataResponse(
    val workingDate: Long,
    val tableDateId: Long,
    val workingMinutes: Int,
    val eventAmount: Long,
    val dayType: TableDateType,
    val userAbsenceTrackerId: Long? = null
)

data class EmployeeKPIResponse(
    val userId: Long,
    val fullName: String,
    val dataList: List<KPIDataResponse>
)

data class KPIDataResponse(
    val workingDate: Long,
    val dayType: TableDateType,
    val workingMinutes: Int,
    val trackedMinutes: Int,
    val requiredMinutes: Int,
    val estimatedMinutes: Int,
    val tableDateId: Long
)

data class ExcelDataResponse(
    val workingDate: Date,
    val tableDateId: Long? = null,
    var workingMinutes: Int? = null,
    val dayType: TableDateType? = null,
    val tourniquetData: List<TourniquetData> = mutableListOf()
) {
    data class TourniquetData(
        val id: Long,
        val type: UserTourniquetType,
        val time: Date
    )
}

data class ExcelWorkingMinutesResponse(
    val userId: Long,
    val fullName: String,
    val requiredMinutes: Int,
    val positionName: String,
    val workSummary: List<ExcelDataResponse>
)

data class DepartmentDataResponse(
    val departmentName: String,
    val userSummary: List<ExcelWorkingMinutesResponse>
)

data class AbsentEvents(
    val userId: Long,
    val fullName: String,
    val userPinfl: String,
    val position: String,
    val department: String,
    val events: List<EventData>
)

data class EmployeeDataRequest(
    val dataList: MutableList<DataRequest>
)

data class DataRequest(
    val dataId: Long,
    val status: EmployeeStatus
)

data class TourniquetEmployeeDto(
    val id: Long,
    val fullName: String,
    val phStatus: PositionHolderStatus,
    val imageHashId: String?,
    val tourniquetData: List<DataResponse>
) {
    data class DataResponse(
        val tourniquetEmployeeId: Long,
        val status: EmployeeStatus,
        val tourniquetId: Long,
        val ip: String,
        val name: String,
        val tourniquetType: TourniquetType,
        val date: Date,
        val message: String? = null
    )

    companion object {
        fun toResponse(dataMap: Map<Employee, List<EmployeeTourniquetData>>): List<TourniquetEmployeeDto> {
            return dataMap.map { mapElement ->
                mapElement.key.run {
                    TourniquetEmployeeDto(
                        id!!,
                        user?.fullName ?: (position.name + "(${workRate})"),
                        phStatus,
                        imageAsset?.hashId,
                        mapElement.value.map { data ->
                            DataResponse(
                                tourniquetEmployeeId = data.id!!,
                                status = data.status,
                                tourniquetId = data.tourniquet.id!!,
                                ip = data.tourniquet.ip,
                                name = data.tourniquet.name,
                                tourniquetType = data.tourniquet.type,
                                date = data.date,
                                message = data.message
                            )
                        }
                    )
                }
            }
        }

    }
}


data class UpdaterDataResponse(
    val employeeId: Long,
    val employeeName: String? = null,
    val image: String? = null,
)

data class HikVisionResponse(
    val eventId: Long,
    val type: HikVisionEventType,
    val tourniquetId: Long,
    val data: UpdaterDataResponse
)

data class UnknownPersonResponse(
    val tourniquetName: String?,
    var image: String?,
    val createdAt: Date = Date()
) {
    companion object {
        fun toDto(unknownPerson: UnknownPerson) = UnknownPersonResponse(
            unknownPerson.tourniquetName,
            unknownPerson.image,
            unknownPerson.createdAt
        )
    }
}

data class UserTourniquetResultResponse(
    val tourniquetName: String,
    val employeeId: String,
    val dateTime: Date,
    val type: ResultType,
    val message: String
) {
    companion object {
        fun toResponse(result: UserTourniquetResult) = result.run {
            UserTourniquetResultResponse(
                tourniquetName,
                employeeId,
                dateTime, type, message
            )
        }
    }
}

data class ClientRequest(
    @field:Size(min = 1, max = 255, message = "username length should be between 1 and 255")
    @field:NotBlank
    val username: String,
    @field:Size(min = 1, max = 255, message = "password length should be between 1 and 255")
    @field:NotBlank
    val password: String,
)

data class ClientUpdateRequest(
    @field:Size(min = 1, max = 255, message = "username length should be between 1 and 255")
    @field:NotBlank
    val username: String,
    @field:Size(min = 1, max = 255, message = "password length should be between 1 and 255")
    val password: String? = null,
)

data class ClientResponse(
    val id: Long,
    val username: String
) {
    companion object {
        fun toResponse(client: TourniquetClient) = client.run {
            ClientResponse(
                id!!,
                username
            )
        }
    }
}

data class OtpDto(
    val hash: String,
    val botUsername: String,
    val retryTimeMin: Int,
    val expireOtpMin: Int
)

data class SubscriberResponse(
    val id: Long,
    val chatId: String,
    val username: String? = null,
    val language: Language,
    val user: UserDto,
) {
    companion object {
        fun toResponse(subscriber: Subscriber) = subscriber.run {
            SubscriberResponse(
                id!!,
                chatId,
                username,
                language,
                UserDto.toResponse(user)
            )
        }
    }
}

data class SubscriberRequest(
    val language: Language
)

data class TaskSubscriberRequest(
    val taskId: Long,
    val actions: MutableSet<TaskAction>
)

data class TaskSubscriberUpdateRequest(
    val id: Long,
    val actions: MutableSet<TaskAction>
)

data class TaskSubscriberResponse(
    val id: Long,
    val user: UserDto,
    val task: TaskResponse,
) {
    companion object {
        fun toResponse(taskSubscriber: TaskSubscriber) = taskSubscriber.run {
            TaskSubscriberResponse(
                id!!,
                UserDto.toResponse(taskSubscriber.subscriber.user),
                TaskResponse.toDto(task)
            )
        }
    }
}

data class BoardSettingsRequest(
    val boardId: Long,
    val actions: MutableSet<TaskAction>
)

data class BoardSettingsUpdateRequest(
    val actions: MutableSet<TaskAction>
)

data class BoardSettingsResponse(
    val id: Long,
    val board: BoardResponse,
    val actions: MutableSet<TaskAction>
) {
    companion object {
        fun toResponse(settings: BoardNotificationSettings) = settings.run {
            BoardSettingsResponse(
                id!!,
                BoardResponse.toDto(board),
                actions
            )
        }
    }
}

data class UserAbsenceTrackerResponse(
    val id: Long,
    val userId: Long,
    val tableDateResponse: TableDateResponse,
    val eventType: EventType,
    val fileDataResponse: FileDataResponse? = null,
    val description: String?
) {
    companion object {
        fun toDto(
            entity: UserAbsenceTracker,
            tableDateResponse: TableDateResponse,
            fileDataResponse: FileDataResponse? = null
        ): UserAbsenceTrackerResponse =
            UserAbsenceTrackerResponse(
                id = entity.id!!,
                userId = entity.user.id!!,
                tableDateResponse = tableDateResponse,
                eventType = entity.eventType,
                fileDataResponse = fileDataResponse,
                description = entity.description
            )
    }
}

data class UserAbsenceTrackerRequest(
    val tableDateId: Long,
    val eventType: EventType,
    val fileHashId: String?,
    val description: String?
)

data class UserAbsenceTrackerAdminResponse(
    val id: Long,
    val userId: Long,
    val employeeId: Long,
    val fullName: String,
    val positionName: String,
    val avatarHashId: String?,
    val eventType: EventType,
    val fileDataResponse: FileDataResponse? = null,
    val description: String?
) {
    companion object {
        fun toDto(
            entity: UserAbsenceTracker,
            employee: Employee,
            fileDateResponse: FileDataResponse?
        ): UserAbsenceTrackerAdminResponse {
            return UserAbsenceTrackerAdminResponse(
                id = entity.id!!,
                userId = entity.user.id!!,
                employeeId = employee.id!!,
                fullName = entity.user.fullName,
                positionName = employee.position.name,
                avatarHashId = employee.imageAsset?.hashId,
                eventType = entity.eventType,
                fileDataResponse = fileDateResponse,
                description = entity.description
            )
        }
    }
}


data class TotalWorkMinutesAndDayResponse(
    var totalWorkingMinutes: BigDecimal? = null,
    var totalWorkingDays: Int? = null,
)

data class TotalTaskInfoResponse(
    var totalWithoutEstimateAmount: Int? = null,
    var totalTaskMinutesAlreadyDone: BigDecimal? = null,
    var totalTaskMinutesNeedToDone: BigDecimal? = null
)

data class WorkLatencyResponse(
    var lateCount: Int? = null,
    var totalLateMinutes: BigDecimal? = null,
)

data class EmployeeTotalStatistics(
    var imageHashId: String? = null,
    var fullName: String,
    var department: String,
    var workMinutes: BigDecimal? = null,
    var workDay: Int? = null,
    var totalWithoutEstimateAmount: Int? = null,
    var totalTaskMinutesAlreadyDone: BigDecimal? = null,
    var totalTaskMinutesNeedToDone: BigDecimal? = null,
    var lateCount: Int? = null,
    var totalLateMinutes: BigDecimal? = null,
)