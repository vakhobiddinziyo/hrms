package itpu.uz.itpuhrms.services.employee

import itpu.uz.itpuhrms.Status
import itpu.uz.itpuhrms.config.ValidWorkRate
import jakarta.validation.constraints.Positive


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