package itpu.uz.itpuhrms.services.employmentHistory


import itpu.uz.itpuhrms.UserEmploymentHistory
import itpu.uz.itpuhrms.base.BaseRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface UserEmploymentHistoryRepository : BaseRepository<UserEmploymentHistory> {
    @Query(
        """
        select h  from UserEmploymentHistory h
            where h.user.id = :userId
            order by h.department.id,h.id
    """
    )
    fun findAllByUserIdAndDeletedFalse(userId: Long, pageable: Pageable): Page<UserEmploymentHistory>

    fun findTopByUserIdAndDepartmentOrganizationIdAndDeletedFalseOrderByIdDesc(
        userId: Long,
        organizationId: Long
    ): UserEmploymentHistory?

    fun findTopByUserIdAndDepartmentOrganizationIdAndDeletedFalseOrderByIdAsc(
        userId: Long,
        organizationId: Long
    ): UserEmploymentHistory?
}