package itpu.uz.itpuhrms.services.permission

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class PermissionRequest(
    @field:Size(min = 1, max = 255, message = "permissionData length should be between 1 and 255")
    @field:NotBlank
    @field:NotNull
    val permissionData: String,
    @field:Size(min = 1, max = 255, message = "description length should be between 1 and 255")
    val description: String?
)