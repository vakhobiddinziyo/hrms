package itpu.uz.itpuhrms.services.board


import itpu.uz.itpuhrms.BoardStatus
import jakarta.validation.constraints.Size

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
