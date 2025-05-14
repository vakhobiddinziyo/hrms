package itpu.uz.itpuhrms.services.stateTemplate

import itpu.uz.itpuhrms.Status
import jakarta.validation.constraints.Size

data class StateTemplateRequest(
    @field:Size(min = 1, max = 128, message = "name length should be between 1 and 128")
    val name: String,
    val status: Status
)