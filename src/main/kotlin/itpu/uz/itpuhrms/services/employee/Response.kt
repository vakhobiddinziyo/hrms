package itpu.uz.itpuhrms.services.employee

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import itpu.uz.itpuhrms.*
import itpu.uz.itpuhrms.services.department.DepartmentResponse
import itpu.uz.itpuhrms.services.department.Structure
import itpu.uz.itpuhrms.services.position.OrgResponse
import itpu.uz.itpuhrms.services.position.PositionResponse
import itpu.uz.itpuhrms.services.user.UserCredentialsResponse
import itpu.uz.itpuhrms.services.user.UserResponse
import itpu.uz.itpuhrms.utils.DateToLongSerializer
import org.springframework.data.domain.PageImpl
import java.util.*

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


data class LateEmployeeResponse(
    val id: Long,
    val userId: Long,
    val name: String,
    val lateDate: Long,
    val imageHashId: String?
)


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



data class AmountEmployeesResponse(
    val amountOfficeEmployees: Long = 0L,
    val amountNotOfficeEmployees: Long = 0L

)


data class LaborActivityResponse(
    val firstHiredDate: Long,
    val currentHiredDate: Long,
    val lastFiredDate: Long
)


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

data class EmployeeKPIResponse(
    val userId: Long,
    val fullName: String,
    val dataList: List<KPIDataResponse>
)

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


data class KPIDataResponse(
    val workingDate: Long,
    val dayType: TableDateType,
    val workingMinutes: Int,
    val trackedMinutes: Int,
    val requiredMinutes: Int,
    val estimatedMinutes: Int,
    val tableDateId: Long
)

data class EmployeeBirthdateResponse(
    val id: Long,
    val imageHashId: String?,
    val birthday: Date,
    val fullName: String,
    val departmentName: String
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