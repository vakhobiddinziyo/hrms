package itpu.uz.itpuhrms.services.projectEmployee

import itpu.uz.itpuhrms.Employee
import itpu.uz.itpuhrms.ProjectEmployee
import itpu.uz.itpuhrms.base.BaseRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ProjectEmployeeRepository : BaseRepository<ProjectEmployee> {
    @Query(
        """
        select pe.*
        from project_employee pe
                 join employee e on pe.employee_id = e.id
                 join users u on e.user_id = u.id
        where (:projectId is null or pe.project_id = :projectId)
          and pe.deleted = false
          and (:search is null or u.full_name ilike concat('%', :search, '%'))
          and (:status is null or e.status = :status)
        order by (e.id = :employeeId) desc, u.full_name
    """, nativeQuery = true,
        countQuery = """
            select count(pe.id)
            from project_employee pe
                     join employee e on pe.employee_id = e.id
                     join users u on e.user_id = u.id
            where (:projectId is null or pe.project_id = :projectId)
              and pe.deleted = false
              and (:search is null or u.full_name ilike concat('%', :search, '%'))
        """
    )
    fun findAllProjectId(
        @Param("projectId") projectId: Long?,
        employeeId: Long,
        search: String?,
        status: String?,
        pageable: Pageable
    ): Page<ProjectEmployee>

    fun findByProjectIdAndEmployeeIdAndDeletedFalse(projectId: Long, employeeId: Long): ProjectEmployee?
    fun existsByProjectIdAndEmployeeIdAndDeletedFalse(projectId: Long, employeeId: Long): Boolean

    @Query(
        """
        select count(e.id) = :employeeCount
        from project_employee pe
                 join employee e on pe.employee_id = e.id
        where pe.project_id = :projectId
          and e.deleted = false
          and e.ph_status = 'BUSY'
          and pe.deleted = false
          and pe.id in (:employeeIds)
    """, nativeQuery = true
    )
    fun existsByProjectAndEmployeeIds(projectId: Long, employeeIds: List<Long>, employeeCount: Long): Boolean
    fun existsByProjectIdAndDeletedFalse(projectId: Long): Boolean
    fun existsByEmployeeIdAndDeletedFalse(employeeId: Long): Boolean
    fun findByEmployee(employee: Employee): List<ProjectEmployee>
}