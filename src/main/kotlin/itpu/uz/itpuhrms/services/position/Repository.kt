package itpu.uz.itpuhrms.services.position

import itpu.uz.itpuhrms.Position
import itpu.uz.itpuhrms.base.BaseRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface PositionRepository : BaseRepository<Position> {

    @Query("""
        select p from Position p 
        where p.deleted = false 
        and (:organizationId is null or p.organization.id = :organizationId)
        order by p.id desc 
    """)
    fun findAllByOrganizationIdAndDeletedFalseOrderByIdDesc(organizationId: Long?, pageable: Pageable): Page<Position>
}