package itpu.uz.itpuhrms.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import itpu.uz.itpuhrms.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

interface StateService {
    fun create(request: StateRequest): StateResponse
    fun getOneById(id: Long): StateResponse
    fun getAll(request: StateSearch, pageable: Pageable): Page<StateResponse>
    fun edit(id: Long, request: StateUpdateRequest): StateResponse
    fun delete(id: Long)
}

@Service
class StateServiceImpl(
    private val repository: StateRepository,
    private val boardRepository: BoardRepository,
    private val extraService: itpu.uz.itpuhrms.services.ExtraService,
    private val validationService: itpu.uz.itpuhrms.services.ValidationService,
    private val jdbcTemplate: JdbcTemplate,
    private val objectMapper: ObjectMapper,
    private val projectEmployeeRepository: ProjectEmployeeRepository,
    private val taskRepository: TaskRepository,
) : itpu.uz.itpuhrms.services.StateService {

    @Transactional
    override fun create(request: StateRequest): StateResponse {
        return request.run {
            val employee = extraService.getEmployeeFromCurrentUser()
            val board = boardRepository.findByIdAndDeletedFalse(boardId)
                ?: throw BoardNotFoundException()
            validationService.validateDifferentOwners(
                projectEmployee(board.project, employee)
            )

            val closed = repository.findTopByBoardIdAndDeletedFalseOrderByOrderDesc(boardId)
                ?: throw StateNotFoundException()
            val order = closed.order

            repository.save(closed.apply { this.order = (order + 1).toShort() })

            StateResponse.toDto(repository.save(State(name, order, board)))
        }
    }

    override fun getOneById(id: Long) =
        repository.findByIdAndDeletedFalse(id)?.let {
            validationService.validateProjectEmployee(
                it.board.project,
                extraService.getEmployeeFromCurrentUser()
            )
            StateResponse.toDto(it)
        } ?: throw StateNotFoundException()

    override fun getAll(request: StateSearch, pageable: Pageable): Page<StateResponse> {

        return request.run {
            val board = boardRepository.findByIdAndDeletedFalse(boardId)
                ?: throw BoardNotFoundException()
            val searchQuery = search?.let { "'$search'" }
            val employee = extraService.getEmployeeFromCurrentUser()
            val projectEmployee = projectEmployee(board.project, employee)
            val employees = employees(projectEmployee, employeeIds, meMode)

            validationService.validateProjectEmployees(board.project, employeeIds, meMode)
            val countQuery = """
            select count(s.id)
            from state s
            where s.board_id = $boardId
              and s.deleted = false
              and ($searchQuery is null or s.name ilike concat('%', $searchQuery, '%'))
        """.trimIndent()

            val count = jdbcTemplate.queryForObject(countQuery, Long::class.java)!!

            val query = """
            with agg_task as (select t.id                                                    as id,
                                     t.title                                                 as title,
                                     t.priority                                              as priority,
                                     t.description                                           as description,
                                     t.parent_task_id                                        as parent_task_id,
                                     t.ordered                                               as ordered,
                                     cast(extract(epoch from t.start_date at time zone 'Asia/Tashkent') * 1000 as bigint) as startDate,
                                     cast(extract(epoch from t.end_date at time zone 'Asia/Tashkent') * 1000 as bigint)  as endDate,
                                     coalesce(t.time_estimate_amount, 0)                     as time_estimate_amount,
                                     t.state_id                                              as state_id,
                                     owner.id                                                as owner_id,
                                     owner.full_name                                         as owner_name,
                                     jsonb_agg(jsonb_build_object(
                                             'id', e.id,
                                             'projectEmployeeId', pe.id,
                                             'fullName', u.full_name,
                                             'imageHashId', fa.hash_id
                                               )) filter (where e.id is not null)            as employees
                              from task t
                                       join users owner on t.owner_id = owner.id
                                       left join task_project_employee tpe on t.id = tpe.task_id
                                       left join project_employee pe on tpe.employees_id = pe.id
                                       left join employee e on pe.employee_id = e.id and e.ph_status = 'BUSY'
                                       left join users u on e.user_id = u.id
                                       left join file_asset fa on e.image_asset_id = fa.id and fa.deleted = false
                              where t.deleted = false
                                and ((${employees?.joinToString()}) is null
                                  or pe.id in (${employees?.joinToString()}))
                              group by t.id, owner.id)
            
            select s.id                                       as id,
                   s.name                                     as name,
                   s.ordered                                  as ordered,
                   s.board_id                                 as board_id,
                   jsonb_agg(
                   jsonb_build_object(
                           'id', t.id,
                           'title', t.title,
                           'priority', t.priority,
                           'description', t.description,
                           'parentTaskId', t.parent_task_id,
                           'order', t.ordered,
                           'startDate', t.startDate,
                           'endDate', t.endDate,
                           'timeEstimateAmount', t.time_estimate_amount,
                           'employees', t.employees,
                           'subTasks', st.subtasks,
                           'ownerName', t.owner_name,
                           'ownerId', t.owner_id
                   ) order by t.ordered
                            ) filter (where t.id is not null) as tasks
            from state s
                     left join agg_task t on s.id = t.state_id
                     left join (select t.parent_task_id,
                                       jsonb_agg(
                                               jsonb_build_object(
                                                       'id', t.id,
                                                       'title', t.title,
                                                       'priority', t.priority,
                                                       'description', t.description,
                                                       'parentTaskId', t.parent_task_id,
                                                       'order', t.ordered,
                                                       'startDate', t.startDate,
                                                       'endDate', t.endDate,
                                                       'timeEstimateAmount', t.time_estimate_amount,
                                                       'state_id', t.state_id,
                                                       'employees', t.employees,
                                                       'ownerName', t.owner_name,
                                                       'ownerId', t.owner_id)) as subtasks
                                from agg_task t
                                where t.parent_task_id is not null
                                group by t.parent_task_id) st on st.parent_task_id = t.id
            where s.board_id = $boardId
              and s.deleted = false
              and t.parent_task_id is null
              and (${searchQuery} is null or s.name ilike concat('%', ${searchQuery}, '%'))
            group by s.id
            order by s.ordered
            limit ${pageable.pageSize} offset ${pageable.offset}
        """.trimIndent()

            val content = jdbcTemplate.query(query) { rs, _ ->
                val tasksJson = rs.getString("tasks")
                StateResponse(
                    rs.getLong("id"),
                    rs.getString("name"),
                    rs.getShort("ordered"),
                    rs.getLong("board_id"),
                    tasksJson?.let {
                        objectMapper.readValue<MutableList<TaskResponse>>(it)
                    } ?: mutableListOf()
                )
            }
            PageImpl(content, pageable, count)
        }
    }

    @Transactional
    override fun edit(id: Long, request: StateUpdateRequest) = repository.findByIdAndDeletedFalse(id)?.let { state ->
        if (state.immutable) throw AccessDeniedException("Access denied")
        validationService.validateDifferentOwners(
            projectEmployee(state.board.project, extraService.getEmployeeFromCurrentUser())
        )

        state.apply {
            request.name?.let { this.name = it }
            request.order?.let {
                this.order = updateStateOrder(
                    state, it,
                    repository.findAllByBoardIdAndDeletedFalseOrderByOrder(board.id!!)
                )
            }
        }
        repository.save(state)
        StateResponse.toDto(state)
    } ?: throw StateNotFoundException()

    @Transactional
    override fun delete(id: Long) {
        repository.findByIdAndDeletedFalse(id)?.let {
            val taskExists = taskRepository.existsByStateIdAndDeletedFalse(it.id!!)
            if (it.immutable || taskExists) throw AccessDeniedException("Access denied")

            validationService.validateDifferentOwners(
                projectEmployee(it.board.project, extraService.getEmployeeFromCurrentUser())
            )
            shiftOldStates(
                it.order,
                repository.findAllByBoardIdAndDeletedFalseOrderByOrder(
                    it.board.id!!
                )
            )
            repository.trash(id)
        } ?: throw StateNotFoundException()
    }

    private fun projectEmployee(project: Project, employee: Employee): ProjectEmployee {
        return projectEmployeeRepository.findByProjectIdAndEmployeeIdAndDeletedFalse(
            project.id!!,
            employee.id!!
        ) ?: throw EmployeeNotFoundException()
    }

    private fun employees(
        projectEmployee: ProjectEmployee,
        employeeIds: List<Long>?,
        meMode: Boolean
    ): List<Long>? {
        return when {
            meMode -> return listOf(projectEmployee.id!!)
            else -> employeeIds
        }
    }

    private fun updateStateOrder(state: State, order: Short, states: MutableList<State>): Short {
        val currentOrder = state.order
        if (currentOrder == order) return order

        validationService.validateStateOrder(
            state.board,
            order
        )

        if (order < currentOrder) {
            states.filter { it.order in order until currentOrder }
                .forEach { it.order = (it.order + 1).toShort() }
        } else {
            states.filter { it.order in (currentOrder + 1)..order }
                .forEach { it.order = (it.order - 1).toShort() }
        }
        repository.saveAll(states)
        return order
    }

    private fun shiftOldStates(order: Short, states: MutableList<State>) {
        for (state in states) {
            if (state.order > order) {
                state.order = (state.order - 1).toShort()
            }
        }
        repository.saveAll(states)
    }

}
