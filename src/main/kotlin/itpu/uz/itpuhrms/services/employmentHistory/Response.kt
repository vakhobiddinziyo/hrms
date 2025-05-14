package itpu.uz.itpuhrms.services.employmentHistory

import itpu.uz.itpuhrms.*
import itpu.uz.itpuhrms.services.employee.DepartmentShortResponse
import itpu.uz.itpuhrms.services.employee.EmployeeResponse
import itpu.uz.itpuhrms.services.position.OrgResponse
import itpu.uz.itpuhrms.services.position.PositionResponse
import itpu.uz.itpuhrms.services.user.UserResponse
import java.util.*

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