package itpu.uz.itpuhrms.services.board


import itpu.uz.itpuhrms.Board
import itpu.uz.itpuhrms.base.BaseRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface BoardRepository : BaseRepository<Board> {
    fun findAllByDeletedFalseOrderByIdDesc(pageable: Pageable): Page<Board>

    @Query(
        value = """
        select *
        from board b
        where b.project_id = :projectId
          and b.deleted = false
          and (:search is null or b.name ilike concat('%', :search, '%'))
        """,
        countQuery = """
        select count(b.id)
        from board b
        where b.project_id = :projectId
          and b.deleted = false
          and (:search is null or b.name ilike concat('%', :search, '%'))
        """,
        nativeQuery = true
    )
    fun findAllByOrganizationIdAndDeletedFalse(
        projectId: Long,
        search: String?,
        pageable: Pageable
    ): Page<Board>

    fun existsByProjectIdAndDeletedFalse(projectId: Long): Boolean
}