package itpu.uz.itpuhrms.services

import itpu.uz.itpuhrms.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import itpu.uz.itpuhrms.TaskAction.*
import java.time.LocalDateTime

interface TaskActionHistoryService {
    fun create(request: TaskActionHistoryRequest)
    fun getOneById(id: Long): TaskActionHistoryResponse
    fun getAll(taskId: Long, pageable: Pageable): Page<TaskActionHistoryResponse>
//    fun delete(id: Long)
}


@Service
class TaskActionHistoryServiceImpl(
    private val repository: TaskActionHistoryRepository,
    private val taskRepository: TaskRepository,
    private val userRepository: UserRepository,
    private val fileAssetRepository: FileAssetRepository,
    private val stateRepository: StateRepository,
    private val employeeRepository: EmployeeRepository,
    private val commentRepository: CommentRepository,
    private val timeTrackingRepository: TimeTrackingRepository,
    private val notificationService: TelegramNotificationService,
) : TaskActionHistoryService {

    @Transactional
    override fun create(request: TaskActionHistoryRequest) {
        val date = LocalDateTime.now()
        val task = taskRepository.findByIdAndDeletedFalse(request.taskId)
            ?: throw TaskNotFoundException()
        val owner = userRepository.findByIdAndDeletedFalse(request.ownerId)
            ?: throw UserNotFoundException()

        when (request.action) {
            CREATE_TASK -> createTaskHistory(task, owner)
            CREATE_SUB_TASK -> createSubTaskHistory(task, owner)
            REMOVE_SUB_TASK -> removeSubTaskHistory(task, owner, request)
            ASSIGN_USER -> assignUserTaskHistory(task, owner, request)
            REMOVE_USER -> removeUserTaskHistory(task, owner, request)
            CHANGE_TITLE -> changeTitleHistory(task, owner, request)

            SET_START_DATE,
            CHANGE_START_DATE,
            REMOVE_START_DATE -> saveStartDateHistory(task, owner, request.action, request)

            SET_DUE_DATE,
            CHANGE_DUE_DATE,
            REMOVE_DUE_DATE -> saveDueDateHistory(task, owner, request.action, request)

            SET_TASK_PRIORITY,
            CHANGE_TASK_PRIORITY,
            REMOVE_TASK_PRIORITY -> saveTaskPriorityHistory(task, owner, request.action, request)

            CHANGE_STATE -> changeStateHistory(task, owner, request)
            FILE_UPLOAD -> saveFileHistory(task, owner, request.action, request)
            REMOVE_FILE -> saveFileHistory(task, owner, request.action, request)
            ADD_COMMENT -> saveCommentHistory(task, owner, request)
            TIME_TRACKED -> saveTimeTrackingHistory(task, owner, request)
            TIME_AMOUNT_ESTIMATED,
            ESTIMATED_TIME_AMOUNT_REMOVED -> saveTimeEstimateHistory(task, owner, request.action, request)
        }
        notificationService.sendMessage(request, date)
    }

    override fun getOneById(id: Long) = repository.findByIdAndDeletedFalse(id)?.let {
        TaskActionHistoryResponse.toDto(it)
    } ?: throw TaskActionHistoryNotFoundException()

    override fun getAll(taskId: Long, pageable: Pageable) =
        repository.findAllByTaskIdAndDeletedFalseOrderByIdDesc(taskId, pageable)
            .map { TaskActionHistoryResponse.toDto(it) }

//    @Transactional
//    override fun delete(id: Long) {
//        repository.trash(id) ?: throw TaskActionHistoryNotFoundException()
//    }


    private fun removeSubTaskHistory(
        task: Task,
        owner: User,
        request: TaskActionHistoryRequest
    ): List<TaskActionHistory> {
        val subtasks = taskRepository.findAllById(request.subtasks!!)

        val actions = subtasks.map { subtask ->
            TaskActionHistory(
                task,
                owner,
                REMOVE_SUB_TASK,
                subTask = subtask
            )
        }
        return repository.saveAll(actions)
    }


    private fun assignUserTaskHistory(
        task: Task,
        owner: User,
        request: TaskActionHistoryRequest
    ): List<TaskActionHistory> {
        val employees = employeeRepository.findAllById(request.subjectEmployeeIds!!)
        val actionHistories = employees.map { employee ->
            TaskActionHistory(
                task,
                owner,
                ASSIGN_USER,
                subjectEmployee = employee
            )
        }
        return repository.saveAll(actionHistories)
    }

    private fun removeUserTaskHistory(
        task: Task,
        owner: User,
        request: TaskActionHistoryRequest
    ): List<TaskActionHistory> {
        val employees = employeeRepository.findAllById(request.subjectEmployeeIds!!)
        val actionHistories = employees.map { employee ->
            TaskActionHistory(
                task,
                owner,
                REMOVE_USER,
                subjectEmployee = employee
            )
        }
        return repository.saveAll(actionHistories)
    }

    private fun saveDueDateHistory(
        task: Task,
        owner: User,
        action: TaskAction,
        request: TaskActionHistoryRequest
    ): TaskActionHistory {
        return repository.save(
            TaskActionHistory(
                task,
                owner,
                action,
                dueDate = request.dueDate
            )
        )
    }

    private fun saveTimeEstimateHistory(
        task: Task,
        owner: User,
        action: TaskAction,
        request: TaskActionHistoryRequest
    ): TaskActionHistory {
        return repository.save(
            TaskActionHistory(
                task,
                owner,
                action,
                timeEstimateAmount = request.timeEstimateAmount
            )
        )
    }

    private fun saveStartDateHistory(
        task: Task,
        owner: User,
        action: TaskAction,
        request: TaskActionHistoryRequest
    ): TaskActionHistory {
        return repository.save(
            TaskActionHistory(
                task,
                owner,
                action,
                startDate = request.startDate
            )
        )
    }

    private fun changeTitleHistory(
        task: Task,
        owner: User,
        request: TaskActionHistoryRequest
    ): TaskActionHistory {
        return repository.save(
            TaskActionHistory(
                task,
                owner,
                CHANGE_TITLE,
                title = request.title
            )
        )
    }

    private fun saveTaskPriorityHistory(
        task: Task,
        owner: User,
        action: TaskAction,
        request: TaskActionHistoryRequest
    ): TaskActionHistory {
        return repository.save(
            TaskActionHistory(
                task,
                owner,
                action,
                priority = request.taskPriority
            )
        )
    }

    private fun changeStateHistory(
        task: Task,
        owner: User,
        request: TaskActionHistoryRequest
    ): TaskActionHistory {
        val fromState = request.fromStateId?.let {
            stateRepository.findByIdAndDeletedFalse(it) ?: throw StateNotFoundException()
        }
        val toState = request.toStateId?.let {
            stateRepository.findByIdAndDeletedFalse(it) ?: throw StateNotFoundException()
        }
        return repository.save(
            TaskActionHistory(
                task,
                owner,
                CHANGE_STATE,
                fromState = fromState,
                toState = toState,
            )
        )
    }

    private fun saveFileHistory(
        task: Task,
        owner: User,
        action: TaskAction,
        request: TaskActionHistoryRequest
    ): TaskActionHistory {
        return repository.save(
            TaskActionHistory(
                task,
                owner,
                action,
                files = request.fileHashIds?.let { files(it) }
            )
        )
    }

    private fun saveCommentHistory(
        task: Task,
        owner: User,
        request: TaskActionHistoryRequest
    ): TaskActionHistory {
        val comment = commentRepository.findByIdAndDeletedFalse(request.commentId!!)
            ?: throw CommentNotFoundException()

        return repository.save(
            TaskActionHistory(
                task,
                owner,
                ADD_COMMENT,
                comment = comment
            )
        )
    }

    private fun saveTimeTrackingHistory(
        task: Task,
        owner: User,
        request: TaskActionHistoryRequest
    ) {
        val timeTrackingList = timeTrackingRepository.findAllById(request.timeTrackingIds!!)
        repository.saveAll(
            timeTrackingList.map {
                repository.save(
                    TaskActionHistory(
                        task,
                        owner,
                        TIME_TRACKED,
                        timeTracking = it
                    )
                )
            }
        )
    }


    private fun files(fileHashIds: List<String>): MutableList<FileAsset> {
        return fileHashIds.map {
            fileAssetRepository.findByHashIdAndDeletedFalse(it) ?: throw FileNotFoundException()
        }.toMutableList()
    }

    private fun createTaskHistory(
        task: Task,
        owner: User,
    ): List<TaskActionHistory> {
        val actionHistories = mutableListOf<TaskActionHistory>()

        actionHistories.add(
            TaskActionHistory(
                task,
                owner,
                CREATE_TASK
            )
        )

        if (task.priority != TaskPriority.NONE) {
            actionHistories.add(
                TaskActionHistory(
                    task,
                    owner,
                    SET_TASK_PRIORITY,
                    priority = task.priority
                )
            )
        }
        if (task.startDate != null) {
            actionHistories.add(
                TaskActionHistory(
                    task,
                    owner,
                    SET_START_DATE,
                    startDate = task.startDate
                )
            )
        }
        if (task.endDate != null) {
            actionHistories.add(
                TaskActionHistory(
                    task,
                    owner,
                    SET_DUE_DATE,
                    dueDate = task.endDate
                )
            )
        }
        if (task.employees.isNotEmpty()) {
            val employeeActions = task.employees.map { projectEmployee ->
                TaskActionHistory(
                    task,
                    owner,
                    ASSIGN_USER,
                    subjectEmployee = projectEmployee.employee
                )
            }
            actionHistories.addAll(
                employeeActions
            )
        }
        if (task.timeEstimateAmount != null) {
            actionHistories.add(
                TaskActionHistory(
                    task,
                    owner,
                    TIME_AMOUNT_ESTIMATED,
                    timeEstimateAmount = task.timeEstimateAmount
                )
            )
        }
        val files = mutableListOf<FileAsset>()
        files.addAll(files)
        if (files.isNotEmpty()) {
            actionHistories.add(
                TaskActionHistory(
                    task,
                    owner,
                    FILE_UPLOAD,
                    files = files
                )
            )
        }
        return repository.saveAll(actionHistories)
    }

    private fun createSubTaskHistory(
        task: Task,
        owner: User
    ): List<TaskActionHistory> {
        val actionHistories = mutableListOf<TaskActionHistory>()
        actionHistories.add(
            TaskActionHistory(
                task.parentTask!!,
                owner,
                CREATE_SUB_TASK,
                subTask = task
            )
        )
        return repository.saveAll(actionHistories)
    }
}