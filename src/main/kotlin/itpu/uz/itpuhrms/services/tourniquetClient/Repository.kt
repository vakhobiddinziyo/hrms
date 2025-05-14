package itpu.uz.itpuhrms.services.tourniquetClient

import itpu.uz.itpuhrms.TourniquetClient
import itpu.uz.itpuhrms.base.BaseRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface TourniquetClientRepository : BaseRepository<TourniquetClient> {
    fun existsByUsername(username: String): Boolean

    @Query(
        """
        select tc.*
        from tourniquet_client tc
        where tc.deleted = false
          and (:search is null or tc.username ilike concat('%', :search, '%'))
          and tc.organization_id = :orgId
    """, nativeQuery = true,
        countQuery = """
            select count(tc.id)
            from tourniquet_client tc
            where tc.deleted = false
              and (:search is null or tc.username ilike concat('%', :search, '%'))
              and tc.organization_id = :orgId
        """
    )
    fun findOrganizationTourniquetClients(search: String?, orgId: Long, pageable: Pageable): Page<TourniquetClient>
    fun findAllByOrganizationIdAndDeletedFalse(organizationId: Long, pageable: Pageable): Page<TourniquetClient>
    fun findByUsernameAndDeletedFalse(username: String): TourniquetClient?
}