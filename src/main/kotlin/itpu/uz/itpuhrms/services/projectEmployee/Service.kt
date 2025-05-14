package itpu.uz.itpuhrms.services.projectEmployee

import itpu.uz.itpuhrms.*
import itpu.uz.itpuhrms.base.BaseMessage
import itpu.uz.itpuhrms.services.ExtraService
import itpu.uz.itpuhrms.services.employee.EmployeeRepository
import itpu.uz.itpuhrms.services.validation.ValidationService
import itpu.uz.itpuhrms.services.notification.BOT_USERNAME
import itpu.uz.itpuhrms.services.project.ProjectRepository
import itpu.uz.itpuhrms.services.subscriber.SubscriberRepository
import itpu.uz.itpuhrms.services.task.TaskSubscriberRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

interface ProjectEmployeeService {
    fun create(request: ProjectEmployeeRequest): BaseMessage
    fun getOneById(id: Long): ProjectEmployeeResponse
    fun getAll(projectId: Long, search: String?, status: String?, pageable: Pageable): Page<ProjectEmployeeResponse>
    fun edit(id: Long, request: ProjectEmployeeEditRequest): ProjectEmployeeResponse
    fun delete(id: Long)
    fun getMe(projectId: Long): ProjectEmployeeResponse
}

@Service
class ProjectEmployeeServiceImpl(
    private val repository: ProjectEmployeeRepository,
    private val projectRepository: ProjectRepository,
    private val employeeRepository: EmployeeRepository,
    private val extraService: ExtraService,
    private val validationService: ValidationService,
    private val subscriberRepository: SubscriberRepository,
    private val taskSubscriberRepository: TaskSubscriberRepository
) : ProjectEmployeeService {

    @Transactional
    override fun create(request: ProjectEmployeeRequest): BaseMessage {
        request.run {
            val owner = extraService.getEmployeeFromCurrentUser()
            val project = projectRepository.findByIdAndDeletedFalse(projectId)
                ?: throw ProjectNotFoundException()

            validationService.validateDifferentOwners(
                projectEmployee(owner, project)
            )
            if (role == ProjectEmployeeRole.OWNER) throw AccessDeniedException("Access denied")

            val projectEmployees = employeeIds.map { employeeId ->
                val employee =
                    employeeRepository.findByIdAndPhStatusAndDeletedFalse(employeeId, PositionHolderStatus.BUSY)
                        ?: throw EmployeeNotFoundException()

                validationService.validateDifferentOrganizations(employee.organization, owner.organization)
                validationService.validateExistingProjectEmployee(employee, project)
                ProjectEmployee(
                    project,
                    employee,
                    role
                )
            }
            repository.saveAll(projectEmployees)
            return BaseMessage.OK
        }
    }

    override fun getOneById(id: Long) =
        repository.findByIdAndDeletedFalse(id)?.let {
            validationService.validateProjectEmployee(
                it.project,
                extraService.getEmployeeFromCurrentUser()
            )
            ProjectEmployeeResponse.toDto(it)
        } ?: throw ProjectEmployeeNotFoundException()

    override fun getAll(projectId: Long, search: String?, status: String?, pageable: Pageable): Page<ProjectEmployeeResponse> {
        val project = projectRepository.findByIdAndDeletedFalse(projectId)
            ?: throw ProjectNotFoundException()
        val employee = extraService.getEmployeeFromCurrentUser()
        validationService.validateProjectEmployee(
            project,
            employee
        )
        return repository.findAllProjectId(projectId, employee.id!!, search ?: "", status, pageable)
            .map { ProjectEmployeeResponse.toDto(it) }
    }

    @Transactional
    override fun edit(id: Long, request: ProjectEmployeeEditRequest) =
        repository.findByIdAndDeletedFalse(id)?.let {
            if (request.role == ProjectEmployeeRole.OWNER) throw AccessDeniedException("Access denied")
            if (it.project.owner.id!! == it.employee.id!!) throw AccessDeniedException("Access denied")

            validationService.validateDifferentOwners(
                projectEmployee(extraService.getEmployeeFromCurrentUser(), it.project)
            )
            it.projectEmployeeRole = request.role
            repository.save(it)
            ProjectEmployeeResponse.toDto(it)
        } ?: throw ProjectEmployeeNotFoundException()

    @Transactional
    override fun delete(id: Long) {
        repository.findByIdAndDeletedFalse(id)?.let { projectEmployee ->
            validationService.validateDeletingPermission(
                projectEmployee(extraService.getEmployeeFromCurrentUser(), projectEmployee.project)
            )
            taskSubscriberRepository.trashList(taskSubscribers(projectEmployee))
            repository.trash(id)
        } ?: throw ProjectEmployeeNotFoundException()
    }

    override fun getMe(projectId: Long): ProjectEmployeeResponse {
        projectRepository.findByIdAndDeletedFalse(projectId)?.let {
            val employee = extraService.getEmployeeFromCurrentUser()
            val projectEmployee = repository.findByProjectIdAndEmployeeIdAndDeletedFalse(
                projectId,
                employee.id!!
            ) ?: throw ProjectEmployeeNotFoundException()
            return ProjectEmployeeResponse.toDto(projectEmployee)
        } ?: throw ProjectNotFoundException()
    }

    private fun taskSubscribers(projectEmployee: ProjectEmployee): List<Long> {
        val subscriber = subscriber(projectEmployee.employee) ?: return listOf()
        return taskSubscriberRepository.findAllBySubscriberIdAndTaskBoardProjectIdAndDeletedFalse(
            subscriber.id!!,
            projectEmployee.project.id!!
        ).map {
            it.id!!
        }
    }

    private fun subscriber(employee: Employee): Subscriber? {
        return subscriberRepository.findByUserIdAndBotUsernameAndDeletedFalse(employee.user!!.id!!, BOT_USERNAME)
    }

    private fun projectEmployee(employee: Employee, project: Project): ProjectEmployee {
        return repository.findByProjectIdAndEmployeeIdAndDeletedFalse(project.id!!, employee.id!!)
            ?: throw EmployeeNotFoundException()
    }
}