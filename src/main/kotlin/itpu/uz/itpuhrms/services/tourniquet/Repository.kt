package itpu.uz.itpuhrms.services.tourniquet

import itpu.uz.itpuhrms.Tourniquet
import itpu.uz.itpuhrms.base.BaseRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface TourniquetRepository : BaseRepository<Tourniquet> {
    fun findAllByOrganizationIdAndDeletedFalse(organizationId: Long, pageable: Pageable): Page<Tourniquet>
    fun existsByName(name: String): Boolean
    fun findByNameAndDeletedFalse(name: String): Tourniquet?
    fun findAllByOrganizationIdAndDeletedFalse(organizationId: Long): MutableList<Tourniquet>

    @Query(
        """
        select t 
        from Tourniquet t 
        where t.deleted = false 
          and (:orgId is null or t.organization.id = :orgId)
    """
    )
    fun findTourniquetsByDeletedFalseAndFilter(orgId: Long?, pageable: Pageable): Page<Tourniquet>
}