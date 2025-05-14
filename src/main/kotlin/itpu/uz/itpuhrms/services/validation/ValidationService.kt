package itpu.uz.itpuhrms.services.validation

import itpu.uz.itpuhrms.*
import org.springframework.stereotype.Service
import itpu.uz.itpuhrms.services.employee.EmployeeRepository
import itpu.uz.itpuhrms.services.projectEmployee.ProjectEmployeeRepository
import itpu.uz.itpuhrms.services.state.StateRepository
import itpu.uz.itpuhrms.services.task.TaskDeleteRequest
import itpu.uz.itpuhrms.services.task.TaskRepository
import itpu.uz.itpuhrms.services.task.TaskSubscriberRepository
import itpu.uz.itpuhrms.services.user.UserCredentialsRepository
import itpu.uz.itpuhrms.utils.Constants
import java.io.File
import kotlin.math.abs
import org.springframework.security.access.AccessDeniedException

interface ValidationService {
    fun validateOrganizationUser(organization: Organization, user: User)
    fun validateDifferentOrganizations(department: Department, position: Position)
    fun validateDifferentOrganizations(organization: Organization, department: Department)
    fun validateDifferentOrganizations(organization: Organization, position: Position)
    fun validateDifferentOrganizations(board: Board, employee: Employee)
    fun validateDifferentOrganizations(organization: Organization, sessionOrganization: Organization)
    fun validateUserCredentials(user: User)
    fun validateExistingEmployee(user: User, organization: Organization)
    fun validateEmployeeImage(fileAsset: FileAsset)
    fun validateImageTypeAndSize(fileAsset: FileAsset)
    fun validateDifferentOrganizations(department: Department, position: Position, organization: Organization)
    fun validateDifferentOwners(timeTracking: TimeTracking, employee: Employee)
    fun validateDifferentOwners(employee: ProjectEmployee)
    fun validateDeletingPermission(employee: ProjectEmployee)
    fun validateProjectEmployee(project: Project, employee: Employee)
    fun validateProjectEmployee(task: Task, employee: Employee)
    fun validateDeletingProjectEmployee(projectEmployee: ProjectEmployee)
    fun validateBoardState(task: Task, state: State)
    fun validateBoardTask(task: Task, board: Board)
    fun validateStateOrderWithTask(currentState: State, newState: State)
    fun validateExistingProjectEmployee(employee: Employee, project: Project)
    fun validateTaskUser(task: Task, user: User)
    fun validateDateRange(startDate: Long, endDate: Long)
    fun validateTaskOrder(state: State, order: Short)
    fun validateProjectEmployees(project: Project, employeeIds: List<Long>)
    fun validateProjectEmployees(project: Project, employeeIds: List<Long>?, meMode: Boolean)
    fun validateStateOrder(board: Board, order: Short)
    fun validateTimeEstimateAmount(timeEstimateAmount: Int?, config: WorkingDateConfig)
    fun validateTasks(tasks: MutableList<Task>, employee: Employee)
    fun validateDeletingTasks(tasks: MutableList<Task>, request: TaskDeleteRequest, employee: Employee)
    fun validateSubTasks(subtasks: MutableList<Task>, employee: Employee)
    fun validateExistingTaskSubscriber(subscriber: Subscriber, task: Task)
    fun validateTaskSubscriber(taskSubscriber: TaskSubscriber, subscriber: Subscriber)
    fun validateBoardProject(task: Task, newBoard: Board)
}

@Service
class ValidationServiceImpl(
    private val employeeRepository: EmployeeRepository,
    private val credentialsRepository: UserCredentialsRepository,
    private val projectEmployeeRepository: ProjectEmployeeRepository,
    private val stateRepository: StateRepository,
    private val taskRepository: TaskRepository,
    private val taskSubscriberRepository: TaskSubscriberRepository
) : ValidationService {

    override fun validateOrganizationUser(organization: Organization, user: User) {
        if (!employeeRepository.existsByUserIdAndOrganizationIdAndDeletedFalse(user.id!!, organization.id!!))
            throw ForeignEmployeeException()
    }

    override fun validateDifferentOrganizations(department: Department, position: Position) {
        if (department.organization.id != position.organization.id) throw DifferentOrganizationsException()
    }

    override fun validateDifferentOrganizations(
        department: Department,
        position: Position,
        organization: Organization
    ) {
        if (department.organization.id != organization.id || position.organization.id != organization.id)
            throw DifferentOrganizationsException()
    }

    override fun validateDifferentOrganizations(organization: Organization, department: Department) {
        if (department.organization.id != organization.id) throw DifferentOrganizationsException()
    }

    override fun validateDifferentOrganizations(organization: Organization, sessionOrganization: Organization) {
        if (organization.id != sessionOrganization.id) throw DifferentOrganizationsException()
    }

    override fun validateDifferentOrganizations(organization: Organization, position: Position) {
        if (organization.id != position.organization.id) throw DifferentOrganizationsException()
    }

    override fun validateDifferentOrganizations(board: Board, employee: Employee) {
        if (employee.organization.id != board.owner.organization.id) throw DifferentOrganizationsException()
    }

    override fun validateUserCredentials(user: User) {
        val exists = credentialsRepository.existsByUserIdAndDeletedFalse(user.id!!)
        if (!exists) throw UserCredentialsNotFoundException()
    }

    override fun validateExistingEmployee(user: User, organization: Organization) {
        val exists = employeeRepository.existsByUserIdAndOrganizationIdAndDeletedFalse(
            user.id!!,
            organization.id!!
        )
        if (exists) throw EmployeeExistsWithinOrganizationException()
    }

    override fun validateEmployeeImage(fileAsset: FileAsset) {
        val exists = employeeRepository.existsByImageAssetId(fileAsset.id!!)
        if (exists) throw EmployeeImageAlreadyUsedException()
    }


    override fun validateImageTypeAndSize(fileAsset: FileAsset) {
        val split = fileAsset.fileContentType.split("/")
        val contentType = split[0]
        val extension = split[1]
        if (extension !in listOf("jpeg", "jpg", "png") || contentType != "image") throw NotValidImageTypeException()

        fileAsset.let {
            val file = File("${it.uploadFolder}/${it.uploadFileName}")
            val kilobyte = (file.readBytes().size / 1024)

            if (kilobyte > Constants.EMPLOYEE_IMAGE_KILOBYTE_LIMIT) throw ImageSizeLimitException()
        }
    }

    override fun validateDifferentOwners(employee: ProjectEmployee) {
        if (employee.projectEmployeeRole !in mutableListOf(
                ProjectEmployeeRole.OWNER,
                ProjectEmployeeRole.MANAGER
            )
        ) throw AccessDeniedException()
    }

    override fun validateDeletingPermission(employee: ProjectEmployee) {
        if (employee.projectEmployeeRole !in mutableListOf(ProjectEmployeeRole.OWNER)) throw AccessDeniedException()
    }


    override fun validateDifferentOwners(timeTracking: TimeTracking, employee: Employee) {
        if (timeTracking.owner.id != employee.user!!.id) throw AccessDeniedException()
    }

    override fun validateProjectEmployee(project: Project, employee: Employee) {
        val exist = projectEmployeeRepository.existsByProjectIdAndEmployeeIdAndDeletedFalse(
            project.id!!,
            employee.id!!
        )
        if (!exist) throw ProjectNotFoundException()
    }

    override fun validateProjectEmployee(task: Task, employee: Employee) {
        val exist = projectEmployeeRepository.existsByProjectIdAndEmployeeIdAndDeletedFalse(
            task.board.project.id!!,
            employee.id!!
        )
        if (!exist) throw ProjectNotFoundException()
    }

    override fun validateBoardState(task: Task, state: State) {
        if (task.state.board.id != state.board.id)
            throw StateNotFoundException()
    }

    override fun validateBoardTask(task: Task, board: Board) {
        if (task.state.board.id != board.id)
            throw BoardNotFoundException()
    }

    override fun validateStateOrderWithTask(currentState: State, newState: State) {
        val result = abs(currentState.order - newState.order)
        if (result != 1 && currentState.order < newState.order) throw IncorrectStateOrderException()
    }

    override fun validateExistingProjectEmployee(employee: Employee, project: Project) {
        if (projectEmployeeRepository.existsByProjectIdAndEmployeeIdAndDeletedFalse(
                project.id!!,
                employee.id!!
            )
        ) throw ProjectEmployeeAlreadyExistsException()
    }

    override fun validateDeletingProjectEmployee(projectEmployee: ProjectEmployee) {
        if (projectEmployee.project.owner.id!! == projectEmployee.employee.id!!) throw DeletingProjectOwnerException()
        val exists = taskRepository.existsByProjectIdAndEmployeeIdAndDeletedFalse(
            projectEmployee.project.id!!,
            projectEmployee.employee.id!!
        )
        if (exists) throw EmployeeConnectedToTaskException()
    }

    override fun validateTaskUser(task: Task, user: User) {
        taskRepository.existsByProjectEmployeeUser(user.id!!, task.id!!).runIfFalse { throw TaskNotFoundException() }
    }

    override fun validateDateRange(startDate: Long, endDate: Long) {
        if (startDate.yearWithUTC() != endDate.yearWithUTC() || startDate.monthWithUTC() != endDate.monthWithUTC())
            throw InvalidDateRangeException()
    }

    override fun validateTaskOrder(state: State, order: Short) {
        val lastTask = taskRepository.findTopByStateIdAndDeletedFalseOrderByOrderDesc(state.id!!)
        val max = lastTask?.order?.plus(1) ?: 1
        val min = 1
        if (order > max || order < min) throw IncorrectTaskOrderException()
    }

    override fun validateProjectEmployees(project: Project, employeeIds: List<Long>) {
        val exist = projectEmployeeRepository.existsByProjectAndEmployeeIds(
            project.id!!,
            employeeIds,
            employeeIds.size.toLong()
        )
        if (!exist) throw EmployeeNotFoundException()
    }

    override fun validateProjectEmployees(project: Project, employeeIds: List<Long>?, meMode: Boolean) {
        if (!meMode) employeeIds?.let { validateProjectEmployees(project, it) }
    }

    override fun validateStateOrder(board: Board, order: Short) {
        val closed = stateRepository.findTopByBoardIdAndDeletedFalseOrderByOrderDesc(board.id!!)
            ?: throw StateNotFoundException()
        if (order.toInt() <= 1 || order >= closed.order) throw AccessDeniedException("Access denied")
    }

    override fun validateTimeEstimateAmount(timeEstimateAmount: Int?, config: WorkingDateConfig) {
        if (timeEstimateAmount != null && timeEstimateAmount > config.requiredMinutes)
            throw AccessDeniedException()
    }

    override fun validateBoardProject(task: Task, newBoard: Board) {
        if (task.board.project.id != newBoard.project.id) {
            throw DifferentBoardProjectException()
        }
    }

    override fun validateTasks(tasks: MutableList<Task>, employee: Employee) {
        tasks.forEach { task ->
            validateProjectEmployee(
                task.board.project,
                employee
            )
            if (task.parentTask != null)
                throw InvalidTaskException()
        }
        val boardIds = tasks.map { it.board.id!! }.toSet()
        if (boardIds.size != 1) throw DifferentBoardTaskException()
    }

    override fun validateDeletingTasks(tasks: MutableList<Task>, request: TaskDeleteRequest, employee: Employee) {
        if (request.isSubtask) {
            validateSubTasks(tasks, employee)
        } else {
            validateTasks(tasks, employee)
        }
    }

    override fun validateSubTasks(subtasks: MutableList<Task>, employee: Employee) {
        subtasks.forEach { subtask ->
            if (subtask.parentTask == null)
                throw InvalidSubTaskException()
        }
        val parents = subtasks.map { it.parentTask!! }.toSet()
        if (parents.size != 1) throw DifferentParentTaskException()

        validateProjectEmployee(
            parents.first().board.project,
            employee
        )
    }

    override fun validateExistingTaskSubscriber(subscriber: Subscriber, task: Task) {
        val exist = taskSubscriberRepository.existsByTaskIdAndSubscriberIdAndDeletedFalse(
            task.id!!,
            subscriber.id!!
        )
        if (!exist) throw TaskSubscriberAlreadyExistException()
    }

    override fun validateTaskSubscriber(taskSubscriber: TaskSubscriber, subscriber: Subscriber) {
        if (taskSubscriber.subscriber.id!! != subscriber.id!!)
            throw AccessDeniedException()
    }
}