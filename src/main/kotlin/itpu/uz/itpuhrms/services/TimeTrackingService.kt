package itpu.uz.itpuhrms.services

import itpu.uz.itpuhrms.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.util.*

interface TimeTrackingService {
    fun create(request: TimeTrackingCreateRequest): List<TimeTrackingResponse>
    fun getOneById(id: Long): TimeTrackingResponse
    fun getAll(taskId: Long): List<TimeTrackingListResponse>
    fun edit(id: Long, request: TimeTrackingUpdateRequest): TimeTrackingResponse
    fun delete(id: Long)
}

@Service
class TimeTrackingServiceImpl(
    private val repository: TimeTrackingRepository,
    private val taskRepository: TaskRepository,
    private val extraService: ExtraService,
    private val validationService: ValidationService,
    private val actionHistoryService: TaskActionHistoryService,
    private val projectEmployeeRepository: ProjectEmployeeRepository,
    private val tableDateRepository: TableDateRepository,
    private val workingDateConfigRepository: WorkingDateConfigRepository,
) : TimeTrackingService {

    @Transactional
    override fun create(request: TimeTrackingCreateRequest): List<TimeTrackingResponse> {
        request.run {
            val employee = extraService.getEmployeeFromCurrentUser()
            val task = taskRepository.findByIdAndDeletedFalse(taskId) ?: throw TaskNotFoundException()
            val pEmployee = projectEmployeeRepository.findByProjectIdAndEmployeeIdAndDeletedFalse(
                task.board.project.id!!,
                employee.id!!
            )
            val today = tableDate(employee.organization)
            val duration = Duration.between(startTime, endTime)

            if (!task.employees.contains(pEmployee)) throw ProjectEmployeeNotFoundException()
            if (startTime > endTime) throw TimeNotCompatibleException()

            if (startDate != null && endDate != null)
                return saveTimeTrackingBetweenDates(request, task, duration, employee)


            validateTimeTracking(today, employee, duration)

            val exists = repository.existsByTaskIdAndDateAndTimeAndDeletedFalse(
                taskId,
                today.date,
                today.date.endOfDay(),
                startTime,
                endTime
            )
            if (exists) throw TimeTrackingAlreadyExistsException()

            val saved = repository.save(
                TimeTracking(
                    employee.user!!, task, today,
                    duration.toMinutes(),
                    startTime,
                    endTime,
                    note
                )
            )

            actionHistoryService.create(
                TaskActionHistoryRequest(
                    task.id!!,
                    employee.user!!.id!!,
                    TaskAction.TIME_TRACKED,
                    timeTrackingIds = listOf(saved.id!!)
                )
            )
            return listOf(TimeTrackingResponse.toDto(saved))
        }
    }


    override fun getOneById(id: Long): TimeTrackingResponse {
        return repository.findByIdAndDeletedFalse(id)?.let {
            val currentEmp = extraService.getEmployeeFromCurrentUser()
            validationService.validateProjectEmployee(it.task.board.project, currentEmp)
            TimeTrackingResponse.toDto(it)
        } ?: throw TimeTrackingNotFoundException()
    }

    override fun getAll(taskId: Long): List<TimeTrackingListResponse> {
        val employee = extraService.getEmployeeFromCurrentUser()
        val task = taskRepository.findByIdAndDeletedFalse(taskId) ?: throw TaskNotFoundException()

        validationService.validateProjectEmployee(task.board.project, employee)

        return repository.findAllByTaskIdAndDeletedFalse(task.id!!).groupBy { it.owner }.map {
            TimeTrackingListResponse.toDto(it.key, it.value)
        }
    }

    @Transactional
    override fun edit(id: Long, request: TimeTrackingUpdateRequest): TimeTrackingResponse {
        val currentEmp = extraService.getEmployeeFromCurrentUser()
        val timeTracking = repository.findByIdAndOwnerIdAndDeletedFalse(id, currentEmp.user!!.id!!)
            ?: throw TimeTrackingNotFoundException()
        validationService.validateDifferentOwners(timeTracking, currentEmp)
        val tableDate = timeTracking.tableDate
        val config = workingDateConfig(currentEmp.organization, tableDate)

        val duration = Duration.between(request.startTime, request.endTime)
        val date = tableDate.date
        val amount = repository.findUserTimeTrackingAmountByDate(
            currentEmp.user!!.id!!,
            tableDate.id!!
        )
        val leftAmount = (amount - timeTracking.duration)

        request.run {
            if (startTime > endTime) throw TimeNotCompatibleException()
            val exists = repository.existsByTaskIdAndDateAndTimeAndDeletedFalse(
                id, timeTracking.task.id!!, date,
                date.endOfDay(),
                startTime,
                endTime
            )
            if (exists) throw TimeTrackingAlreadyExistsException()
            if (config != null && (leftAmount + duration.toMinutes()) > (config.requiredMinutes * 1.5))
                throw TimeTrackingLimitException(tableDate.date)

            timeTracking.startTime = startTime
            timeTracking.endTime = endTime
            timeTracking.duration = duration.toMinutes()
            timeTracking.note = request.note
        }
        actionHistoryService.create(
            TaskActionHistoryRequest(
                timeTracking.task.id!!,
                currentEmp.user!!.id!!,
                TaskAction.TIME_TRACKED,
                timeTrackingIds = listOf(timeTracking.id!!)
            )
        )
        return TimeTrackingResponse.toDto(repository.save(timeTracking))
    }

    @Transactional
    override fun delete(id: Long) {
        repository.findByIdAndDeletedFalse(id)?.let {
            validationService.validateDifferentOwners(
                it,
                extraService.getEmployeeFromCurrentUser()
            )
            repository.trash(id)
        } ?: throw TimeTrackingNotFoundException()
    }


    private fun saveTimeTrackingBetweenDates(
        request: TimeTrackingCreateRequest,
        task: Task,
        duration: Duration,
        employee: Employee
    ): List<TimeTrackingResponse> {
        val startDate = Date(request.startDate!!).midnight()
        val endDate = Date(request.endDate!!).endOfDay()

        val user = employee.user!!

        if (startDate > endDate) throw TimeNotCompatibleException()

        return request.run {
            val exists = repository.existsByTaskIdAndDateAndTimeAndDeletedFalse(
                taskId,
                startDate,
                endDate,
                startTime,
                endTime
            )
            if (exists) throw TimeTrackingAlreadyExistsException()


            val tableDates = tableDates(employee.organization, startDate, endDate)
            val timeTrackingList = mutableListOf<TimeTracking>()
            tableDates.map { tableDate ->
                validateTimeTracking(tableDate, employee, duration)

                if (tableDate.type == TableDateType.WORK_DAY) {
                    timeTrackingList.add(
                        TimeTracking(
                            user, task, tableDate,
                            duration.toMinutes(),
                            startTime,
                            endTime,
                            note
                        )
                    )
                }
            }

            val saved = repository.saveAll(timeTrackingList)
            actionHistoryService.create(
                TaskActionHistoryRequest(
                    task.id!!,
                    user.id!!,
                    TaskAction.TIME_TRACKED,
                    timeTrackingIds = saved.map { it.id!! }
                )
            )

            timeTrackingList.map {
                TimeTrackingResponse.toDto(it)
            }
        }
    }

    private fun tableDate(organization: Organization): TableDate {
        return tableDateRepository.findByDateAndOrganizationIdAndDeletedFalse(
            Date(),
            organization.id!!
        ) ?: throw TableDateNotFoundException()
    }

    private fun tableDates(organization: Organization, startDate: Date, endDate: Date): MutableList<TableDate> {
        return tableDateRepository.findByOrganizationIdAndDeletedFalse(
            organization.id!!,
            startDate,
            endDate
        )
    }

    private fun workingDateConfig(organization: Organization, tableDate: TableDate): WorkingDateConfig? {
        val configs = workingDateConfigRepository.findAllByOrganizationIdAndDeletedFalse(organization.id!!)
        return configs.firstOrNull { it.day == tableDate.date.weekDay() }
    }

    private fun validateTimeTracking(
        tableDate: TableDate,
        employee: Employee,
        duration: Duration
    ) {

        val config = workingDateConfig(employee.organization, tableDate)

        val amount = repository.findUserTimeTrackingAmountByDate(
            employee.user!!.id!!,
            tableDate.id!!
        )

        if (config != null && (amount + duration.toMinutes()) > (config.requiredMinutes * 1.5))
            throw TimeTrackingLimitException(tableDate.date)

    }
}