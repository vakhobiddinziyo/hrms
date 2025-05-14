package itpu.uz.itpuhrms.services.project

import itpu.uz.itpuhrms.*
import itpu.uz.itpuhrms.base.BaseMessage
import itpu.uz.itpuhrms.services.ExtraService
import itpu.uz.itpuhrms.services.board.BoardRepository
import itpu.uz.itpuhrms.services.department.DepartmentRepository
import itpu.uz.itpuhrms.services.projectEmployee.ProjectEmployeeRepository
import itpu.uz.itpuhrms.services.validation.ValidationService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service


interface ProjectService {
    fun createProject(request: ProjectCreateRequest): ProjectResponse
    fun getAllProject(departmentId: Long?): List<ProjectResponse>
    fun getProjectById(id: Long): ProjectResponse
    fun getAllProjectForOrgAdmin(departmentId: Long?, status: ProjectStatus?, pageable: Pageable): Page<ProjectResponse>
    fun getProjectsPageForCurrentUser(
        search: String?,
        status: ProjectStatus?,
        pageable: Pageable
    ): Page<ProjectResponse>

    fun update(id: Long, request: ProjectUpdateRequest): ProjectResponse
    fun delete(id: Long): BaseMessage
}


@Service
class ProjectServiceImpl(
    private val repository: ProjectRepository,
    private val extraService: ExtraService,
    private val departmentRepository: DepartmentRepository,
    private val projectEmployeeRepository: ProjectEmployeeRepository,
    private val validationService: ValidationService,
    private val jdbcTemplate: JdbcTemplate,
    private val boardRepository: BoardRepository
) : ProjectService {

    override fun createProject(request: ProjectCreateRequest): ProjectResponse {
        val employee = extraService.getEmployeeFromCurrentUser()
        val project = repository.save(request.toEntity(employee))
        projectEmployeeRepository.save(
            ProjectEmployee(
                project,
                employee,
                ProjectEmployeeRole.OWNER
            )
        )
        val employeeAmount = projectEmployeeAmount(project.id!!)
        return project.let { ProjectResponse.toDto(it, employeeAmount) }
    }

    // for Current User
    override fun getAllProject(departmentId: Long?): List<ProjectResponse> {
        val organization = extraService.getOrgFromCurrentUser()

        departmentId?.run {
            departmentRepository.existsByIdAndOrganizationIdAndDeletedFalse(this, organization.id!!)
                .runIfFalse { throw DepartmentNotFoundException() }
        }
        return repository.findAllByDepartmentIdAndDeletedFalse(departmentId)
            .map { ProjectResponse.toDto(it, projectEmployeeAmount(it.id!!)) }
    }


    override fun getAllProjectForOrgAdmin(
        departmentId: Long?,
        status: ProjectStatus?,
        pageable: Pageable
    ): Page<ProjectResponse> {

        val organization = extraService.getOrgFromCurrentUser()
        val department = department(departmentId)
        val statusQuery = status?.let { "'$it'" }

        val countQuery = """
            select count(distinct p.id)
            from project p
            join department d on p.department_id = d.id
            join project_employee pe on p.id = pe.project_id
            join employee e on p.owner_id = e.id
            where p.deleted = false
              and pe.deleted = false
              and d.organization_id = ${organization.id}
              and (${department?.id} is null or d.id = ${department?.id})
              and (${statusQuery} is null or p.status = ${statusQuery})
            """.trimIndent()

        val projectAmount = jdbcTemplate.queryForObject(countQuery, Long::class.java)!!

        val query = """
            select p.id                      as id,
                   p.name                    as name,
                   p.description             as description,
                   d.name                    as department_name,
                   coalesce(u.full_name, '') as full_name,
                   p.owner_id                as owner_id,
                   count(pe.id)              as employee_amount,
                   p.status                  as status
            from project p
                     join department d on p.department_id = d.id
                     join project_employee pe on p.id = pe.project_id
                     join employee e on p.owner_id = e.id
                     left join users u on e.user_id = u.id
            where p.deleted = false
              and pe.deleted = false
              and d.organization_id = ${organization.id}
              and (${department?.id} is null or d.id = ${department?.id})
              and (${statusQuery} is null or p.status = ${statusQuery})
            group by p.id, d.id, u.id
            limit ${pageable.pageSize} offset ${pageable.offset}
        """.trimIndent()

        val result = jdbcTemplate.query(query) { rs, _ ->
            ProjectResponse(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("full_name"),
                rs.getString("department_name"),
                rs.getLong("employee_amount"),
                ProjectStatus.valueOf(rs.getString("status")),
                rs.getLong("owner_id"),
            )
        }
        return PageImpl(result, pageable, projectAmount)
    }

    override fun getProjectsPageForCurrentUser(
        search: String?,
        status: ProjectStatus?,
        pageable: Pageable
    ): Page<ProjectResponse> {
        val employee = extraService.getEmployeeFromCurrentUser()
        val statusQuery = status?.let { "'$it'" }
        val searchQuery = search?.let { "'$it'" }

        val countQuery = """
            select count(p.id)
            from project p
                     join department d on p.department_id = d.id
                     join project_employee pe on p.id = pe.project_id
            where p.deleted = false
              and (${searchQuery} is null or p.name ilike concat('%', ${searchQuery}, '%'))
              and (${statusQuery} is null or p.status = ${statusQuery})
              and pe.deleted = false
              and pe.employee_id = ${employee.id}
            """.trimIndent()

        val count = jdbcTemplate.queryForObject(countQuery, Long::class.java)!!

        val query = """
            with owner as (select u.full_name,
                                  e.id
                           from employee e
                                    left join users u on u.id = e.user_id and u.deleted = false)
            select p.id                         as id,
                   p.name                       as name,
                   p.description                as description,
                   d.name                       as department_name,
                   p.status                     as status,
                   o.full_name                  as full_name,
                   o.id                         as owner_id,
                   (select count(i_pe)
                    from project_employee i_pe
                    where i_pe.project_id = p.id
                      and i_pe.deleted = false) as employee_amount
            from project p
                     join owner o on o.id = p.owner_id
                     join department d on p.department_id = d.id
                     join project_employee pe on p.id = pe.project_id
            where p.deleted = false
              and pe.deleted = false
              and (${searchQuery} is null or p.name ilike concat('%', ${searchQuery}, '%'))
              and (${statusQuery} is null or p.status = ${statusQuery})
              and pe.employee_id = ${employee.id}
            limit ${pageable.pageSize} offset ${pageable.offset}
        """.trimIndent()

        val projects = projectResults(query)
        return PageImpl(projects, pageable, count)
    }

    override fun getProjectById(id: Long): ProjectResponse {
        repository.findByIdAndDeletedFalse(id)?.let {
            validationService.validateProjectEmployee(
                it,
                extraService.getEmployeeFromCurrentUser()
            )
            return ProjectResponse.toDto(it, projectEmployeeAmount(it.id!!))
        } ?: throw ProjectNotFoundException()
    }

    override fun update(id: Long, request: ProjectUpdateRequest): ProjectResponse {
        return repository.findByIdAndDeletedFalse(id)?.let {
            validationService.validateDifferentOwners(
                projectEmployee(extraService.getEmployeeFromCurrentUser(), it)
            )
            it.name = request.name!!
            it.description = request.description
            it.status = request.status
            ProjectResponse.toDto(repository.save(it), projectEmployeeAmount(it.id!!))
        } ?: throw ProjectNotFoundException()
    }

    override fun delete(id: Long): BaseMessage {
        repository.findByIdAndDeletedFalse(id)?.let {
            val boardExists = boardRepository.existsByProjectIdAndDeletedFalse(it.id!!)
            val employeeExists = projectEmployeeRepository.existsByProjectIdAndDeletedFalse(it.id!!)
            // if (boardExists || employeeExists) throw AccessDeniedException("Access denied")

            validationService.validateDeletingPermission(
                projectEmployee(extraService.getEmployeeFromCurrentUser(), it)
            )
            repository.trash(id)
        } ?: throw ProjectNotFoundException()
        return BaseMessage.OK
    }

    private fun department(departmentId: Long?): Department? {
        return departmentId?.let {
            val department = departmentRepository.findByIdAndDeletedFalse(departmentId)
                ?: throw DepartmentNotFoundException()
            validationService.validateDifferentOrganizations(
                extraService.getOrgFromCurrentUser(),
                department
            )
            department
        }
    }

    private fun projectResults(query: String): List<ProjectResponse> {
        return jdbcTemplate.query(query) { rs, _ ->
            ProjectResponse(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("full_name"),
                rs.getString("department_name"),
                rs.getLong("employee_amount"),
                ProjectStatus.valueOf(rs.getString("status")),
                rs.getLong("owner_id"),
            )
        }
    }

    private fun projectEmployeeAmount(projectId: Long): Long {
        val countQuery = """
            select count(pe.id)
            from project_employee pe
            where pe.project_id = $projectId
              and pe.deleted = false
        """.trimIndent()
        return jdbcTemplate.queryForObject(countQuery, Long::class.java)!!
    }

    private fun projectEmployee(employee: Employee, project: Project): ProjectEmployee {
        return projectEmployeeRepository.findByProjectIdAndEmployeeIdAndDeletedFalse(project.id!!, employee.id!!)
            ?: throw EmployeeNotFoundException()
    }
}