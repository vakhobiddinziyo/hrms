package itpu.uz.itpuhrms.services.workingDate

import itpu.uz.itpuhrms.DayOfWeek
import itpu.uz.itpuhrms.WorkingDateConfig
import itpu.uz.itpuhrms.base.BaseRepository
import org.springframework.stereotype.Repository


@Repository
interface WorkingDateConfigRepository : BaseRepository<WorkingDateConfig> {
    fun existsByOrganizationIdAndDayAndDeletedFalse(organizationId: Long, day: DayOfWeek): Boolean
    fun findByOrganizationIdAndDayAndDeletedFalse(organizationId: Long, day: DayOfWeek): WorkingDateConfig?
    fun findAllByOrganizationIdAndDeletedFalse(organizationId: Long): List<WorkingDateConfig>
}
