package itpu.uz.itpuhrms.services

import itpu.uz.itpuhrms.*
import org.springframework.stereotype.Service



interface SubscriberService {
    fun getMe(): SubscriberResponse
    fun update(request: SubscriberRequest): SubscriberResponse
}


interface TaskSubscriberService {
    fun create(request: TaskSubscriberRequest): TaskSubscriberResponse
    fun get(boardId: Long): List<TaskSubscriberResponse>
    fun getOne(id: Long): TaskSubscriberResponse
    fun delete(id: Long)
}

interface NotificationSettingsService {
    fun create(request: BoardSettingsRequest): BoardSettingsResponse
    fun update(id: Long, request: BoardSettingsUpdateRequest): BoardSettingsResponse
    fun getOne(boardId: Long): BoardSettingsResponse
}

@Service
class SubscriberServiceImpl(
    private val extraService: ExtraService,
    private val repository: SubscriberRepository
) : SubscriberService {
    override fun getMe(): SubscriberResponse {
        val employee = extraService.getEmployeeFromCurrentUser()
        return SubscriberResponse.toResponse(subscriber(employee))
    }

    override fun update(request: SubscriberRequest): SubscriberResponse {
        val employee = extraService.getEmployeeFromCurrentUser()
        return repository.findByUserIdAndBotUsernameAndDeletedFalse(employee.user!!.id!!, BOT_USERNAME)?.let { subscriber ->
            val saved = repository.save(
                subscriber.apply {
                    this.language = request.language
                }
            )
            SubscriberResponse.toResponse(saved)
        } ?: throw SubscriberNotFoundException()
    }


    private fun subscriber(employee: Employee): Subscriber {
        return repository.findByUserIdAndBotUsernameAndDeletedFalse(employee.user!!.id!!, BOT_USERNAME) ?: throw SubscriberNotFoundException()
    }
}


@Service
class TaskSubscriberServiceImpl(
    private val repository: TaskSubscriberRepository,
    private val extraService: ExtraService,
    private val taskRepository: TaskRepository,
    private val subscriberRepository: SubscriberRepository,
    private val validationService: ValidationService,
    private val boardRepository: BoardRepository
) : TaskSubscriberService {

    override fun create(request: TaskSubscriberRequest): TaskSubscriberResponse {
        return request.run {
            val task = task(taskId)
            val employee = extraService.getEmployeeFromCurrentUser()
            val subscriber = subscriber(employee)
            validationService.validateExistingTaskSubscriber(
                subscriber, task
            )

            val saved = repository.save(
                TaskSubscriber(
                    subscriber,
                    task,
                    false
                )
            )
            TaskSubscriberResponse.toResponse(saved)
        }
    }

    override fun get(boardId: Long): List<TaskSubscriberResponse> {
        val subscriber = subscriber(extraService.getEmployeeFromCurrentUser())
        val board = boardRepository.findByIdAndDeletedFalse(boardId)
            ?: throw BoardNotFoundException()
        validationService.validateProjectEmployee(
            board.project,
            extraService.getEmployeeFromCurrentUser()
        )
        return taskSubscribers(subscriber, board).map(TaskSubscriberResponse::toResponse)
    }

    override fun getOne(id: Long): TaskSubscriberResponse {
        return repository.findByIdAndDeletedFalse(id)?.let { taskSubscriber ->
            validationService.validateProjectEmployee(
                taskSubscriber.task,
                extraService.getEmployeeFromCurrentUser()
            )
            TaskSubscriberResponse.toResponse(taskSubscriber)
        } ?: throw TaskSubscriberNotFoundException()
    }

    override fun delete(id: Long) {
        repository.findByIdAndDeletedFalse(id)?.let { taskSubscriber ->
            validationService.validateProjectEmployee(
                taskSubscriber.task,
                extraService.getEmployeeFromCurrentUser()
            )
            repository.trash(id)
        } ?: throw TaskSubscriberNotFoundException()
    }

    private fun task(taskId: Long): Task {
        return taskRepository.findByIdAndDeletedFalse(taskId)
            ?: throw TaskNotFoundException()
    }

    private fun subscriber(employee: Employee): Subscriber {
        return subscriberRepository.findByUserIdAndBotUsernameAndDeletedFalse(employee.user!!.id!!, BOT_USERNAME)
            ?: throw SubscriberNotFoundException()
    }

    private fun taskSubscribers(subscriber: Subscriber, board: Board): MutableList<TaskSubscriber> {
        return repository.findAllBySubscriberIdAndTaskBoardIdAndDeletedFalse(
            subscriber.id!!,
            board.id!!
        )
    }
}

@Service
class NotificationSettingsServiceImpl(
    private val repository: BoardNotificationSettingsRepository,
    private val extraService: ExtraService,
    private val validationService: ValidationService,
    private val boardRepository: BoardRepository,
    private val projectEmployeeRepository: ProjectEmployeeRepository
) : NotificationSettingsService {
    override fun create(request: BoardSettingsRequest): BoardSettingsResponse {
        return request.run {
            val employee = extraService.getEmployeeFromCurrentUser()
            val board = boardRepository.findByIdAndDeletedFalse(boardId)
                ?: throw BoardNotFoundException()
            validationService.validateDifferentOwners(
                projectEmployee(board.project, employee)
            )
            if (repository.existsByBoardIdAndDeletedFalse(board.id!!))
                throw BoardSettingsAlreadyExistException()

            BoardSettingsResponse.toResponse(
                repository.save(
                    BoardNotificationSettings(
                        board, actions
                    )
                )
            )
        }
    }

    override fun update(id: Long, request: BoardSettingsUpdateRequest): BoardSettingsResponse {
        return repository.findByIdAndDeletedFalse(id)?.let { settings ->
            validationService.validateDifferentOwners(
                projectEmployee(settings.board.project, extraService.getEmployeeFromCurrentUser())
            )
            val save = repository.save(
                settings.apply {
                    this.actions = request.actions
                }
            )
            BoardSettingsResponse.toResponse(save)
        } ?: throw BoardSettingsNotFoundException()
    }

    override fun getOne(boardId: Long): BoardSettingsResponse {
        val employee = extraService.getEmployeeFromCurrentUser()
        val board = boardRepository.findByIdAndDeletedFalse(boardId)
            ?: throw BoardNotFoundException()
        validationService.validateDifferentOwners(
            projectEmployee(board.project, employee)
        )
        val settings = repository.findByBoardIdAndDeletedFalse(boardId)
            ?: throw BoardSettingsNotFoundException()
        return BoardSettingsResponse.toResponse(settings)
    }

    private fun projectEmployee(project: Project, employee: Employee): ProjectEmployee {
        return projectEmployeeRepository.findByProjectIdAndEmployeeIdAndDeletedFalse(
            project.id!!,
            employee.id!!
        ) ?: throw EmployeeNotFoundException()
    }
}