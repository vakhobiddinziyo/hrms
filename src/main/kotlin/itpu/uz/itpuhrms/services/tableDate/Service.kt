package itpu.uz.itpuhrms.services.tableDate

import itpu.uz.itpuhrms.*
import itpu.uz.itpuhrms.services.ExtraService
import itpu.uz.itpuhrms.services.organization.OrganizationRepository
import itpu.uz.itpuhrms.services.userAbsenceTracker.UserAbsenceTrackerRepository
import itpu.uz.itpuhrms.services.validation.ValidationService
import itpu.uz.itpuhrms.services.workingDate.WorkingDateConfigRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

interface TableDateService {
    fun create(request: TableDateRequest): TableDateResponse
    fun getOneById(id: Long): TableDateResponse
    fun getAll(startDate: Long, endDate: Long, pageable: Pageable): Page<TableDateResponse>
    fun edit(id: Long, request: TableDateUpdateRequest): TableDateResponse

    //    fun delete(id: Long)
    fun refresh()
}

@Service
class TableDateServiceImpl(
    private val repository: TableDateRepository,
    private val orgRepository: OrganizationRepository,
    private val extraService: ExtraService,
    private val validationService: ValidationService,
    private val jdbcTemplate: JdbcTemplate,
    private val userAbsenceTrackerRepository: UserAbsenceTrackerRepository,
    private val organizationRepository: OrganizationRepository,
    private val workingDateConfigRepository: WorkingDateConfigRepository
) : TableDateService {

    @Transactional
    override fun create(request: TableDateRequest): TableDateResponse {
        if (repository.existsByDateAndOrganizationIdAndTypeAndDeletedFalse(
                Date(request.date),
                request.organizationId,
                request.type
            )
        ) throw TableDateAlreadyExistException()

        val organization = orgRepository.findByIdAndDeletedFalse(request.organizationId)
            ?: throw OrganizationNotFoundException()
        return TableDateResponse.toDto(
            repository.save(
                TableDate(
                    Date(request.date),
                    request.type,
                    organization
                )
            )
        )
    }

    override fun getOneById(id: Long) = repository.findByIdAndDeletedFalse(id)?.let {
        TableDateResponse.toDto(it)
    } ?: throw TableDateNotFoundException()

    override fun getAll(startDate: Long, endDate: Long, pageable: Pageable): Page<TableDateResponse> {
        val organization = extraService.getOrgFromCurrentUser()


        return repository.findByOrganizationIdAndDeletedFalsePageable(
            organization.id!!,
            Date(startDate),
            Date(endDate),
            pageable
        ).map {
            TableDateResponse.toDto(it, userAbsenceTrackerRepository.countByTableDateIdAndDeletedFalse(it.id!!))
        }
    }

    @Transactional
    override fun edit(id: Long, request: TableDateUpdateRequest) =
        repository.findByIdAndDeletedFalse(id)?.let { tableDate ->
            validationService.validateDifferentOrganizations(
                tableDate.organization,
                extraService.getOrgFromCurrentUser()
            )
            if (tableDate.date < Date()) throw PastDateException()

            tableDate.apply {
                type = request.type
            }
            TableDateResponse.toDto(repository.save(tableDate))
        } ?: throw TableDateNotFoundException()

    @Transactional
    override fun refresh() {
        val organizations = organizationRepository.findAllNotDeleted()
        val calendar = mutableListOf<TableDate>()
        organizations.forEach { org ->
            val currentMonth = Date().startOfMonth()
            val nextMonth = currentMonth.plusMonth(1)

            val currentMonthDates = daysOfMonth(currentMonth)
            val nextMonthDates = daysOfMonth(nextMonth)
            calendar.addAll(saveTableDates(currentMonthDates, org))
            calendar.addAll(saveTableDates(nextMonthDates, org))
            repository.saveAll(calendar)
        }
    }

    private fun daysOfMonth(date: Date): MutableList<Date> {
        val dates = mutableListOf<Date>()
        val calendar = Calendar.getInstance()
        calendar.time = date
        val lastDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        for (i in 1..lastDay) {
            calendar.set(Calendar.DAY_OF_MONTH, i)
            dates.add(calendar.time)
        }
        return dates
    }

    private fun saveTableDates(
        dates: List<Date>,
        org: Organization
    ): MutableList<TableDate> {
        val calendar = mutableListOf<TableDate>()
        val exists = repository.existByDateAndOrganizationIdAndDeletedFalse(
            dates.first(),
            org.id!!
        )
        if (!exists) {
            dates.forEach { date ->
                val weekDay = date.weekDay()
                val config =
                    workingDateConfigRepository.findByOrganizationIdAndDayAndDeletedFalse(org.id!!, weekDay)
                val tableDate = config?.let {
                    TableDate(
                        date,
                        TableDateType.WORK_DAY,
                        org
                    )
                } ?: run {
                    TableDate(
                        date,
                        TableDateType.REST_DAY,
                        org
                    )
                }
                calendar.add(tableDate)
            }
        }
        return calendar
    }


}