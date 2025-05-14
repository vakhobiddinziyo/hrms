package itpu.uz.itpuhrms.services.organization

import itpu.uz.itpuhrms.Status
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

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