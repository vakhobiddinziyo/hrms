package itpu.uz.itpuhrms.services.stateTemplate

import itpu.uz.itpuhrms.StateTemplate
import itpu.uz.itpuhrms.Status
import itpu.uz.itpuhrms.base.BaseRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface StateTemplateRepository : BaseRepository<StateTemplate> {
    @Query("select s from StateTemplate s where s.status = :status and (s.name = 'OPEN' or s.name = 'CLOSED')")
    fun findDefaultTemplatesByStatus(@Param("status") status: Status): List<StateTemplate>
}