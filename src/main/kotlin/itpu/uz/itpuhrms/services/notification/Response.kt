package itpu.uz.itpuhrms.services.notification

import itpu.uz.itpuhrms.BoardNotificationSettings
import itpu.uz.itpuhrms.TaskAction
import itpu.uz.itpuhrms.services.board.BoardResponse

data class BoardSettingsResponse(
    val id: Long,
    val board: BoardResponse,
    val actions: MutableSet<TaskAction>
) {
    companion object {
        fun toResponse(settings: BoardNotificationSettings) = settings.run {
            BoardSettingsResponse(
                id!!,
                BoardResponse.toDto(board),
                actions
            )
        }
    }
}
