package itpu.uz.itpuhrms.services.userAbsenceTracker

import itpu.uz.itpuhrms.UserAbsenceTracker
import itpu.uz.itpuhrms.base.BaseRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
interface UserAbsenceTrackerRepository : BaseRepository<UserAbsenceTracker> {

    fun existsByIdAndUserIdAndDeletedFalse(trackerId: Long, userId: Long): Boolean

    fun existsByUserIdAndTableDateIdAndDeletedFalse(userId: Long, tableDateId: Long): Boolean
    fun existsByIdIsNotAndUserIdAndTableDateIdAndDeletedFalse(trackerId: Long, userId: Long, tableDateId: Long): Boolean

    fun countByTableDateIdAndDeletedFalse(tableDateId: Long): Int

    fun findAllByTableDateIdAndDeletedFalse(tableDateId: Long?, pageable: Pageable): Page<UserAbsenceTracker>
}