package itpu.uz.itpuhrms.services.board

import itpu.uz.itpuhrms.*
import itpu.uz.itpuhrms.BoardNotFoundException
import itpu.uz.itpuhrms.EmployeeNotFoundException
import itpu.uz.itpuhrms.ProjectNotFoundException
import itpu.uz.itpuhrms.services.ExtraService
import itpu.uz.itpuhrms.services.project.ProjectRepository
import itpu.uz.itpuhrms.services.projectEmployee.ProjectEmployeeRepository
import itpu.uz.itpuhrms.services.stateTemplate.StateTemplateService
import itpu.uz.itpuhrms.services.validation.ValidationService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

interface BoardService {
    fun create(request: BoardRequest): BoardResponse
    fun getOneById(id: Long): BoardResponse
    fun getAll(projectId: Long, search: String?, status: BoardStatus?, pageable: Pageable): Page<BoardAdminResponse>
    fun getAllForOrgAdmin(projectId: Long, search: String?, status: BoardStatus?, pageable: Pageable): Page<BoardAdminResponse>
    fun edit(id: Long, request: BoardUpdateRequest): BoardResponse
    fun delete(id: Long)
}


@Service
class BoardServiceImpl(
    private val repository: BoardRepository,
    private val projectRepository: ProjectRepository,
    private val extraService: ExtraService,
    private val validationService: ValidationService,
    private val jdbcTemplate: JdbcTemplate,
    private val stateTemplateService: StateTemplateService,
    private val projectEmployeeRepository: ProjectEmployeeRepository,
) : BoardService {

    @Transactional
    override fun create(request: BoardRequest): BoardResponse {
        val owner = extraService.getEmployeeFromCurrentUser()
        val project = projectRepository.findByIdAndDeletedFalse(request.projectId)
            ?: throw ProjectNotFoundException()
        validationService.validateDifferentOwners(
            projectEmployee(owner, project)
        )

        val save = repository.save(
            Board(
                request.name,
                project,
                owner,
                BoardStatus.ACTIVE
            )
        )
        stateTemplateService.saveDefaultStates(save)
        return BoardResponse.toDto(save)
    }

    override fun getOneById(id: Long): BoardResponse {
        repository.findByIdAndDeletedFalse(id)?.let {
            validationService.validateProjectEmployee(
                it.project,
                extraService.getEmployeeFromCurrentUser()
            )
            return BoardResponse.toDto(it)
        } ?: throw BoardNotFoundException()
    }

    @Transactional
    override fun edit(id: Long, request: BoardUpdateRequest): BoardResponse {
        return repository.findByIdAndDeletedFalse(id)?.let {
            validationService.validateDifferentOwners(
                projectEmployee(extraService.getEmployeeFromCurrentUser(), it.project)
            )
            it.name = request.name
            it.status = request.status
            repository.save(it)
            BoardResponse.toDto(it)
        } ?: throw BoardNotFoundException()
    }

    override fun getAll(
        projectId: Long,
        search: String?,
        status: BoardStatus?,
        pageable: Pageable
    ): Page<BoardAdminResponse> {
        val project = projectRepository.findByIdAndDeletedFalse(projectId)
            ?: throw ProjectNotFoundException()
        val employee = extraService.getEmployeeFromCurrentUser()
        validationService.validateProjectEmployee(
            project,
            employee,
        )
        val searchQuery = search?.let { "'$it'" }
        val statusQuery = status?.let { "'$it'" }

        val countQuery = """
            select count(b.id)
            from board b
            where b.project_id = $projectId
              and b.deleted = false
              and (${searchQuery} is null or b.name ilike concat('%', ${searchQuery}, '%'))
              and (${statusQuery} is null or b.status = $statusQuery)
        """.trimIndent()

        val count = jdbcTemplate.queryForObject(countQuery, Long::class.java)!!

        val query = """
            select b.id                      as id,
       b.name                    as name,
       b.project_id              as project_id,
       b.status                  as status,
       b.owner_id                as owner_id,
       coalesce(u.full_name, '') as full_name,
       count(t.id)               as task_amount,
       ta.project_open_task_count as project_open_task_count,
       ta.employee_open_task_count as employee_open_task_count
from board b
         join employee e on b.owner_id = e.id
         left join users u on e.user_id = u.id
         left join task t on b.id = t.board_id and t.deleted = false
         left join (select
                        b.id as board_id,
                        count(distinct case when s.name = 'OPEN' then t.id end ) as project_open_task_count,
                        count(distinct case when s.name = 'OPEN' and pe.employee_id = ${employee.id} then t.id end) as employee_open_task_count
                    from board b
                             left join task t on b.id = t.board_id
                             left join state s on t.state_id = s.id
                             left join task_project_employee tpe on t.id = tpe.task_id
                             left join project_employee pe on tpe.employees_id = pe.id
                    where b.project_id = ${projectId}
                      and b.deleted = false
                      and pe.deleted = false
                    group by b.id) as ta on ta.board_id = b.id
where b.project_id = ${projectId}
  and b.deleted = false
  and (${searchQuery} is null or b.name ilike concat('%', ${searchQuery}, '%'))
  and (${statusQuery} is null or b.status = ${statusQuery})
group by b.id, u.id, ta.project_open_task_count, ta.employee_open_task_count
            limit ${pageable.pageSize} offset ${pageable.offset}
        """.trimIndent()

        val content = jdbcTemplate.query(query) { rs, _ ->
            BoardAdminResponse(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getLong("project_id"),
                ProjectStatus.valueOf(rs.getString("status")),
                rs.getLong("owner_id"),
                rs.getString("full_name"),
                rs.getLong("task_amount"),
                rs.getLong("project_open_task_count"),
                rs.getLong("employee_open_task_count"),
            )
        }
        return PageImpl(content, pageable, count)
    }

    override fun getAllForOrgAdmin(
        projectId: Long,
        search: String?,
        status: BoardStatus?,
        pageable: Pageable
    ): Page<BoardAdminResponse> {

        val organizationId = extraService.getOrgFromCurrentUser().id
        val searchQuery = search?.let { "'${search}'" }
        val statusQuery = status?.let { "'${status}'" }

        val project = projectRepository.findByIdAndDeletedFalse(projectId) ?: throw ProjectNotFoundException()
        if (project.department.organization.id != organizationId) throw ProjectNotFoundException()

        val query = """
            select b.id                      as id,
                   b.name                    as name,
                   b.project_id              as project_id,
                   b.status                  as status,
                   b.owner_id                as owner_id,
                   coalesce(u.full_name, '') as full_name,
                   count(t.id)               as task_amount,
                   ta.project_open_task_count as project_open_task_count
            from board b
            join employee e on b.owner_id = e.id
            left join users u on e.user_id = u.id
            left join user_org_store uos on u.id = uos.user_id
            left join task t on b.id = t.board_id and t.deleted = false
            left join (select b.id as board_id,
                              count(distinct case when s.name = 'OPEN' then t.id end ) as project_open_task_count
                       from board b
                       left join task t on b.id = t.board_id
                       left join state s on t.state_id = s.id
                       where b.project_id = ${projectId}
                       and b.deleted = false
                       group by b.id) as ta on ta.board_id = b.id
            where b.project_id = ${projectId}
            and uos.organization_id = ${organizationId}
            and b.deleted = false
            and (${searchQuery} is null or b.name ilike concat('%', ${searchQuery}, '%'))
            and (${statusQuery} is null or b.status = ${statusQuery})
            group by b.id, u.id, ta.project_open_task_count
            limit ${pageable.pageSize} offset ${pageable.offset}
        """.trimIndent()

        val result = jdbcTemplate.query(query) { rs, _ ->
            BoardAdminResponse(
                id = rs.getLong("id"),
                name = rs.getString("name"),
                projectId = rs.getLong("project_id"),
                status = ProjectStatus.valueOf(rs.getString("status")),
                ownerId = rs.getLong("owner_id"),
                ownerName = rs.getString("full_name"),
                taskAmount = rs.getLong("task_amount"),
                projectOpenTaskAmount = rs.getLong("project_open_task_count"),
            )
        }

        val countQuery = """
            select count(*)
            from board b
            join project p on b.project_id = p.id
            join employee e on b.owner_id = e.id
            join users u on e.user_id = u.id
            join user_org_store uos on u.id = uos.user_id
            where b.deleted = false
            and p.id = ${projectId}
            and uos.organization_id = ${organizationId}
            and (${searchQuery} is null or b.name ilike concat('%', ${searchQuery}, '%'))
            and (${statusQuery} is null or b.status = ${statusQuery})
        """.trimIndent()
        val totalElements = jdbcTemplate.queryForObject(countQuery, Integer::class.java)!!.toLong()

        return PageImpl(result, pageable, totalElements)
    }

    @Transactional
    override fun delete(id: Long) {
        repository.findByIdAndDeletedFalse(id)?.let {
            validationService.validateDeletingPermission(
                projectEmployee(extraService.getEmployeeFromCurrentUser(), it.project)
            )
            repository.trash(id)
        } ?: throw BoardNotFoundException()
    }

    private fun projectEmployee(employee: Employee, project: Project): ProjectEmployee {
        return projectEmployeeRepository.findByProjectIdAndEmployeeIdAndDeletedFalse(project.id!!, employee.id!!)
            ?: throw EmployeeNotFoundException()
    }

}