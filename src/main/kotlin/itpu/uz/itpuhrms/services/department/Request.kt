package itpu.uz.itpuhrms.services.department

import itpu.uz.itpuhrms.DepartmentType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

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