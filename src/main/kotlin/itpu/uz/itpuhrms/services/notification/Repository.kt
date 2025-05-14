package itpu.uz.itpuhrms.services.notification

import itpu.uz.itpuhrms.BoardNotificationSettings
import itpu.uz.itpuhrms.base.BaseRepository
import org.springframework.stereotype.Repository

@Repository
interface BoardNotificationSettingsRepository : BaseRepository<BoardNotificationSettings> {
    fun existsByBoardIdAndDeletedFalse(boardId: Long): Boolean
    fun findByBoardIdAndDeletedFalse(boardId: Long): BoardNotificationSettings?
}
