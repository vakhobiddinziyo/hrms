package itpu.uz.itpuhrms.services.project

import itpu.uz.itpuhrms.Project
import itpu.uz.itpuhrms.base.BaseRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ProjectRepository : BaseRepository<Project> {
    @Query(
        """select p from Project p 
                 where (:departmentId is null  or p.department.id = :departmentId) 
                 and p.deleted = false"""
    )
    fun findAllByDepartmentIdAndDeletedFalse(@Param("departmentId") departmentId: Long?): List<Project>

    @Query(
        """select p from Project p 
                 where (:departmentId is null  or p.department.id = :departmentId) 
                 and p.deleted = false"""
    )
    fun findAllByDepartmentIdAndDeletedFalse(
        @Param("departmentId") departmentId: Long?,
        pageable: Pageable
    ): Page<Project>

    fun findByIdAndDepartmentOrganizationIdAndDeletedFalse(id: Long, organizationId: Long): Project?
}
