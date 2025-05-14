package itpu.uz.itpuhrms.services.task

import com.fasterxml.jackson.databind.ObjectMapper
import itpu.uz.itpuhrms.*
import itpu.uz.itpuhrms.base.BaseMessage
import itpu.uz.itpuhrms.services.ExtraService
import itpu.uz.itpuhrms.services.board.BoardRepository
import itpu.uz.itpuhrms.services.employee.EmployeeRepository
import itpu.uz.itpuhrms.services.file.FileAssetRepository
import itpu.uz.itpuhrms.services.taskActionHistory.TaskActionHistoryService
import itpu.uz.itpuhrms.services.validation.ValidationService
import itpu.uz.itpuhrms.services.notification.BOT_USERNAME
import itpu.uz.itpuhrms.services.projectEmployee.ProjectEmployeeRepository
import itpu.uz.itpuhrms.services.state.StateRepository
import itpu.uz.itpuhrms.services.subscriber.SubscriberRepository
import itpu.uz.itpuhrms.services.taskActionHistory.TaskActionHistoryRequest
import itpu.uz.itpuhrms.services.userAbsenceTracker.FileDataResponse
import itpu.uz.itpuhrms.services.workingDate.WorkingDateConfigRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.File
import java.util.*

interface TaskService {
    fun create(request: TaskCreateRequest): TaskResponse
    fun getOneById(id: Long): TaskResponse
    fun getAll(parentId: Long?, boardId: Long, pageable: Pageable): Page<TaskResponse>
    fun getAllForOrgAdmin(boardId: Long, stateName: String?, priority: TaskPriority?, pageable: Pageable): Page<TaskResponse>
    fun searchById(
        taskId: Long?,
        boardId: Long,
        startDate: Long?,
        endDate: Long?,
        priority: TaskPriority?,
        pageable: Pageable): Page<TaskResponse>
//    fun getAll(stateName: String?, pageable: Pageable): Page<TaskStateResponse>
    fun edit(id: Long, request: TaskUpdateRequest): TaskResponse
    fun changeState(id: Long, stateId: Long, order: Short): BaseMessage
    fun moveTaskToBoard(taskId: Long, newBoardId: Long): TaskResponse
    fun getTasksByType(): TypedTaskResponse
    fun delete(request: TaskDeleteRequest)
}

@Service
class TaskServiceImpl(
    private val repository: TaskRepository,
    private val boardRepository: BoardRepository,
    private val stateRepository: StateRepository,
    private val projectEmployeeRepository: ProjectEmployeeRepository,
    private val fileAssetRepository: FileAssetRepository,
    private val extraService: ExtraService,
    private val validationService: ValidationService,
    private val actionHistoryService: TaskActionHistoryService,
    private val workingDateConfigRepository: WorkingDateConfigRepository,
    private val subscriberRepository: SubscriberRepository,
    private val taskSubscriberRepository: TaskSubscriberRepository,
    private val employeeRepository: EmployeeRepository,
    private val jdbcTemplate: JdbcTemplate,
    private val objectMapper: ObjectMapper
) : TaskService {

    @Transactional
    override fun create(request: TaskCreateRequest): TaskResponse {
        val owner = extraService.getEmployeeFromCurrentUser()
        val state = stateRepository.findByIdAndDeletedFalse(request.stateId)
            ?: throw StateNotFoundException()
        val parentTask = parentTask(request.parentTaskId, state)
        val employees = taskEmployees(request.employeeIds, state)
        val files = files(request.filesHashIds)
        val config = config(owner.organization)

        validationService.validateProjectEmployee(state.board.project, owner)
        validationService.validateTimeEstimateAmount(request.timeEstimateAmount, config)

        val task = repository.save(
            Task(
                request.title,
                employees,
                parentTask?.state ?: state,
                state.board,
                files,
                parentTask?.let { taskOrder(it) } ?: taskOrder(state),
                request.priority,
                owner.user!!,
                request.description,
                request.startDate?.let { Date(it) },
                request.endDate?.let { Date(it) },
                request.timeEstimateAmount,
                parentTask,
            )
        )
        createTaskEmployeesHistory(task, owner.user!!)
        createTaskSubscriber(task, owner)
        createTaskHistory(task, owner.user!!)
//        createTaskPriorityHistory(task, owner.user!!)
//        createTaskStartDateHistory(task, owner.user!!)
//        createTaskDueDateHistory(task, owner.user!!)
//        createTaskEstimateTimeHistory(task, owner.user!!)
//        createFilesHistory(task, owner.user!!)
        val ownerPhotoHashId = owner.imageAsset?.hashId
        return TaskResponse.toDto(task, ownerPhotoHashId = ownerPhotoHashId)
    }

    private fun getOwnerPhotoHashId(ownerId: Long, orgId: Long): String? {
        return employeeRepository.findOwnerPhotoHashIdByUserIdAndOrgId(ownerId, orgId)
    }

    override fun getOneById(id: Long): TaskResponse {
        val currentEmp = extraService.getEmployeeFromCurrentUser()
        val task = repository.findByIdAndDeletedFalse(id) ?: throw TaskNotFoundException()
        validationService.validateProjectEmployee(task.board.project, currentEmp)
        val ownerPhotoHashId = getOwnerPhotoHashId(task.owner.id!!, currentEmp.organization.id!!)

        return TaskResponse.toDto(task, subtasks = subtasks(task).map { TaskResponse.toDto(it) }, fileResponse = task.files.map { FileDataResponse.toResponse(it) }, ownerPhotoHashId = ownerPhotoHashId)
    }

    override fun getAll(parentId: Long?, boardId: Long, pageable: Pageable): Page<TaskResponse> {
        val currentEmp = extraService.getEmployeeFromCurrentUser()
        val board = boardRepository.findByIdAndDeletedFalse(boardId) ?: throw BoardNotFoundException()

        validationService.validateProjectEmployee(board.project, currentEmp)
        parentId?.let {
            val parent = repository.findByIdAndDeletedFalse(it) ?: throw TaskNotFoundException()
            validationService.validateBoardTask(
                parent,
                board
            )
        }
        return repository.findAllByParentId(parentId, boardId, pageable)
            .map {
                val ownerPhotoHashId =
                    getOwnerPhotoHashId(it.owner.id!!, currentEmp.organization.id!!)  // Employee ID ishlatilmoqda
                TaskResponse.toDto(it, ownerPhotoHashId = ownerPhotoHashId)
            }
    }

    override fun getAllForOrgAdmin(boardId: Long, stateName: String?, priority: TaskPriority?, pageable: Pageable): Page<TaskResponse> {
        val board = boardRepository.findByIdAndDeletedFalse(boardId) ?: throw BoardNotFoundException()
        val organizationId = extraService.getOrgFromCurrentUser().id!!
        if (board.project.department.organization.id != organizationId) throw BoardNotFoundException()



        val result = repository.findAllForOrgAdmin(boardId, organizationId, stateName, priority, pageable)

        return result.map {
            val ownerPhotoHashId = it.owner.avatarPhoto?.hashId
            val dataFileResponse = it.files.map { files -> FileDataResponse.toResponse(files) }
            val subtasks = subtasks(it).map {tasks -> TaskResponse.toDto(tasks)}
            TaskResponse.toDto(it, dataFileResponse, subtasks, ownerPhotoHashId)
        }
    }

    override fun searchById(
        taskId: Long?,
        boardId: Long,
        startDate: Long?,
        endDate: Long?,
        priority: TaskPriority?,
        pageable: Pageable
    ): Page<TaskResponse> {
        val organizationId = extraService.getOrgFromCurrentUser().id
        val employeeId = extraService.getEmployeeFromCurrentUser().id
        boardId.let {
            boardRepository.findByIdAndDeletedFalse(it) ?: throw BoardNotFoundException()
        }
        val priorityQuery = priority?.let {"'$priority'"}
        val startLocalDate = startDate?.let {"'${startDate.localDateWithUTC()}'"}
        val endLocalDate = endDate?.let {"'${endDate.localDateWithUTC()}'"}
        val query = """
        with recursive recursive_task as (
            select *
            from task t1
            where (${taskId} is null) or t1.id = ${taskId}
            union all
            select t2.*
            from task t2
            join recursive_task rt  on t2.parent_task_id = rt.id),
        
           task_files as (
                select
                    tfa.task_id,
                    coalesce(
                    jsonb_agg(
                    jsonb_build_object(
                       'hashId', fa.hash_id,
                       'fileName', fa.file_name,
                       'fileContentType', fa.file_content_type,
                       'fileSize', fa.file_size )) filter (where fa.hash_id is not null), '[]') as files
                from task t
                left join task_file_asset tfa on t.id = tfa.task_id
                left join file_asset fa on tfa.files_id = fa.id and fa.deleted = false
                group by tfa.task_id),
        
           task_employees as (
                 select
                     tpe.task_id,
                     coalesce(
                       jsonb_agg(
                       jsonb_build_object(
                          'id', e.id,
                          'fullName', u.full_name,
                          'imageHashId', ef.hash_id,
                          'projectEmployeeId', pe.id)), '[]') as employees
                 from task t
                 left join task_project_employee tpe on t.id = tpe.task_id
                 left join project_employee pe on tpe.employees_id = pe.id
                 left join employee e on pe.employee_id = e.id and e.status = 'ACTIVE'
                 left join users u on e.user_id = u.id and u.deleted = false
                 left join file_asset ef on e.image_asset_id = ef.id
                 group by tpe.task_id),
           subtasks as(
                 select
                     parent_task_id,
                     coalesce(
                       jsonb_agg(distinct
                       jsonb_build_object(
                          'id', t.id,
                          'title', t.title,
                          'priority', t.priority,
                          'order', t.ordered,
                          'stateId', s.id,
                          'stateOrder', s.ordered,
                          'boardId', b.id,
                          'boardName', b.name,
                          'files', tf.files,
                          'ownerName', o.full_name,
                          'ownerId', o.id,
                          'ownerPhotoHashId', oi.hash_id,
                          'startDate', t.start_date,
                          'endDate', t.end_date,
                          'description', t.description,
                          'parentTaskId', t.parent_task_id,
                          'employees', te.employees,
                          'timeEstimateAmount', t.time_estimate_amount)) filter (where t.id is not null), '[]') as sub_tasks
                 from recursive_task t
                 left join board b on t.board_id = b.id
                 left join state s on t.state_id = s.id
                 left join users o on t.owner_id = o.id
                 left join file_asset oi on o.avatar_photo_id = oi.id
                 left join task_files tf on t.id = tf.task_id
                 left join task_employees te on t.id = te.task_id
                 where t.parent_task_id is not null
                 group by t.parent_task_id)
        
        select
              t.id                          as task_id,
              t.title                       as task_title,
              t.priority                    as priority,
              t.ordered                     as task_order,
              t.start_date                  as start_date,
              t.end_date                    as end_date,
              t.time_estimate_amount        as time_estimate_amount,
              t.description                 as task_description,
              t.parent_task_id              as parent_task_id,
              o.id                          as owner_id,
              o.full_name                   as owner_full_name,
              oi.hash_id                    as owner_image_hash_id,
              b.id                          as board_id,
              b.name                        as board_name,
              s.id                          as state_id,
              s.ordered                     as state_order,
              tf.files                      as files,
              te.employees                  as employees,
              st.sub_tasks                  as subtasks
        from task t
        join board b on t.board_id = b.id
        join state s on t.state_id = s.id
        join users o on t.owner_id = o.id
        left join file_asset oi on o.avatar_photo_id = oi.id
        left join task_files tf on t.id = tf.task_id
        left join task_employees te on t.id = te.task_id
        left join subtasks st on st.parent_task_id = t.id
        join project p on p.id = b.project_id
        join department d on p.department_id = d.id
        join organization org on d.organization_id = org.id
        join project_employee pe on p.id = pe.project_id
        where t.deleted = false
          and t.parent_task_id is null
          and ((${taskId} is null) or t.id = ${taskId})
          and ((${priorityQuery} is null) or  t.priority = ${priorityQuery})
          and (
             ${startLocalDate} is not null and ${endLocalDate} is not null and t.end_date between ${startLocalDate} and ${endLocalDate}
             or (${startLocalDate} is null or ${endLocalDate} is null)
          )
          and b.id = ${boardId}
          and org.id = ${organizationId}
          and pe.employee_id = ${employeeId}
          and b.deleted = false
          and s.deleted = false
        order by t.ordered
        limit ${pageable.pageSize} offset ${pageable.offset}
        """.trimIndent()

        val result = jdbcTemplate.query(query){ rs, _ ->
            TaskResponse(
                id = rs.getLong("task_id"),
                title = rs.getString("task_title"),
                priority = TaskPriority.valueOf(rs.getString("priority")),
                startDate = rs.getDate("start_date"),
                endDate = rs.getDate("end_date"),
                order = rs.getShort("task_order"),
                timeEstimateAmount = rs.getInt("time_estimate_amount"),
                description = rs.getString("task_description"),
                parentTaskId = rs.getLong("parent_task_id").takeIf { !rs.wasNull() },
                ownerId = rs.getLong("owner_id"),
                ownerName = rs.getString("owner_full_name"),
                ownerPhotoHashId = rs.getString("owner_image_hash_id"),
                boardId = rs.getLong("board_id"),
                boardName = rs.getString("board_name"),
                stateId = rs.getLong("state_id"),
                stateOrder = rs.getShort("state_order"),
                files = rs.getString("files")?.let { objectMapper.readValue(it, Array<FileDataResponse>::class.java).toList() } ?: emptyList(),
                employees = rs.getString("employees")?.let { objectMapper.readValue(it, Array<TaskEmployeeResponse>::class.java).toList() } ?: emptyList(),
                subTasks = rs.getString("subtasks")?.let { objectMapper.readValue(it, Array<TaskResponse>::class.java).toList() } ?: emptyList()
            )
        }

        val count = """
        select count(t)
        from task t
                 join board b on t.board_id = b.id
                 join state s on t.state_id = s.id
                 join users o on t.owner_id = o.id
                 join project p on p.id = b.project_id
                 join department d on p.department_id = d.id
                 join organization org on d.organization_id = org.id
                 join project_employee pe on p.id = pe.project_id
        where t.deleted = false
          and t.parent_task_id is null
          and ((${taskId} is null) or t.id = ${taskId})
          and ((${boardId} is null) or b.id = ${boardId})
          and ((${priorityQuery} is null) or  t.priority = ${priorityQuery})
          and (
             ${startLocalDate} is not null and ${endLocalDate} is not null and t.end_date between ${startLocalDate} and ${endLocalDate}
             or (${startLocalDate} is null or ${endLocalDate} is null)
          )
          and org.id = ${organizationId}
          and pe.employee_id = ${employeeId}
          and b.deleted = false
          and s.deleted = false
        """.trimIndent()
        val totalElements = jdbcTemplate.queryForObject(count, Int::class.java)!!.toLong()

        return PageImpl(result, pageable, totalElements)
    }


    @Transactional
    override fun edit(id: Long, request: TaskUpdateRequest): TaskResponse {
        val task = repository.findByIdAndDeletedFalse(id)
            ?: throw TaskNotFoundException()
        val owner = extraService.getEmployeeFromCurrentUser()
        val config = config(owner.organization)

        validationService.validateProjectEmployee(
            task.board.project,
            owner
        )
        validationService.validateTimeEstimateAmount(request.timeEstimateAmount, config)
        task.apply {
            request.title?.let {
                if (request.title != title) {
                    this.title = updateTitle(it, task, owner.user!!)
                }
            }
            request.priority?.let {
                if (request.priority != priority) {
                    this.priority = updateTaskPriority(it, task, owner.user!!)
                }
            }
            request.filesHashIds?.let { file ->
                if (request.filesHashIds != files.map { it.hashId }) {
                    this.files = updateFiles(file, task, owner.user!!)
                }
            }
            request.employeeIds?.let { employee ->
                if (request.employeeIds != task.employees.map { it.employee.id!! }) {
                    this.employees = updateTaskEmployees(employee, task, owner.user!!, state)
                }
            }
            if (startDate?.time != request.startDate) {
                this.startDate = updateTaskStartDate(request.startDate?.let { Date(it) }, task, owner.user!!)
            }
            if (endDate?.time != request.endDate) {
                this.endDate = updateTaskDueDate(request.endDate?.let { Date(it) }, task, owner.user!!)
            }
            if (timeEstimateAmount != request.timeEstimateAmount) {
                this.timeEstimateAmount = updateTaskEstimateTime(request.timeEstimateAmount, task, owner.user!!)
            }
            this.description = request.description
        }
        repository.save(task)
        val ownerPhotoHashId = getOwnerPhotoHashId(task.owner.id!!, owner.organization.id!!)  // Employee ID ishlatilmoqda
        return TaskResponse.toDto(task, subtasks =  subtasks(task).map { TaskResponse.toDto(it) },  ownerPhotoHashId = ownerPhotoHashId)
    }

    private fun changeOrder(task: Task, order: Short): BaseMessage {
        repository.save(
            task.apply {
                this.order = updateTaskOrder(order, task)
            }
        )
        return BaseMessage.OK
    }

    @Transactional
    override fun changeState(id: Long, stateId: Long, order: Short): BaseMessage {
        val task = repository.findByIdAndDeletedFalse(id)
            ?: throw TaskNotFoundException()
        val state = stateRepository.findByIdAndBoardIdAndDeletedFalse(stateId, task.board.id!!)
            ?: throw StateNotFoundException()
        val owner = extraService.getEmployeeFromCurrentUser()

        validationService.validateProjectEmployee(task.board.project, owner)
        validationService.validateTaskOrder(state, order)
        if (task.parentTask != null)
            throw InvalidSubTaskException()

        if (state.id == task.state.id!!) return changeOrder(task, order)

        validationService.validateStateOrderWithTask(task.state, state)

        shiftTasksOfNewState(order, repository.findAllByStateIdAndDeletedFalseOrderByOrder(state.id!!))
        shiftTasksOfOldState(task.order, repository.findAllByStateIdAndDeletedFalseOrderByOrder(task.state.id!!))
        repository.save(
            task.apply {
                this.state = updateState(state, task, owner.user!!)
                this.order = order
            }
        )
        val subtasks = subtasks(task).map { subtask ->
            subtask.apply {
                this.state = task.state
            }
        }
        repository.saveAll(subtasks)
        return BaseMessage.OK
    }

    @Transactional
    override fun moveTaskToBoard(taskId: Long, newBoardId: Long): TaskResponse {
        val task = repository.findByIdAndDeletedFalse(taskId)
            ?: throw TaskNotFoundException()
        val newBoard = boardRepository.findByIdAndDeletedFalse(newBoardId)
            ?: throw BoardNotFoundException()
        val owner = extraService.getEmployeeFromCurrentUser()
        val config = config(owner.organization)

        validationService.validateTimeEstimateAmount(task.timeEstimateAmount, config)
        validationService.validateBoardProject(task, newBoard)

        task.board = newBoard
        //task.order = 1
        val newState = stateRepository.findByBoardAndOrder(newBoard, 1)
            ?: throw StateNotFoundException()
        task.state = newState
        repository.save(task)
        return TaskResponse.toDto(task)
    }

    @Transactional
    override fun delete(request: TaskDeleteRequest) {
        val tasks = repository.findAllByIdInAndDeletedFalse(request.taskIds)
        val employee = extraService.getEmployeeFromCurrentUser()
        validationService.validateDeletingTasks(
            tasks, request, employee
        )
        deleteTasks(tasks, request)
        if (request.isSubtask && tasks.isNotEmpty()) {
            val parents = tasks.map { it.parentTask!! }.toSet()
            createSubtaskRemoveHistory(tasks, employee.user!!, parents.first())
        }
        deleteTaskSubscribers(tasks)
    }

    private fun deleteTaskSubscribers(tasks: MutableList<Task>) {
        tasks.forEach { task ->
            val taskSubscribers = taskSubscriberRepository.findAllByTaskIdAndDeletedFalse(task.id!!)
            taskSubscribers.forEach { subtask ->
                taskSubscriberRepository.trash(subtask.id!!)
            }
        }
    }

    private fun deleteTasks(tasks: MutableList<Task>, request: TaskDeleteRequest) {
        tasks.forEach { task ->
            if (request.isSubtask) {
                deleteSubTask(task)
            } else
                deleteTask(task)
        }
    }

    private fun deleteSubTask(task: Task) {
        repository.trash(task.id!!)
        deleteFiles(task.files)
    }

    private fun deleteTask(task: Task) {
        shiftTasksOfOldState(
            task.order,
            repository.findAllByStateIdAndDeletedFalseOrderByOrder(
                task.state.id!!
            )
        )
        repository.trash(task.id!!)
        deleteFiles(task.files)
        val subtasks = subtasks(task)
        if (subtasks.isNotEmpty())
            repository.trashList(
                subtasks.map { subtask ->
                    deleteFiles(subtask.files)
                    subtask.id!!
                }
            )
    }

    private fun subtasks(task: Task): MutableList<Task> {
        return repository.findAllByParentTaskIdAndDeletedFalse(task.id!!)
    }

    private fun parentTask(parentTaskId: Long?, state: State): Task? {
        return parentTaskId?.let {
            val parent = repository.findByIdAndDeletedFalse(it) ?: throw TaskNotFoundException()
            validationService.validateBoardState(parent, state)
            if (parent.parentTask != null)
                throw InvalidSubTaskException()
            parent
        }
    }

    private fun taskEmployees(employees: List<Long>, state: State): MutableSet<ProjectEmployee> {
        val exists = projectEmployeeRepository.existsByProjectAndEmployeeIds(
            state.board.project.id!!,
            employees,
            employees.size.toLong()
        )
        if (!exists) throw ProjectEmployeeNotFoundException()
        if (employeeRepository.existsByIdInAndStatusNot(employees, Status.ACTIVE))
            throw AssignDeactivatedEmployeeException()
        return projectEmployeeRepository.findAllById(employees).toMutableSet()
    }

    private fun files(fileHashIds: List<String>): MutableList<FileAsset> {
        return fileHashIds.map {
            fileAssetRepository.findByHashIdAndDeletedFalse(it) ?: throw FileNotFoundException()
        }.toMutableList()
    }

    private fun taskOrder(state: State): Short {
        val lastTask = repository.findTopByStateIdAndDeletedFalseOrderByOrderDesc(state.id!!)
        return lastTask?.order?.plus(1)?.toShort() ?: 1
    }

    private fun taskOrder(parentTask: Task): Short {
        val lastSubTask = repository.findTopByParentTaskIdAndDeletedFalseOrderByOrderDesc(parentTask.id!!)
        return lastSubTask?.order?.plus(1)?.toShort() ?: 1
    }


    private fun updateTaskEmployees(
        employeeIds: List<Long>,
        task: Task,
        owner: User,
        state: State
    ): MutableSet<ProjectEmployee> {
        val currentEmployeeIds = task.employees.map { it.id }.toSet()
        val updatedEmployees = taskEmployees(employeeIds, state).toMutableSet()
        val newEmployees = updatedEmployees.filterNot { currentEmployeeIds.contains(it.id) }.toMutableSet()
        val employeesToRemove = task.employees.filterNot { employeeIds.contains(it.id) }.toMutableList()

        newEmployees.forEach { projectEmployee ->
            subscriber(projectEmployee.employee)?.let {
                saveTaskSubscriber(task, it, false)
            }
        }
        if (newEmployees.isNotEmpty()) {
            actionHistoryService.create(
                TaskActionHistoryRequest(
                    task.id!!,
                    owner.id!!,
                    TaskAction.ASSIGN_USER,
                    subjectEmployeeIds = newEmployees.map { it.employee.id!! }
                )
            )
        }

        if (employeesToRemove.isNotEmpty()) {
            actionHistoryService.create(
                TaskActionHistoryRequest(
                    task.id!!,
                    owner.id!!,
                    TaskAction.REMOVE_USER,
                    subjectEmployeeIds = employeesToRemove.map { it.employee.id!! }
                )
            )
        }
        deleteTaskSubscribers(employeesToRemove, task)
        return updatedEmployees
    }

    private fun createSubtaskRemoveHistory(
        subtasks: MutableList<Task>,
        owner: User,
        task: Task
    ) {
        if (subtasks.isNotEmpty()) {
            actionHistoryService.create(
                TaskActionHistoryRequest(
                    task.id!!,
                    owner.id!!,
                    TaskAction.REMOVE_SUB_TASK,
                    subtasks = subtasks.map { it.id!! }.toMutableList()
                )
            )
        }
    }


    private fun updateFiles(fileHashIds: List<String>, task: Task, owner: User): MutableList<FileAsset> {
        val currentFiles = task.files.map { it.hashId }.toSet()
        val updatedFiles = files(fileHashIds).toMutableList()

        val newFiles = updatedFiles.filterNot { currentFiles.contains(it.hashId) }.toMutableList()
        val filesToRemove = task.files.filterNot { fileHashIds.contains(it.hashId) }

        if (newFiles.isNotEmpty()) {
            actionHistoryService.create(
                TaskActionHistoryRequest(
                    task.id!!,
                    owner.id!!,
                    TaskAction.FILE_UPLOAD,
                    fileHashIds = newFiles.map { it.hashId }
                )
            )
        }

        if (filesToRemove.isNotEmpty()) {
            actionHistoryService.create(
                TaskActionHistoryRequest(
                    task.id!!,
                    owner.id!!,
                    TaskAction.REMOVE_FILE,
                    fileHashIds = filesToRemove.map { it.hashId }
                )
            )
        }
        return updatedFiles
    }


    private fun createFilesHistory(task: Task, owner: User) {
        if (task.files.isNotEmpty()) {
            actionHistoryService.create(
                TaskActionHistoryRequest(
                    task.id!!,
                    owner.id!!,
                    TaskAction.FILE_UPLOAD,
                    fileHashIds = task.files.map { it.hashId }
                )
            )
        }
    }

    private fun updateTitle(title: String, task: Task, owner: User): String {
        actionHistoryService.create(
            TaskActionHistoryRequest(
                task.id!!,
                owner.id!!,
                TaskAction.CHANGE_TITLE,
                title = title
            )
        )

        return title
    }

    private fun updateState(state: State, task: Task, owner: User): State {
        actionHistoryService.create(
            TaskActionHistoryRequest(
                task.id!!,
                owner.id!!,
                TaskAction.CHANGE_STATE,
                fromStateId = task.state.id!!,
                toStateId = state.id!!
            )
        )
        return state
    }


    private fun updateTaskPriority(priority: TaskPriority, task: Task, owner: User): TaskPriority {
        val taskAction = when {
            task.priority == TaskPriority.NONE && priority != TaskPriority.NONE -> TaskAction.SET_TASK_PRIORITY
            task.priority != TaskPriority.NONE && priority == TaskPriority.NONE -> TaskAction.REMOVE_TASK_PRIORITY
            task.priority != priority -> TaskAction.CHANGE_TASK_PRIORITY
            else -> null
        }
        taskAction?.let {
            actionHistoryService.create(
                TaskActionHistoryRequest(
                    task.id!!,
                    owner.id!!,
                    taskAction,
                    taskPriority = priority
                )
            )
        }
        return priority
    }


    private fun createTaskHistory(task: Task, owner: User) {
        task.parentTask?.let {
            actionHistoryService.create(
                TaskActionHistoryRequest(
                    task.id!!,
                    owner.id!!,
                    TaskAction.CREATE_SUB_TASK
                )
            )
        } ?: run {
            actionHistoryService.create(
                TaskActionHistoryRequest(
                    task.id!!,
                    owner.id!!,
                    TaskAction.CREATE_TASK,
                    taskPriority = task.priority,
                    subjectEmployeeIds = task.employees.map { it.employee.id!! },
                    startDate = task.startDate,
                    dueDate = task.endDate,
                    timeEstimateAmount = task.timeEstimateAmount
                )
            )
        }
    }

    private fun updateTaskDueDate(dueDate: Date?, task: Task, owner: User): Date? {
        val taskAction = when {
            task.endDate == null && dueDate != null -> TaskAction.SET_DUE_DATE
            task.endDate != null && dueDate == null -> TaskAction.REMOVE_DUE_DATE
            task.endDate != dueDate -> TaskAction.CHANGE_DUE_DATE
            else -> null
        }
        taskAction?.let {
            actionHistoryService.create(
                TaskActionHistoryRequest(
                    task.id!!,
                    owner.id!!,
                    taskAction,
                    dueDate = task.endDate ?: dueDate
                )
            )
        }
        return dueDate
    }

    private fun updateTaskStartDate(startDate: Date?, task: Task, owner: User): Date? {
        val taskAction = when {
            task.startDate == null && startDate != null -> TaskAction.SET_START_DATE
            task.startDate != null && startDate == null -> TaskAction.REMOVE_START_DATE
            task.startDate != startDate -> TaskAction.CHANGE_START_DATE
            else -> null
        }
        taskAction?.let {
            actionHistoryService.create(
                TaskActionHistoryRequest(
                    task.id!!,
                    owner.id!!,
                    taskAction,
                    startDate = task.startDate ?: startDate
                )
            )
        }
        return startDate
    }

    private fun updateTaskEstimateTime(timeEstimateAmount: Int?, task: Task, owner: User): Int? {
        val taskAction = when {
            task.timeEstimateAmount != null && timeEstimateAmount == null -> TaskAction.ESTIMATED_TIME_AMOUNT_REMOVED
            else -> TaskAction.TIME_AMOUNT_ESTIMATED
        }

        actionHistoryService.create(
            TaskActionHistoryRequest(
                task.id!!,
                owner.id!!,
                taskAction,
                timeEstimateAmount = timeEstimateAmount ?: task.timeEstimateAmount
            )
        )
        return timeEstimateAmount
    }


    private fun updateTaskOrder(order: Short, task: Task): Short {
        val currentOrder = task.order
        if (order == currentOrder) return order

        val tasks = repository.findAllByStateIdAndDeletedFalseOrderByOrder(task.state.id!!)
        if (order < currentOrder) {
            tasks.filter { it.order in order until currentOrder }
                .forEach { it.order = (it.order + 1).toShort() }
        } else {
            tasks.filter { it.order in (currentOrder + 1)..order }
                .forEach { it.order = (it.order - 1).toShort() }
        }
        repository.saveAll(tasks)
        return order
    }

    private fun shiftTasksOfOldState(order: Short, tasks: MutableList<Task>) {
        for (t in tasks) {
            if (t.order > order) {
                t.order = (t.order - 1).toShort()
            }
        }
        repository.saveAll(tasks)
    }

    private fun shiftTasksOfNewState(order: Short, tasks: MutableList<Task>) {
        for (t in tasks) {
            if (t.order >= order) {
                t.order = (t.order + 1).toShort()
            }
        }
        repository.saveAll(tasks)
    }

    private fun config(organization: Organization): WorkingDateConfig {
        val configs = workingDateConfigRepository.findAllByOrganizationIdAndDeletedFalse(organization.id!!)
        if (configs.isNotEmpty()) return configs.first()
        throw WorkingDateConfigNotFoundException()
    }

    private fun deleteFiles(files: MutableList<FileAsset>) {
        files.forEach { asset ->
            val file = File("${asset.uploadFolder}/${asset.uploadFileName}")
            file.delete()
            fileAssetRepository.trash(asset.id!!)
        }
    }

    private fun subscriber(employee: Employee): Subscriber? {
        return subscriberRepository.findByUserIdAndBotUsernameAndDeletedFalse(employee.user!!.id!!, BOT_USERNAME)
    }

    private fun createTaskSubscriber(task: Task, owner: Employee) {
        val taskOwnerSubscriber = subscriber(owner)
        val boardOwnerSubscriber = subscriber(task.board.owner)
        if (taskOwnerSubscriber != null)
            saveTaskSubscriber(task, taskOwnerSubscriber, true)
        if (boardOwnerSubscriber != null)
            saveTaskSubscriber(task, boardOwnerSubscriber, true)
    }

    fun saveTaskSubscriber(task: Task, subscriber: Subscriber, immutable: Boolean) {
        taskSubscriberRepository.findBySubscriberIdAndTaskId(
            subscriber.id!!,
            task.id!!
        )?.let { taskSubscriber ->
            if (taskSubscriber.deleted) {
                taskSubscriberRepository.save(
                    taskSubscriber.apply {
                        this.deleted = false
                    }
                )
            }
        } ?: run {
            taskSubscriberRepository.save(
                TaskSubscriber(
                    subscriber,
                    task,
                    immutable
                )
            )
        }
    }

    private fun deleteTaskSubscribers(employees: MutableList<ProjectEmployee>, task: Task) {
        taskSubscriberRepository.removeTaskSubscribersByEmployee(employees.map { it.id!! }, task.id!!)
    }

    private fun createTaskEmployeesHistory(
        task: Task,
        owner: User,
    ) {
        task.employees.forEach { projectEmployee ->
            subscriber(projectEmployee.employee)?.let {
                saveTaskSubscriber(task, it, false)
            }
        }
    }

    override fun getTasksByType(): TypedTaskResponse {
        val employee = extraService.getEmployeeFromCurrentUser()
        val projectEmployees = projectEmployeeRepository.findByEmployee(employee)

        if (projectEmployees.isEmpty()) {
            return TypedTaskResponse()
        }


        val projectEmployeeIds = projectEmployees.map { it.id!! }

        val openedTasks = repository.findOpenedTasks(projectEmployeeIds).map { TaskStatResponse.toDto(it) }
        val closedTasks = repository.findClosedTasks(projectEmployeeIds).map { TaskStatResponse.toDto(it) }
        val upcomingTasks = repository.findUpcomingTasks(projectEmployeeIds).map { TaskStatResponse.toDto(it) }

        return TypedTaskResponse(
            openedTasks = openedTasks,
            closedTasks = closedTasks,
            upcomingTasks = upcomingTasks
        )
    }

    }
