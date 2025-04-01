package itpu.uz.itpuhrms.services

import itpu.uz.itpuhrms.*
import org.apache.commons.logging.LogFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*

interface ScheduleService {
    fun updateUserTourniquets()
    fun updateInactiveFiles()
    fun updateTableDates()
    fun updateTourniquetEmployees()
}

@Service
class ScheduleServiceImpl(
    private val organizationRepository: OrganizationRepository,
    private val userTourniquetRepository: UserTourniquetRepository,
    private val fileAssetRepository: FileAssetRepository,
    private val trackerRepository: TourniquetTrackerRepository,
    private val employeeRepository: EmployeeRepository,
    private val tableDateService: TableDateService,
    private val dataRepository: EmployeeTourniquetDataRepository,
) : ScheduleService {
    private val logger = LogFactory.getLog(javaClass)


    //Every day at 9 a.m
    @Scheduled(cron = "0 0 9 * * ?")
    override fun updateUserTourniquets() {
        logger.info("UserTourniquet Schedule started at ${LocalDateTime.now()}")

        val currentMidnight = Date().midnight()
        val events = userTourniquetRepository.findLastInUserTourniquets(currentMidnight)

        events.forEach { event ->
            val midnight = event.time.midnight().plusDay(1)
            val duration = event.time.minuteDuration(midnight)

            val newEventTime = if (duration > 60) {
                event.time.plusHour(1)
            } else {
                midnight
            }

            event.run {
                saveUserTourniquet(event, newEventTime)
                saveTourniquetTracker(event, newEventTime)
                updateEmployeeAtOffice(event.user, event.organization)
            }
        }
    }

    //Every day at midnight - 12am
    @Scheduled(cron = "0 0 0 * * ?")
    override fun updateInactiveFiles() {
        logger.info("Inactive files Schedule started at ${LocalDateTime.now()}")
        fileAssetRepository.findAllByActiveFalseAndDeletedFalse().forEach {
            fileAssetRepository.delete(it)
        }
    }

    //Every day at midnight - 12am
    @Scheduled(cron = "0 0 0 * * ?")
    override fun updateTableDates() {
        tableDateService.refresh()
    }

    //Every day at midnight - 12am
    @Scheduled(cron = "0 0 0 * * ?")
    override fun updateTourniquetEmployees() {
        val dataList = dataRepository.findByStatusInAndDeletedFalse(mutableListOf(EmployeeStatus.UPDATED))
        val updatedData = dataList.map { data ->
            data.apply {
                this.status = EmployeeStatus.ACTIVE
            }
        }
        dataRepository.saveAll(updatedData)
    }

    private fun saveUserTourniquet(event: UserTourniquet, newEventTime: Date) {
        event.run {
            userTourniquetRepository.save(
                UserTourniquet(
                    organization,
                    user, tourniquet, tableDate, newEventTime, userType, UserTourniquetType.OUT
                )
            )
        }
    }


    private fun saveTourniquetTracker(event: UserTourniquet, newEventTime: Date) {
        event.run {
            trackerRepository.save(
                TourniquetTracker(
                    time,
                    newEventTime,
                    time.minuteDuration(newEventTime),
                    user,
                    tourniquet,
                    tableDate
                )
            )
        }
    }


    private fun updateEmployeeAtOffice(user: User, organization: Organization) {
        employeeRepository.findByUserIdAndOrganizationIdAndDeletedFalse(user.id!!, organization.id!!)?.let {
            it.atOffice = false
            employeeRepository.save(it)
        }
    }


}