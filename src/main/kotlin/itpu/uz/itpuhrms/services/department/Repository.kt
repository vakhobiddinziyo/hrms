package itpu.uz.itpuhrms.services.department


import itpu.uz.itpuhrms.Department
import itpu.uz.itpuhrms.DepartmentType
import itpu.uz.itpuhrms.base.BaseRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface DepartmentRepository : BaseRepository<Department> {
    fun findAllByOrganizationIdAndDeletedFalseOrderByIdDesc(organizationId: Long, pageable: Pageable): Page<Department>
    fun findByOrganizationIdAndDepartmentTypeAndDeletedFalse(orgId: Long, departmentType: DepartmentType): Department?
    fun findAllByOrganizationIdAndDepartmentTypeAndDeletedFalse(
        orgId: Long,
        departmentType: DepartmentType
    ): List<Department>

    fun findByIdAndOrganizationIdAndDeletedFalse(id: Long, organizationId: Long): Department?

    fun existsByIdAndOrganizationIdAndDeletedFalse(departmentId: Long, organizationId: Long): Boolean
    fun existsByParentDepartmentIdAndDeletedFalse(parentDepartmentId: Long): Boolean

    @Query(
        """
        select d 
        from Department d
        where d.deleted = false
          and d.organization.id = :orgId
          and (:parentId is null and d.parentDepartment is null or d.parentDepartment.id = :parentId)
    """
    )
    fun findOrganizationDepartments(orgId: Long, parentId: Long?): List<Department>
}