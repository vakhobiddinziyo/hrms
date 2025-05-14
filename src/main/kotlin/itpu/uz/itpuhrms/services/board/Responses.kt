package itpu.uz.itpuhrms.services.board

import itpu.uz.itpuhrms.Board
import itpu.uz.itpuhrms.BoardStatus
import itpu.uz.itpuhrms.ProjectStatus

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
