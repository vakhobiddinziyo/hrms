package itpu.uz.itpuhrms.services.state

import jakarta.validation.constraints.Size

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