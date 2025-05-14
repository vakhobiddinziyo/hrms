package itpu.uz.itpuhrms.services.userAbsenceTracker

import itpu.uz.itpuhrms.*
import itpu.uz.itpuhrms.services.ExtraService
import itpu.uz.itpuhrms.services.employee.EmployeeRepository
import itpu.uz.itpuhrms.services.file.FileAssetRepository
import itpu.uz.itpuhrms.services.tableDate.TableDateRepository
import itpu.uz.itpuhrms.services.tableDate.TableDateResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import java.util.*

interface UserAbsenceTrackerService {

    fun create(request: UserAbsenceTrackerRequest): UserAbsenceTrackerResponse
    fun update(id: Long, request: UserAbsenceTrackerRequest): UserAbsenceTrackerResponse
    fun getOne(id: Long): UserAbsenceTrackerResponse
    fun getAll(
        userId: Long?,
        startDate: Long,
        endDate: Long,
        eventType: String?,
        pageable: Pageable
    ): Page<UserAbsenceTrackerResponse>
    fun getAllByTableDate(id: Long, pageable: Pageable): Page<UserAbsenceTrackerAdminResponse>
    fun delete(id: Long)
}

@Service
class UserAbsenceTrackerServiceImpl(
    private val repository: UserAbsenceTrackerRepository,
    private val tableDateRepository: TableDateRepository,
    private val extraService: ExtraService,
    private val fileAssetRepository: FileAssetRepository,
    private val jdbcTemplate: JdbcTemplate,
    private val employeeRepository: EmployeeRepository,
): UserAbsenceTrackerService {

    override fun create(request: UserAbsenceTrackerRequest): UserAbsenceTrackerResponse {
        val employee = extraService.getEmployeeFromCurrentUser()
        val organization = extraService.getOrgFromCurrentUser()
        val tableDate = validationTableDate(request.tableDateId)
        existsTableDateByOrganization(tableDate.id!!, organization.id!!)
        existsByTableDate(request.tableDateId, employee.user!!.id!!)
        val file = validationFileHashId(request.fileHashId)

        val userAbsenceTracker = UserAbsenceTracker(
            user = employee.user!!,
            tableDate = tableDate,
            reasonDoc = file,
            eventType = request.eventType,
            description = request.description,
        )
        val fileDataResponse: FileDataResponse? = request.fileHashId?.let { FileDataResponse.toResponse(file!!) }
        return UserAbsenceTrackerResponse.toDto(repository.save(userAbsenceTracker), TableDateResponse.toDto(tableDate),fileDataResponse)
    }

    override fun update(id: Long, request: UserAbsenceTrackerRequest): UserAbsenceTrackerResponse {
        val userAbsenceTracker = repository.findByIdAndDeletedFalse(id)
           ?: throw UserAbsenceTrackerNotFoundException()

        val employee = extraService.getEmployeeFromCurrentUser()
        validUserAndAbsenceTracker(employee.user!!.id!!, id)
        existsByTableDate(id, request.tableDateId, employee.user!!.id!!)
        existsTableDateByOrganization(request.tableDateId, employee.organization.id!!)
        userAbsenceTracker.tableDate = tableDateRepository.findByIdAndDeletedFalse(request.tableDateId) ?: throw TableDateNotFoundException()
        userAbsenceTracker.reasonDoc = validationFileHashId(request.fileHashId)
        userAbsenceTracker.description = request.description
        userAbsenceTracker.eventType = request.eventType
        val newTacker = repository.save(userAbsenceTracker)
        val fileDataResponse: FileDataResponse? = request.fileHashId?.let { FileDataResponse.toResponse(newTacker.reasonDoc!!) }
        return UserAbsenceTrackerResponse.toDto(newTacker, TableDateResponse.toDto(newTacker.tableDate), fileDataResponse)
    }

    override fun getOne(id: Long): UserAbsenceTrackerResponse {
        val tracker = repository.findByIdAndDeletedFalse(id)
            ?: throw UserAbsenceTrackerNotFoundException()
        val employee = extraService.getEmployeeFromCurrentUser()
        validUserAndAbsenceTracker(employee.user!!.id!!, tracker.id!!)
        val fileDataResponse: FileDataResponse? = tracker.reasonDoc?.let { FileDataResponse.toResponse(it) }
        return UserAbsenceTrackerResponse.toDto(tracker, TableDateResponse.toDto(tracker.tableDate), fileDataResponse)
    }

    override fun getAll(
        userId: Long?,
        startDate: Long,
        endDate: Long,
        eventType: String?,
        pageable: Pageable
    ): Page<UserAbsenceTrackerResponse> {

        val eventTypeQuery = eventType?.let { "'$eventType'" }

        val query = """
select
    u.id                                                          as id,
    u.user_id                                                     as user_id,
    t.id                                                          as table_date_id,
    cast(extract(epoch from t.date) * 1000 as bigint )            as date,
    t.type                                                        as table_date_type,
    t.organization_id                                             as organization_id,
    u.event_type                                                  as event_type,
    f.hash_id                                                     as hash_id,
    f.file_size                                                   as file_size,
    f.file_content_type                                           as content_type,
    f.file_name                                                   as file_name,
    u.description                                                 as description
from user_absence_tracker u
         join table_date t on t.id = u.table_date_id
         left join file_asset f on u.reason_doc_id = f.id
where u.deleted = false
  and (${userId} is null or u.user_id = ${userId})
  and  t.date between '${startDate.timeWithUTC()}' and '${endDate.timeWithUTC()}'
  and (${eventTypeQuery} is null or u.event_type = ${eventTypeQuery})
        """.trimIndent()

        val countQuery = """
             select 
                 count(u)
            from user_absence_tracker u
                     join table_date t on t.id = u.table_date_id
                     left join file_asset f on u.reason_doc_id = f.id
             where u.deleted = false
             and (${userId} is null or u.user_id = ${userId})
            and  t.date between '${startDate.timeWithUTC()}' and '${endDate.timeWithUTC()}'
            and (${eventTypeQuery} is null or u.event_type = ${eventTypeQuery})
        """.trimIndent()
        val result = jdbcTemplate.query(query){rs, _ ->
            UserAbsenceTrackerResponse(
                id = rs.getLong("id"),
                userId = rs.getLong("user_id"),
                tableDateResponse = TableDateResponse(
                    id = rs.getLong("table_date_id"),
                    date = rs.getLong("date"),
                    type = TableDateType.valueOf(rs.getString("table_date_type")),
                    organizationId = rs.getLong("organization_id"),
                ),
                fileDataResponse = rs.getString("hash_id").takeIf { !rs.wasNull() }?.let {
                    FileDataResponse(
                        fileSize = rs.getLong("file_size"),
                        fileName = rs.getString("file_name"),
                        fileContentType = rs.getString("content_type"),
                        hashId = it,
                    )
                },
                eventType = EventType.valueOf(rs.getString("event_type")),
                description = rs.getString("description"),
            )
        }
        val count = jdbcTemplate.queryForObject(countQuery, Long::class.java)!!

        return PageImpl(result, pageable, count)
    }

    override fun getAllByTableDate(id: Long, pageable: Pageable): Page<UserAbsenceTrackerAdminResponse> {
        val organization = extraService.getOrgFromCurrentUser()
        val tableDate = tableDateRepository.findByIdAndDeletedFalse(id) ?: throw TableDateNotFoundException()
        existsTableDateByOrganization(tableDate.id!!, organization.id!!)
        return repository.findAllByTableDateIdAndDeletedFalse(id, pageable)
            .map { UserAbsenceTrackerAdminResponse.toDto(
                it,
                employeeRepository.findByUserIdAndOrganizationIdAndDeletedFalse(it.user.id!!, organization.id!!)!!,
                it.reasonDoc?.let { file -> FileDataResponse.toResponse(file) }) }
    }

    override fun delete(id: Long) {
        val employee = extraService.getEmployeeFromCurrentUser()
        val tracker = repository.findByIdAndDeletedFalse(id)
            ?: throw UserAbsenceTrackerNotFoundException()
        validUserAndAbsenceTracker(employee.user!!.id!!, tracker.id!!)
        repository.trash(id)
    }

    private fun validationFileHashId(fileHashId: String?) : FileAsset? {
       return fileHashId?.let { fileAssetRepository.findByHashIdAndDeletedFalse(it)
            ?: throw FileNotFoundException()
       }
    }

    private fun validationTableDate(id: Long) : TableDate {
        val tableDate =  tableDateRepository.findByIdAndDeletedFalse(id)
            ?:throw TableDateNotFoundException()
        if (tableDate.type != TableDateType.WORK_DAY) throw TableDateNotWorkDayException()
        if (tableDate.date.toLocalDate() < Date().toLocalDate()) throw PastDateException()
        return tableDate
    }

    private fun validUserAndAbsenceTracker(userId: Long, trackerId: Long) {
        if (!repository.existsByIdAndUserIdAndDeletedFalse(trackerId,userId))
            throw UserAbsenceTrackerNotFoundException()
    }

    private fun existsByTableDate(tableDateId: Long, userId: Long){
        if(repository.existsByUserIdAndTableDateIdAndDeletedFalse(userId,tableDateId))
            throw UserAbsenceTrackerAlreadyExistsException()
    }
    private fun existsByTableDate(trackerId: Long, tableDateId: Long, userId: Long){
        if(repository.existsByIdIsNotAndUserIdAndTableDateIdAndDeletedFalse(trackerId,userId,tableDateId))
            throw UserAbsenceTrackerAlreadyExistsException()
    }
    private fun existsTableDateByOrganization(tableDateId: Long,organizationId: Long){
        if (!tableDateRepository.existsByIdAndOrganizationIdAndDeletedFalse(tableDateId,organizationId))
            throw TableDateNotFoundException()
    }
}
