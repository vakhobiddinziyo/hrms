package itpu.uz.itpuhrms.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import itpu.uz.itpuhrms.*
import org.apache.commons.logging.LogFactory
import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xssf.usermodel.XSSFCellStyle
import org.apache.poi.xssf.usermodel.XSSFRow
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*


interface UserTourniquetService {
    fun create(request: UserTourniquetRequest, image: MultipartFile? = null): String
    fun getOrganizationUserTourniquets(
        userId: Long?,
        startDate: Long,
        endDate: Long,
        search: String?,
        pageable: Pageable
    ): Page<UserTourniquetResponse>

    fun synchronizeEvents(tableDateId: Long)
    fun getUserEvents(userId: Long, startDate: Long, endDate: Long): List<EventPairData>

    fun getWorkingMinutes(
        startDate: Long,
        endDate: Long,
        search: String?,
        departmentId: Long?,
        active: Boolean,
        pageable: Pageable
    ): Page<WorkingMinutesResponse>

    fun getWorkingMinutesExcel(
        startDate: Long,
        endDate: Long,
        search: String?,
        departmentId: Long?
    ): ResponseEntity<ByteArray>

    fun getUserWorkingMinutes(
        userId: Long,
        startDate: Long, endDate: Long
    ): List<DataResponse>

    fun getLastAbsentEvents(
        startDate: Long,
        endDate: Long,
        search: String?,
        departmentId: Long?,
        pageable: Pageable
    ): Page<AbsentEvents>

    fun dailyAttendance(date: Long): ResponseEntity<ByteArray>
    fun saveUnknownPerson(response: EventResponse, image: MultipartFile?)
    fun getUnknownPeople(orgId: Long, pageable: Pageable): Page<UnknownPersonResponse>
}

@Service
class UserTourniquetServiceImpl(
    private val repository: UserTourniquetRepository,
    private val tourniquetRepository: TourniquetRepository,
    private val employeeRepository: EmployeeRepository,
    private val resultRepository: UserTourniquetResultRepository,
    private val visitorRepository: VisitorRepository,
    private val userRepository: UserRepository,
    private val tableDateRepository: TableDateRepository,
    private val validationService: ValidationService,
    private val organizationRepository: OrganizationRepository,
    private val departmentRepository: DepartmentRepository,
    private val trackerRepository: TourniquetTrackerRepository,
    private val jdbcTemplate: JdbcTemplate,
    private val objectMapper: ObjectMapper,
    private val unknownPersonRepository: UnknownPersonRepository,
    private val fileService: FileService,
    private val fileAssetRepository: FileAssetRepository,
    private val extraService: ExtraService,
    private val workingDateConfigRepository: WorkingDateConfigRepository
) : UserTourniquetService {
    private val logger = LogFactory.getLog(javaClass)

    @Transactional
    override fun synchronizeEvents(tableDateId: Long) {
        val organization = extraService.getOrgFromCurrentUser()
        val tableDate = tableDateRepository.findByIdAndDeletedFalse(tableDateId)
            ?: throw TableDateNotFoundException()

        validationService.validateDifferentOrganizations(organization, tableDate.organization)

        val users = userRepository.findOrganizationUsers(organization.id!!)
        users.forEach { user ->
            repository.updateOldOrganizationUserDailyEventsDeleted(organization.id!!, user.id!!, tableDate.id!!)
            val events = repository.findOrganizationUserDailyEvents(organization.id!!, user.id!!, tableDate.id!!)
            events.forEachIndexed { index, event ->
                if (index != events.lastIndex) {
                    val nextEvent = events[index + 1]
                    if (event.type == UserTourniquetType.IN && nextEvent.type == UserTourniquetType.OUT) {
                        saveTracker(event.time, nextEvent.time, event.time.minuteDuration(nextEvent.time), nextEvent)
                    }
                }
            }
        }
    }

    override fun getUserEvents(userId: Long, startDate: Long, endDate: Long): List<EventPairData> {
        val organization = if (extraService.getSessionRole() == Role.USER) {
            extraService.getEmployeeFromCurrentUser().organization
        } else {
            extraService.getOrgFromCurrentUser()
        }
        val user = userRepository.findByIdAndDeletedFalse(userId)
            ?: throw UserNotFoundException()

        val query = """
            SELECT * FROM
             get_paired_event_times(
                     ${organization.id},
                     ${user.id!!},
                     '${Date(startDate)}',
                     '${Date(endDate)}'
                     ) as eventTimes;
        """.trimIndent()

        logger.info("getUserEvents: $query")

        val list = jdbcTemplate.query(query) { rs, _ ->
            val eventTimesJson = rs.getString("eventTimes")
            objectMapper.readValue<List<EventPairData>>(eventTimesJson)
        }
        return list.flatten()
    }

    override fun getLastAbsentEvents(
        startDate: Long,
        endDate: Long,
        search: String?,
        departmentId: Long?,
        pageable: Pageable
    ): Page<AbsentEvents> {
        val searchQuery = search?.let { "'$search'" }
        val organization = extraService.getOrgFromCurrentUser()

        val countQuery = """
             with result as (select ut.time                                                           as time,
                                    u.id                                                              as user_id,
                                    ut.type                                                           as type,
                                    row_number() over (partition by ut.user_id order by ut.time desc) as rn
                             from user_tourniquet ut
                                      join users u on ut.user_id = u.id
                             where ut.time between '${startDate.timeWithUTC()}' and '${endDate.timeWithUTC()}'
                               and ut.organization_id = ${organization.id}
                             order by u.id, ut.time desc)
             
             select count(distinct r.user_id)
             from result r
                      join user_credentials uc on r.user_id = uc.user_id
                      join user_employment_history ueh on r.user_id = ueh.user_id
                      join department d on ueh.department_id = d.id
                      join position p on ueh.position_id = p.id
             where d.organization_id = ${organization.id}
               and p.organization_id = ${organization.id}
               and ((r.rn in (1, 2) and r.type = 'OUT') or (r.type = 'IN' and r.rn = 1))
               and ($departmentId is null or $departmentId = d.id)
               and r.time between cast(ueh.hired_date as timestamp)
                 and coalesce(cast(ueh.dismissed_date as timestamp), CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Tashkent')
               and (${searchQuery} is null
                 or uc.fio ilike concat('%', ${searchQuery}, '%')
                 or uc.pinfl ilike concat('%', ${searchQuery}, '%'))
        """.trimIndent()
        val count = jdbcTemplate.queryForObject(countQuery, Long::class.java)!!

        val query = """
            with result as (select ut.time                                                           as time,
                                   u.id                                                              as user_id,
                                   ut.type                                                           as type,
                                   row_number() over (partition by ut.user_id order by ut.time desc) as rn
                            from user_tourniquet ut
                                     join users u on ut.user_id = u.id
                            where ut.time between '${startDate.timeWithUTC()}' and '${endDate.timeWithUTC()}'
                              and ut.organization_id = ${organization.id}
                            order by u.id, ut.time desc)
            
            select r.user_id  as user_id,
                   uc.pinfl   as pinfl,
                   uc.fio     as fio,
                   d.name     as department_name,
                   p.name     as position_name,
                   json_agg(
                           json_build_object(
                                   'eventTime', r.time,
                                   'type', r.type
                           )) as eventDetails
            from result r
                     join user_credentials uc on r.user_id = uc.user_id
                     join user_employment_history ueh on r.user_id = ueh.user_id
                     join department d on ueh.department_id = d.id
                     join position p on ueh.position_id = p.id
            where d.organization_id = ${organization.id}
              and p.organization_id = ${organization.id}
              and ((r.rn in (1, 2) and r.type = 'OUT') or (r.type = 'IN' and r.rn = 1))
              and ($departmentId is null or $departmentId = d.id)
              and r.time between cast(ueh.hired_date as timestamp)
              and coalesce(cast(ueh.dismissed_date as timestamp), CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Tashkent')
              and (${searchQuery} is null
                or uc.fio ilike concat('%', ${searchQuery}, '%')
                or uc.pinfl ilike concat('%', ${searchQuery}, '%'))
            group by r.user_id, uc.id, d.id, p.id
            limit ${pageable.pageSize} offset ${pageable.offset}
        """.trimIndent()

        val absentEvents = jdbcTemplate.query(query) { rs, _ ->
            AbsentEvents(
                rs.getLong("user_id"),
                rs.getString("fio"),
                rs.getString("pinfl"),
                rs.getString("department_name"),
                rs.getString("position_name"),
                objectMapper.readValue<MutableList<EventData>>(
                    rs.getString("eventDetails")
                ).map {
                    EventData(
                        it.type,
                        it.eventTime
                    )
                }
            )
        }
        return PageImpl(absentEvents, pageable, count)
    }

    override fun getWorkingMinutes(
        startDate: Long,
        endDate: Long,
        search: String?,
        departmentId: Long?,
        active: Boolean,
        pageable: Pageable
    ): Page<WorkingMinutesResponse> {
        val organization = extraService.getOrgFromCurrentUser()

        val searchQuery = search?.let { "'$search'" }
        departmentId?.let {
            val department = departmentRepository.findByIdAndDeletedFalse(it)
                ?: throw DepartmentNotFoundException()
            if (department.organization.id != organization.id) throw DifferentOrganizationsException()
        }

        return if (active)
            activeEmployeeInfo(organization, startDate, endDate, searchQuery, departmentId, pageable)
        else
            allEmployeeInfo(organization, startDate, endDate, searchQuery, departmentId, pageable)
    }

    override fun getWorkingMinutesExcel(
        startDate: Long,
        endDate: Long,
        search: String?,
        departmentId: Long?
    ): ResponseEntity<ByteArray> {
        val excel = writeWorkingHoursExcel(startDate, endDate, search, departmentId)

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline;filename=${excel.name}")
            .body(excel.readBytes())
    }

    override fun getUserWorkingMinutes(
        userId: Long,
        startDate: Long,
        endDate: Long
    ): List<DataResponse> {
        val organization = if (extraService.getSessionRole() == Role.USER) {
            extraService.getEmployeeFromCurrentUser().organization
        } else {
            extraService.getOrgFromCurrentUser()
        }

        val user = userRepository.findByIdAndDeletedFalse(userId)
            ?: throw UserNotFoundException()

        val query = """
            select cast(extract(epoch from td.date) * 1000 as bigint) as work_date,
                   td.type                                            as day_type,
                   coalesce(tr.amount, 0)                             as work_minutes,
                   coalesce(ut.amount, 0)                             as event_amount,
                   td.id                                              as table_date_id,
                   uat.id                                             as user_absence_tracker_id
            from organization o
                     join employee e on e.organization_id = o.id
                     join users u on e.user_id = u.id
                     join table_date td on td.organization_id = o.id
                     left join (select tr.table_date_id as table_date_id,
                                       tr.user_id       as user_id,
                                       sum(tr.amount)   as amount
                                from tourniquet_tracker tr
                                where tr.deleted = false
                                group by tr.table_date_id, tr.user_id) tr on td.id = tr.table_date_id and tr.user_id = u.id
                     left join (select ut.table_date_id as table_date_id,
                                       ut.user_id       as user_id,
                                       count(ut.id)     as amount
                                from user_tourniquet ut
                                group by ut.table_date_id, ut.user_id) ut
                               on u.id = ut.user_id and ut.table_date_id = td.id
                     left join (select u.id,
                                       u.user_id,
                                       u.table_date_id
                                 from user_absence_tracker u
                                 join table_date t on u.table_date_id = t.id
                                 join users us on u.user_id = us.id
                                 where u.deleted = false
                                 and u.table_date_id = t.id
                                 and u.user_id = us.id) uat on u.id = uat.user_id and td.id = uat.table_date_id          
            where o.id = ${organization.id!!}
              and e.deleted = false
              and td.deleted = false
              and o.deleted = false
              and u.deleted = false
              and u.id = ${user.id!!}
              and td.date between '${startDate.timeWithUTC()}' and '${endDate.timeWithUTC()}'
            order by td.id
        """.trimIndent()

        return jdbcTemplate.query(query) { rs, _ ->
            DataResponse(
                rs.getLong("work_date"),
                rs.getLong("table_date_id"),
                rs.getInt("work_minutes"),
                rs.getLong("event_amount"),
                TableDateType.valueOf(rs.getString("day_type")),
                if (rs.getObject("user_absence_tracker_id") == null) null else rs.getLong("user_absence_tracker_id")
            )
        }
    }

    override fun getOrganizationUserTourniquets(
        userId: Long?,
        startDate: Long,
        endDate: Long,
        search: String?,
        pageable: Pageable
    ): Page<UserTourniquetResponse> {

        val organization = extraService.getOrgFromCurrentUser()
        val searchQuery = search?.let { "'$search'" }

        userId?.let {
            userRepository.findByIdAndDeletedFalse(it) ?: throw UserNotFoundException()
        }

        val countQuery = """
            select count(distinct u.id)
            from users u
                     join user_credentials uc on u.id = uc.user_id
                     join user_tourniquet ut on u.id = ut.user_id
                     join table_date td on ut.table_date_id = td.id
                     join user_employment_history ueh on u.id = ueh.user_id
                     join position p on ueh.position_id = p.id and p.organization_id = ${organization.id}
            where td.organization_id = ${organization.id}
              and td.deleted = false
              and ut.time between cast(ueh.hired_date as timestamp)
              and coalesce(cast(ueh.dismissed_date as timestamp), CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Tashkent')
              and (u.id = $userId or $userId is null)
              and (${searchQuery} is null
                or uc.fio ilike concat('%', ${searchQuery}, '%')
                or uc.pinfl ilike concat('%', ${searchQuery}, '%'))
              and td.date between '${startDate.timeWithUTC()}' and '${endDate.timeWithUTC()}'
        """.trimIndent()

        val count = jdbcTemplate.queryForObject(countQuery, Long::class.java)!!

        val query = """
            select u.id        as userId,
                   u.full_name as fullName,
                   uc.pinfl    as userPinfl,
                   p.name      as positionName,
                   json_agg(
                           json_build_object(
                                   'eventTime', ut.time, 'type', ut.type
                           ) order by ut.time
                   )           as eventDetails
            from users u
                     join user_credentials uc on u.id = uc.user_id
                     join user_tourniquet ut on u.id = ut.user_id
                     join table_date td on ut.table_date_id = td.id
                     join user_employment_history ueh on u.id = ueh.user_id
                     join position p on ueh.position_id = p.id and p.organization_id = ${organization.id}
            where td.organization_id = ${organization.id}
              and ut.time between ueh.hired_date
                and coalesce(ueh.dismissed_date, current_timestamp at time zone 'Asia/Tashkent')
              and (u.id = $userId or $userId is null)
              and td.deleted = false
              and (${searchQuery} is null
                or uc.fio ilike concat('%', ${searchQuery}, '%')
                or uc.pinfl ilike concat('%', ${searchQuery}, '%'))
              and td.date between '${startDate.timeWithUTC()}' and '${endDate.timeWithUTC()}'
            group by u.id, uc.id, p.id
            order by u.full_name
            limit ${pageable.pageSize} offset ${pageable.offset}    
        """.trimIndent()

        val userTourniquets = jdbcTemplate.query(query) { rs, _ ->
            UserTourniquetResponse(
                rs.getLong("userId"),
                rs.getString("fullName"),
                rs.getString("userPinfl"),
                rs.getString("positionName"),
                objectMapper.readValue<MutableList<EventData>>(
                    rs.getString("eventDetails")
                ).map {
                    EventData(
                        it.type,
                        it.eventTime
                    )
                }
            )
        }
        return PageImpl(userTourniquets, pageable, count)
    }

    override fun create(request: UserTourniquetRequest, image: MultipartFile?): String {
        logger.info("REQUEST CAME")
        request.run {

            val tourniquet = tourniquetRepository.findByNameAndDeletedFalse(name) ?: run {
                logger.info("TOURNIQUET NOT FOUND $name")
                saveResults(name, employeeId, dateTime, ResultType.FAILED, "Tourniquet not found")
                return "ok"
            }

            val snapshot = image?.let {
                fileAssetRepository.findByHashIdAndDeletedFalse(
                    fileService.upload(it).hashId
                )
            }
            val user = getUserOrVisitor(employeeId, tourniquet)

            val tableDate =
                tableDateRepository.findByDateAndOrganizationIdAndDeletedFalse(
                    request.dateTime,
                    tourniquet.organization.id!!
                )

            if (user != null && tableDate != null) {
                logger.info("TABLE DATE AND USER FOUND")
                when (tourniquet.type) {
                    TourniquetType.IN,
                    TourniquetType.OUT -> {
                        saveNormalUserTourniquet(
                            user,
                            tourniquet,
                            userType,
                            dateTime,
                            name,
                            employeeId,
                            tableDate,
                            snapshot
                        )
                    }

                    TourniquetType.DEFAULT -> {
                        saveDefaultUserTourniquet(
                            user,
                            tourniquet,
                            userType,
                            dateTime,
                            name,
                            employeeId,
                            tableDate,
                            snapshot
                        )
                    }
                }
            } else {
                logger.info("TABLE DATE OR USER NOT FOUND")
                logger.info("$employeeId and ${request.dateTime}")
                val message = user?.let { "Employee or not found" } ?: "Table date not found"
                saveResults(name, employeeId, dateTime, ResultType.FAILED, message)
            }
        }
        return "ok"
    }

    override fun dailyAttendance(date: Long): ResponseEntity<ByteArray> {
        val organization = extraService.getOrgFromCurrentUser()

        val excel = writeExcel(organization, date)
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline;filename=${excel.name}")
            .body(excel.readBytes())

    }

    override fun saveUnknownPerson(response: EventResponse, image: MultipartFile?) {

        if (response.accessControllerEvent != null) {
            val deviceName = response.accessControllerEvent.deviceName
            val tourniquet = tourniquetRepository.findByNameAndDeletedFalse(deviceName)

            val imageEncode = image?.let { Base64.getEncoder().encodeToString(it.bytes) }

            unknownPersonRepository.save(
                UnknownPerson(
                    tourniquet?.organization?.id,
                    tourniquet?.id,
                    deviceName,
                    imageEncode,
                    response.dateTime
                )
            )
        }


    }

    override fun getUnknownPeople(orgId: Long, pageable: Pageable): Page<UnknownPersonResponse> {
        organizationRepository.findByIdAndDeletedFalse(orgId)
            ?: throw OrganizationNotFoundException()
        return unknownPersonRepository.findAllByOrganizationId(orgId, pageable)
            .map { UnknownPersonResponse.toDto(it) }

    }

    private fun writeWorkingHoursExcel(
        startDate: Long,
        endDate: Long,
        search: String?,
        departmentId: Long?
    ): File {

        val organization = extraService.getOrgFromCurrentUser()
        val departmentsData = departmentsData(startDate, endDate, search, departmentId, organization)
        val headers = headers()
        val daysBetween = Date(startDate).daysBetween(Date(endDate))
        val lastColumn = (headers.size + daysBetween).toInt()
        var rowNum: Int

        val workbook = XSSFWorkbook()
        val centeredBoldBorderWrapStyle = centeredBoldBorderWrapStyle(workbook)
        val centeredBoldBorderStyle = centeredBoldBorderStyle(workbook)
        val centeredBorderStyle = centeredBorderStyle(workbook)

        val nameUzb = LocalDate.now().month.nameUzb()
        val sheet = workbook.createSheet(nameUzb)

        rowNum = createFirstRow(sheet, organization, lastColumn, workbook)
        rowNum = createSecondRow(sheet, lastColumn, rowNum)
        rowNum = createThirdRow(sheet, startDate, endDate, daysBetween, workbook, rowNum)
        rowNum = createHeaders(sheet, daysBetween, workbook, rowNum, departmentsData)
        rowNum++

        var number = 0
        departmentsData.forEach { response ->
            printDepartment(sheet, rowNum, daysBetween, response, centeredBoldBorderWrapStyle, headers, workbook)
            rowNum++
            response.userSummary.forEach { user ->
                val downRow = sheet.createRow(rowNum)
                val numberCell = downRow.createCell(0)
                numberCell.setCellValue((++number).toString())
                numberCell.cellStyle = centeredBoldBorderStyle

                val fullNameCell = downRow.createCell(1)
                fullNameCell.setCellValue(user.fullName)
                fullNameCell.cellStyle = centeredBoldBorderStyle

                val positionNameCell = downRow.createCell(2)
                positionNameCell.setCellValue(user.positionName)
                positionNameCell.cellStyle = centeredBorderStyle

                val requiredHoursCell = downRow.createCell(3)
                requiredHoursCell.setCellValue(requiredHours(user.requiredMinutes))
                requiredHoursCell.cellStyle = centeredBorderStyle

                val workSummaryResults = workSummaryResults(user.workSummary)
                workSummaryResults.forEachIndexed { index, workSummary ->
                    printWorkSummary(downRow, index, workSummary, centeredBorderStyle, workbook)
                }

                val presentHoursCell = downRow.createCell(4)
                presentHoursCell.setCellValue(presentHours(workSummaryResults).toString())
                presentHoursCell.cellStyle = centeredBorderStyle

                rowNum++
            }
        }

        sheet.createFreezePane(0, 7)

        val tempFile = File.createTempFile("report", ".xlsx")
        workbook.write(FileOutputStream(tempFile))
        workbook.close()
        return tempFile
    }

    private fun writeExcel(org: Organization, date: Long): File {

        val query = """
            select uc.pinfl,
                   u.full_name,
                   p.name as position_name,
                   MIN(CASE WHEN type = 'IN' THEN time END) as start_date,
                   MAX(CASE WHEN type = 'OUT' THEN time END) as end_date
            from user_tourniquet ut
                     join employee e on ut.user_id = e.user_id
                     join users u on ut.user_id = u.id
                     join user_credentials uc on uc.user_id = u.id
                     join position p on p.id = e.position_id
            where ut.organization_id = ${org.id!!}
              and DATE(time) = '${date.localDate()}'
            group by u.full_name, p.name, uc.pinfl
       """.trimIndent()

        val dailyAttendance = jdbcTemplate.query(query) { rs, _ ->
            DailyAttendanceResponse(
                pinfl = rs.getString("pinfl"),
                fullName = rs.getString("full_name"),
                positionName = rs.getString("position_name"),
                startDate = rs.getObject("start_date", LocalDateTime::class.java),
                endDate = rs.getObject("end_date", LocalDateTime::class.java),
                givenDate = date.localDate()

            )
        }

        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Daily_Attendance")
        val row = sheet.createRow(0)
        val headers = listOf(
            "Pinfl",
            "Ishchilar ismi",
            "Lavozimi",
            "Birinchi kirish vaqti",
            "Oxirgi chiqish vaqti",
            "Belgilangan sana"
        )

        for (i in headers.indices) {
            val font = workbook.createFont()
            font.bold = true
            val style = workbook.createCellStyle()
            style.setFont(font)
            val cell = row.createCell(i)
            cell.setCellValue(headers[i])
            cell.cellStyle = style
        }
        for (i in headers.indices) {
            sheet.autoSizeColumn(i, true)
        }

        val columnWidths = IntArray(6) { 0 }
        dailyAttendance.forEach { attendance ->
            val cellValues = listOf(
                attendance.pinfl,
                attendance.fullName,
                attendance.positionName,
                attendance.startDate.toString(),
                attendance.endDate.toString(),
                attendance.givenDate.toString()
            )
            cellValues.forEachIndexed { colIndex, value ->
                val currentWidth = value.length + 5
                columnWidths[colIndex] = columnWidths[colIndex].coerceAtLeast(currentWidth)
            }
        }

        val headerRow = sheet.getRow(0)
        if (headerRow != null) {
            for (cellIndex in 0 until headerRow.physicalNumberOfCells) {
                val headerCell = headerRow.getCell(cellIndex)
                val headerValue = headerCell?.stringCellValue ?: ""
                val headerWidth = headerValue.length + 5 //
                columnWidths[cellIndex] = columnWidths[cellIndex].coerceAtLeast(headerWidth)
            }
        }

        columnWidths.forEachIndexed { index, width ->
            sheet.setColumnWidth(index, width * 256)
        }

        val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
        dailyAttendance.forEachIndexed { rowIndex, attendance ->
            val dataRow = sheet.createRow(rowIndex + 1)

            val cellValues = listOf(
                attendance.pinfl,
                attendance.fullName,
                attendance.positionName,
                (attendance.startDate?.format(formatter) ?: "").toString(),
                (attendance.endDate?.format(formatter) ?: "").toString(),
                attendance.givenDate.toString()
            )
            cellValues.forEachIndexed { colIndex, value ->
                val dataCell = dataRow.createCell(colIndex)
                dataCell.setCellValue(value)
            }
        }

        val tempFile = File.createTempFile("report${date.localDate()}_", ".xlsx")
        workbook.write(FileOutputStream(tempFile))
        workbook.close()
        return tempFile
    }


    private fun saveDefaultUserTourniquet(
        user: User,
        tourniquet: Tourniquet,
        userTourniquetType: String,
        dateTime: Date,
        name: String,
        employeeId: String,
        tableDate: TableDate,
        snapshot: FileAsset?
    ) {
        logger.info("EVENTS FUNCTION STARTS")
        val lastEvent =
            repository.findTopByUserIdAndTourniquetOrganizationIdAndDeletedFalseAndTimeIsBeforeOrderByTimeDesc(
                user.id!!,
                tourniquet.organization.id!!,
                dateTime
            )
        val type =
            if (lastEvent == null || lastEvent.type == UserTourniquetType.OUT) UserTourniquetType.IN else UserTourniquetType.OUT
        val userType = TourniquetEmployeeType.valueOf(userTourniquetType.uppercase())
        val existAtInterval = existsInInterval(user, tourniquet, dateTime)

        logger.info("$type EVENT TYPE")
        if (!existAtInterval) {
            if (employeeId.isNumeric()) updateEmployeeAtOffice(type, employeeId.toLong())
            when (type) {
                UserTourniquetType.IN -> {
                    saveUserTourniquet(tourniquet, user, dateTime, type, userType, tableDate, snapshot)
                }

                UserTourniquetType.OUT -> {
                    val newEvent = saveUserTourniquet(tourniquet, user, dateTime, type, userType, tableDate, snapshot)
                    validateTimeAndSaveTracker(newEvent, lastEvent!!)
                }
            }
            saveResults(name, employeeId, dateTime, ResultType.COMPLETED, "Successfully saved")
        } else {
            logger.info("${lastEvent?.user?.fullName}")
            logger.info("${lastEvent?.tourniquet?.id}")
            logger.info("${lastEvent?.tourniquet?.name}")
        }
    }

    private fun saveNormalUserTourniquet(
        user: User,
        tourniquet: Tourniquet,
        userTourniquetType: String,
        dateTime: Date,
        name: String,
        employeeId: String,
        tableDate: TableDate,
        snapshot: FileAsset?
    ) {
        val type = UserTourniquetType.valueOf(tourniquet.type.name)
        val existAtInterval = existsInIntervalByType(user, tourniquet, type, dateTime)
        val userType = TourniquetEmployeeType.valueOf(userTourniquetType.uppercase())
        val lastEvent =
            repository.findTopByUserIdAndTourniquetOrganizationIdAndDeletedFalseAndTimeIsBeforeOrderByTimeDesc(
                user.id!!,
                tourniquet.organization.id!!,
                dateTime
            )
        if (!existAtInterval) {
            if (employeeId.isNumeric()) {
                updateEmployeeAtOffice(type, employeeId.toLong())
            }

            val today = dateTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            val isFirstEntryToday = repository.existsByUserIdAndTourniquetTypeAndDate(
                user.id!!,
                UserTourniquetType.IN,
                today
            )

            val status = if (!isFirstEntryToday && tourniquet.type == TourniquetType.IN)
                calculateArrivalStatus(dateTime, tableDate) else null

            val newEvent = saveUserTourniquet(tourniquet, user, dateTime, type, userType, tableDate, snapshot, status)
            if (tourniquet.type == TourniquetType.OUT) {
                if (lastEvent != null && lastEvent.type != UserTourniquetType.OUT) {
                    validateTimeAndSaveTracker(newEvent, lastEvent)
                }
            }
            saveResults(name, employeeId, dateTime, ResultType.COMPLETED, "Successfully saved")
        }
    }

    fun calculateArrivalStatus(dateTime: Date, tableDate: TableDate): ArrivalStatus {
        val organizationId = tableDate.organization.id!!

        val localDateTime = dateTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
        val hrmsDayOfWeek = DayOfWeek.valueOf(localDateTime.dayOfWeek.name)
        val expectedStartHour = getStartHourForOrganization(organizationId, hrmsDayOfWeek)

        return if (localDateTime.toLocalTime().isAfter(expectedStartHour)) {
            ArrivalStatus.LATE
        } else {
            ArrivalStatus.ON_TIME
        }
    }

    fun getStartHourForOrganization(organizationId: Long, day: DayOfWeek): LocalTime {
        val config = workingDateConfigRepository.findByOrganizationIdAndDayAndDeletedFalse(organizationId, day)
            ?: throw WorkingDateConfigNotFoundException()
        return config.startHour
    }

    private fun saveResults(
        tourniquetName: String,
        employeeId: String,
        dateTime: Date,
        resultType: ResultType,
        message: String
    ) = resultRepository.save(UserTourniquetResult(tourniquetName, employeeId, dateTime, resultType, message))

    private fun saveTracker(
        inTime: Date,
        outTime: Date,
        amount: Int,
        userTourniquet: UserTourniquet
    ) = trackerRepository.save(
        TourniquetTracker(
            inTime,
            outTime,
            amount,
            userTourniquet.user,
            userTourniquet.tourniquet,
            userTourniquet.tableDate
        )
    )

    private fun validateTimeAndSaveTracker(newEvent: UserTourniquet, lastEvent: UserTourniquet) {
        val newEventDate = newEvent.time
        val midnight = newEventDate.midnight()
        val lastEventDate = lastEvent.time
        if (lastEventDate < midnight) {
            saveTracker(lastEventDate, midnight, lastEventDate.minuteDuration(midnight), lastEvent)
            saveTracker(midnight, newEventDate, midnight.minuteDuration(newEventDate), newEvent)
        } else {
            saveTracker(lastEventDate, newEventDate, lastEventDate.minuteDuration(newEventDate), newEvent)
        }
    }

    private fun saveUserTourniquet(
        tourniquet: Tourniquet,
        user: User,
        dateTime: Date,
        type: UserTourniquetType,
        userType: TourniquetEmployeeType,
        tableDate: TableDate,
        fileAsset: FileAsset?,
        status: ArrivalStatus? = null
    ) = repository.save(
        UserTourniquet(
            tourniquet.organization,
            user,
            tourniquet,
            tableDate,
            dateTime,
            userType,
            type,
            fileAsset,
            status = status
        )
    )

    private fun getUserOrVisitor(employeeId: String, tourniquet: Tourniquet): User? {
        return if (employeeId.isNumeric()) {
            val employee = employeeRepository.findByIdAndPhStatusAndDeletedFalse(
                employeeId.toLong(),
                PositionHolderStatus.BUSY
            )
            if (employee?.organization?.id == tourniquet.organization.id && employee?.status == Status.ACTIVE) employee.user else null
        } else {
            val userId =
                visitorRepository.findByHashIdAndOrganizationId(employeeId, tourniquet.organization.id!!)?.userId
            userId?.let { userRepository.findByIdAndDeletedFalse(it) }
        }
    }

    private fun existsInInterval(
        user: User,
        tourniquet: Tourniquet,
        dateTime: Date,
    ): Boolean {
        return repository.existsByLastUserTourniquetAtIntervalTime(
            user.id!!,
            tourniquet.organization.id!!,
            dateTime
        )
    }

    private fun existsInIntervalByType(
        user: User,
        tourniquet: Tourniquet,
        type: UserTourniquetType,
        dateTime: Date,
    ): Boolean {
        return repository.existsByLastUserTourniquetAtIntervalTimeByType(
            user.id!!,
            tourniquet.organization.id!!,
            dateTime,
            type.name
        )
    }

    private fun updateEmployeeAtOffice(type: UserTourniquetType, employeeId: Long) {
        val employee = employeeRepository.findByIdAndDeletedFalse(employeeId)
        val atOffice = type == UserTourniquetType.IN
        employee?.let {
            it.atOffice = atOffice
            employeeRepository.save(it)
        }
    }

    private fun departmentsData(
        startDate: Long,
        endDate: Long,
        search: String?,
        departmentId: Long?, organization: Organization
    ): List<DepartmentDataResponse> {
        val searchQuery = search?.let { "'$search'" }
        val department = departmentId?.let {
            departmentRepository.findByIdAndOrganizationIdAndDeletedFalse(
                it,
                organization.id!!
            ) ?: throw DepartmentNotFoundException()
        }

        val query = """
        with result as (select u.id                                    as id,
                               u.full_name                             as full_name,
                               td.date                                 as work_date,
                               td.type                                 as day_type,
                               coalesce((select sum(tr.amount)
                                         from tourniquet_tracker tr
                                         where tr.user_id = u.id
                                           and tr.table_date_id = td.id
                                           and tr.deleted = false), 0) as work_minutes,
                               td.id                                   as table_date_id,
                               d.name                                  as department_name,
                               p.name                                  as position_name,
                               coalesce(jsonb_agg(jsonb_build_object(
                                       'id', ut.id,
                                       'type', ut.type,
                                       'time', ut.time at time zone 'Asia/Tashkent'
                                                  ) order by ut.time
                                                 )
                                        filter ( where ut.id is not null ), '[]')
                                                                       as tourniquet_data
                        from organization o
                                 join table_date td on td.organization_id = o.id
                                 join department d on d.organization_id = o.id
                                 join user_employment_history ueh on d.id = ueh.department_id
                                 join position p on ueh.position_id = p.id
                                 join users u on ueh.user_id = u.id
                                 left join user_tourniquet ut on u.id = ut.user_id and ut.table_date_id = td.id
                        where u.deleted = false
                          and td.deleted = false
                          and o.id = ${organization.id}
                          and (${department?.id} is null or ${department?.id} = d.id)
                          and (${searchQuery} is null or u.full_name ilike concat('%', ${searchQuery}, '%'))
                          and ((hired_date between '${startDate.timeWithUTC()}' and '${endDate.timeWithUTC()}')
                            or ('${startDate.timeWithUTC()}' between ueh.hired_date and coalesce(ueh.dismissed_date,
                                                                                               current_timestamp at time zone
                                                                                               'Asia/Tashkent'))
                            or ('${endDate.timeWithUTC()}' between ueh.hired_date and coalesce(ueh.dismissed_date,
                                                                                             current_timestamp at time zone
                                                                                             'Asia/Tashkent')))
                          and td.date between '${startDate.timeWithUTC()}' and '${endDate.timeWithUTC()}'
                          and td.date between ueh.hired_date and coalesce(ueh.dismissed_date,
                                                                          current_timestamp at time zone
                                                                          'Asia/Tashkent')
                        group by u.id, td.id, d.id, p.id
                        order by u.id, td.date),
             department_result as (select r.id                as id,
                                          r.full_name         as full_name,
                                          r.department_name   as department_name,
                                          r.position_name     as position_name,
                                          sum(case
                                                  when day_type = 'WORK_DAY' and wdc.id is not null
                                                      then wdc.required_minutes
                                                  else 0 end) as required_minutes,
                                          json_agg(
                                                  json_build_object(
                                                          'workingDate', cast(extract(epoch from work_date) * 1000 as bigint),
                                                          'dayType', day_type,
                                                          'workingMinutes', work_minutes,
                                                          'tableDateId', table_date_id,
                                                          'tourniquetData', r.tourniquet_data
                                                  ) order by work_date
                                          )                   as work_summary
                                   from result r
                                            left join working_date_config wdc
                                                      on ${organization.id} = wdc.organization_id and
                                                         wdc.day = trim(to_char(r.work_date, 'DAY')) and wdc.deleted = false
                                   group by r.id, r.full_name, r.department_name, r.position_name
                                   order by full_name)
        select department_name,
               jsonb_agg(
                       jsonb_build_object(
                               'id', id,
                               'fullName', full_name,
                               'requiredMinutes', required_minutes,
                               'positionName', position_name,
                               'workSummary', work_summary
                       )
               ) as userSummary
        from department_result
        group by department_name
        """.trimIndent()

        return jdbcTemplate.query(query) { rs, _ ->
            val workSummaryJson = rs.getString("userSummary")
            DepartmentDataResponse(
                rs.getString("department_name"),
                objectMapper.readValue<List<ExcelWorkingMinutesResponse>>(
                    workSummaryJson
                )
            )
        }
    }

    private fun centeredStyle(workbook: XSSFWorkbook): XSSFCellStyle {
        val cellStyle = workbook.createCellStyle()
        cellStyle.alignment = HorizontalAlignment.CENTER
        cellStyle.verticalAlignment = VerticalAlignment.CENTER
        return cellStyle
    }

    private fun centeredWrapStyle(workbook: XSSFWorkbook): XSSFCellStyle {
        val cellStyle = workbook.createCellStyle()
        cellStyle.alignment = HorizontalAlignment.CENTER
        cellStyle.verticalAlignment = VerticalAlignment.CENTER
        cellStyle.wrapText = true
        return cellStyle
    }

    private fun centeredBoldBorderWrapStyle(workbook: XSSFWorkbook): XSSFCellStyle {
        val font = workbook.createFont().apply { bold = true }
        val borderStyle = centeredBorderStyle(workbook)
        borderStyle.apply {
            wrapText = true
            setFont(font)
        }
        return borderStyle
    }

    private fun centeredBoldBorderStyle(workbook: XSSFWorkbook): XSSFCellStyle {
        val font = workbook.createFont().apply { bold = true }
        val borderStyle = centeredBorderStyle(workbook)
        borderStyle.apply {
            setFont(font)
        }
        return borderStyle
    }

    private fun centeredBorderStyle(workbook: XSSFWorkbook): XSSFCellStyle {
        val cellStyle = workbook.createCellStyle()
        cellStyle.apply {
            alignment = HorizontalAlignment.CENTER
            verticalAlignment = VerticalAlignment.CENTER
            borderTop = BorderStyle.THIN
            borderBottom = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
            topBorderColor = IndexedColors.BLACK.index
            bottomBorderColor = IndexedColors.BLACK.index
            leftBorderColor = IndexedColors.BLACK.index
            rightBorderColor = IndexedColors.BLACK.index
        }
        return cellStyle
    }

    private fun centeredRightBorderStyle(workbook: XSSFWorkbook): XSSFCellStyle {
        val cellStyle = workbook.createCellStyle()
        cellStyle.apply {
            alignment = HorizontalAlignment.CENTER
            verticalAlignment = VerticalAlignment.CENTER
            borderTop = BorderStyle.THIN
            borderBottom = BorderStyle.THIN
            borderRight = BorderStyle.THIN
            topBorderColor = IndexedColors.BLACK.index
            bottomBorderColor = IndexedColors.BLACK.index
            rightBorderColor = IndexedColors.BLACK.index
        }
        return cellStyle
    }

    private fun centeredBorderGreyStyle(workbook: XSSFWorkbook): XSSFCellStyle {
        val cellStyle = centeredBorderStyle(workbook)
        cellStyle.fillForegroundColor = IndexedColors.GREY_40_PERCENT.index
        cellStyle.fillPattern = FillPatternType.SOLID_FOREGROUND
        return cellStyle
    }

    private fun centeredBorderYellowStyle(workbook: XSSFWorkbook): XSSFCellStyle {
        val cellStyle = centeredBorderStyle(workbook)
        cellStyle.fillForegroundColor = IndexedColors.LIGHT_YELLOW.index
        cellStyle.fillPattern = FillPatternType.SOLID_FOREGROUND
        return cellStyle
    }

    private fun boldStyle(workbook: XSSFWorkbook): XSSFCellStyle {
        val font = workbook.createFont().apply { bold = true }
        val cellStyle = workbook.createCellStyle()
        cellStyle.setFont(font)
        return cellStyle
    }

    private fun headers(): MutableList<String> {
        return mutableListOf(
            "Nomer",
            "F.I.SH",
            "Lavozimi",
            "Talab qilingan vaqt soati",
            "Offisda bo'lgan vaqt soati"
        )
    }

    private fun createFirstRow(sheet: Sheet, organization: Organization, lastColumn: Int, workbook: XSSFWorkbook): Int {
        val firstRow = sheet.createRow(0)
        val cell = firstRow.createCell(lastColumn - 4)
        cell.setCellValue(organization.name)
        cell.cellStyle = centeredWrapStyle(workbook)
        firstRow.heightInPoints = 100f
        sheet.addMergedRegion(
            CellRangeAddress(firstRow.rowNum, firstRow.rowNum, lastColumn - 4, lastColumn)
        )
        return firstRow.rowNum
    }

    private fun createSecondRow(sheet: Sheet, lastColumn: Int, rowNumber: Int): Int {
        val secondRow = sheet.createRow(rowNumber + 1)
        secondRow.createCell(lastColumn - 3).setCellValue(
            "_________________.__________________"
        )
        return secondRow.rowNum
    }

    private fun printWorkSummary(
        downRow: XSSFRow,
        i: Int,
        workSummary: ExcelDataResponse,
        centeredBorderStyle: XSSFCellStyle,
        workbook: XSSFWorkbook
    ) {
        val finalResult = finalResult(workSummary)
        val summaryCell = downRow.createCell(i + 5)
        summaryCell.setCellValue(finalResult)
        summaryCell.cellStyle = when (workSummary.dayType) {
            TableDateType.WORK_DAY -> centeredBorderStyle
            TableDateType.HOLIDAY -> centeredBorderYellowStyle(workbook)
            TableDateType.REST_DAY -> centeredBorderGreyStyle(workbook)
            null -> centeredBorderStyle
        }
    }

    private fun printDepartment(
        sheet: Sheet,
        rowNum: Int,
        daysBetween: Long,
        response: DepartmentDataResponse,
        centeredBoldBorderWrapStyle: XSSFCellStyle,
        headers: MutableList<String>,
        workbook: XSSFWorkbook
    ) {
        val row = sheet.createRow(rowNum)

        val departmentCell = row.createCell(0)
        departmentCell.setCellValue(response.departmentName)
        departmentCell.cellStyle = centeredBoldBorderWrapStyle

        for (i in 1..headers.size + daysBetween) {
            row.createCell(i.toInt()).cellStyle = centeredBorderStyle(workbook)
        }
        sheet.addMergedRegion(CellRangeAddress(rowNum, rowNum, 0, (headers.size + daysBetween).toInt()))

        for (i in 0..(headers.size + daysBetween).toInt()) {
            row.getCell(i)?.cellStyle = centeredBoldBorderWrapStyle
        }
    }


    private fun createThirdRow(
        sheet: Sheet,
        startDate: Long,
        endDate: Long,
        daysBetween: Long,
        workbook: XSSFWorkbook,
        rowNumber: Int
    ): Int {

        validationService.validateDateRange(startDate, endDate)

        val thirdRow = sheet.createRow(rowNumber + 2)
        val thirdRowCell = thirdRow.createCell(0)
        thirdRowCell.setCellValue(
            "${startDate.yearWithUTC()}-yil ${startDate.dayWithUTC()}-${startDate.monthNameWithUTC()}dan " +
                    " ${endDate.dayWithUTC()}-${endDate.monthNameWithUTC()}gacha"
        )
        thirdRowCell.cellStyle = centeredWrapStyle(workbook)
        sheet.addMergedRegion(
            CellRangeAddress(thirdRow.rowNum, thirdRow.rowNum, 0, (headers().size + daysBetween).toInt())
        )

        return thirdRow.rowNum
    }

    private fun createHeaders(
        sheet: Sheet,
        daysBetween: Long,
        workbook: XSSFWorkbook,
        rowNumber: Int,
        departmentsData: List<DepartmentDataResponse>
    ): Int {
        val headersRow = sheet.createRow(rowNumber + 1)
        val middleRow = sheet.createRow(rowNumber + 2)
        val lowerRow = sheet.createRow(rowNumber + 3)
        val centeredBoldBorderWrapStyle = centeredBoldBorderWrapStyle(workbook)
        val headers = headers()

        val longestFullNameLength = departmentsData.flatMap { it.userSummary }
            .maxByOrNull { it.fullName.length }?.fullName?.length ?: 0
        val fullNameWidth = (longestFullNameLength + 2) * 256

        val longestPositionLength = departmentsData.flatMap { it.userSummary }
            .maxByOrNull { it.positionName.length }?.positionName?.length ?: 0
        val positionWith = (longestPositionLength + 2) * 256

        for (i in 0 until headers.size) {
            val headerCell = headersRow.createCell(i)
            headerCell.setCellValue(headers[i])
            headerCell.cellStyle = centeredBoldBorderWrapStyle
            middleRow.createCell(i).cellStyle = centeredBoldBorderWrapStyle
            lowerRow.createCell(i).cellStyle = centeredBoldBorderWrapStyle

            sheet.addMergedRegion(
                CellRangeAddress(headersRow.rowNum, lowerRow.rowNum, i, i)
            )
        }
        if (longestFullNameLength != 0) {
            sheet.setColumnWidth(1, fullNameWidth)
        }
        if (longestPositionLength != 0) {
            sheet.setColumnWidth(2, positionWith)


            for (day in 0..daysBetween) {
                val index = (headers.size + day).toInt()
                val headerCell = lowerRow.createCell(index)
                headerCell.setCellValue((day + 1).toString())
                headerCell.cellStyle = centeredBoldBorderWrapStyle
                middleRow.createCell(index).cellStyle = centeredBoldBorderWrapStyle
                headersRow.createCell(index).cellStyle = centeredBoldBorderWrapStyle
            }

            sheet.addMergedRegion(
                CellRangeAddress(
                    headersRow.rowNum,
                    middleRow.rowNum,
                    headers.size,
                    (headers.size + daysBetween).toInt()
                )
            )
        }

        return lowerRow.rowNum
    }


    private fun finalResult(workSummary: ExcelDataResponse): String {
        return workSummary.workingMinutes?.let {
            val hours = it / 60
            val minutes = it % 60
            val result = StringBuilder()
            if (hours > 0 || minutes > 0) {
                if (hours > 0) result.append("$hours s ")
                if (minutes > 0) result.append("$minutes m")
            } else {
                when (workSummary.dayType) {
                    TableDateType.REST_DAY -> result.append("D")
                    TableDateType.HOLIDAY -> result.append("B")
                    else -> result.append("K")
                }
            }
            result.toString().trim()
        } ?: ""
    }

    private fun workSummaryResults(workSummary: List<ExcelDataResponse>): MutableList<ExcelDataResponse> {
        val dates = dates(workSummary.first().workingDate)
        val summaryMap = workSummary.associateBy { it.workingDate.midnight() }
        val userSummary = mutableListOf<ExcelDataResponse>()
        dates.forEach { date ->
            userSummary.add(
                summaryMap[date]?.let { summary ->
                    summary(summary)
                } ?: run {
                    ExcelDataResponse(
                        date
                    )
                }
            )
        }
        return userSummary
    }

    private fun dates(date: Date): MutableList<Date> {
        val startOfMonth = date.startOfMonth()
        val dates = mutableListOf<Date>()
        val calendar = Calendar.getInstance()

        calendar.time = startOfMonth
        val lastDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        for (i in 1..lastDay) {
            calendar.set(Calendar.DAY_OF_MONTH, i)
            dates.add(calendar.time)
        }
        return dates
    }

    private fun summary(summary: ExcelDataResponse): ExcelDataResponse {
        val data = summary.tourniquetData
        val size = data.size.minus(1)
        var minutes = 0
        for (i in 0 until size) {
            if (data[i].type == UserTourniquetType.OUT && data[i + 1].type == UserTourniquetType.IN
                && isCorrectTime(data[i].time, data[i + 1].time)
            ) {
                val outDate = data[i].time
                val inDate = data[i + 1].time

                val elevenAM = outDate.elevenAM()
                val threePM = inDate.threePM()

                val outTime = if (outDate < elevenAM) elevenAM else outDate
                val inTime = if (inDate > threePM) threePM else inDate

                minutes += (outTime.minuteDuration(inTime))
            }
        }

        val amount = if (minutes < 60 && summary.workingMinutes!! > 60) {
            summary.workingMinutes?.minus(60 - minutes)
        } else {
            summary.workingMinutes
        }
        return ExcelDataResponse(
            summary.workingDate,
            summary.tableDateId,
            amount,
            summary.dayType
        )
    }

    private fun presentHours(summaryList: MutableList<ExcelDataResponse>): Int {
        var totalMinutes = 0
        summaryList.forEach { user ->
            user.workingMinutes?.let {
                totalMinutes += it
            }
        }
        return totalMinutes / 60
    }

    private fun isCorrectTime(outDate: Date, intDate: Date): Boolean {
        val elevenAM = outDate.elevenAM()
        val threePM = intDate.threePM()
        if (outDate >= elevenAM && intDate <= threePM)
            return true
        if (outDate < elevenAM && intDate > threePM)
            return true
        if (intDate > threePM && intDate < threePM)
            return true

        return false
    }

    private fun requiredHours(minutes: Int): String {
        val hours = minutes / 60.0
        return "%.1f hours".format(hours)
    }

    private fun activeEmployeeInfo(
        organization: Organization,
        startDate: Long,
        endDate: Long,
        searchQuery: String?,
        departmentId: Long?,
        pageable: Pageable
    ): PageImpl<WorkingMinutesResponse> {

        val countQuery = """
            select count(u.id)
            from organization o
                     join (select e.user_id         as user_id,
                                  e.organization_id as organization_id,
                                  e.department_id   as department_id
                           from employee e
                           where e.status = 'ACTIVE'
                             and e.deleted = false
                             and e.ph_status = 'BUSY') e on e.organization_id = o.id
                     join users u on e.user_id = u.id
            where o.id = ${organization.id}
              and u.deleted = false
              and (${departmentId} is null or $departmentId = e.department_id)
              and (${searchQuery} is null or u.full_name ilike concat('%', ${searchQuery}, '%'))
        """.trimIndent()

        val count = jdbcTemplate.queryForObject(countQuery, Long::class.java)!!

        val query = """
            with result as (select u.id                                               as id,
                                   u.full_name                                        as full_name,
                                   cast(extract(epoch from td.date) * 1000 as bigint) as work_date,
                                   td.type                                            as day_type,
                                   coalesce(sum(worked_time.work_minutes), 0)         as work_minutes,
                                   coalesce(sum(user_events.event_amount), 0)         as event_amount,
                                   td.id                                              as table_date_id,
                                   uat.id                                             as user_absence_tracker_id
                            from organization o
                                     join table_date td on td.organization_id = o.id
                                     join (select e.user_id         as user_id,
                                                  e.organization_id as organization_id,
                                                  e.department_id   as department_id
                                           from employee e
                                           where e.status = 'ACTIVE'
                                             and e.deleted = false
                                             and e.ph_status = 'BUSY') e on e.organization_id = o.id
                                     join users u on e.user_id = u.id
                                     left join (select ut.user_id       as user_id,
                                                       ut.table_date_id as table_date_id,
                                                       count(ut.id)     as event_amount
                                                from user_tourniquet ut
                                                group by ut.user_id, ut.table_date_id) user_events
                                               on user_events.user_id = u.id and user_events.table_date_id = td.id
                                     left join (select tr.user_id       as user_id,
                                                       tr.table_date_id as table_date_id,
                                                       sum(tr.amount)   as work_minutes
                                                from tourniquet_tracker tr
                                                where tr.deleted = false
                                                group by tr.user_id, tr.table_date_id) worked_time
                                               on worked_time.user_id = u.id and worked_time.table_date_id = td.id
                                                left join (select u.id,
                                           u.user_id,
                                           u.table_date_id
                                    from user_absence_tracker u
                                    join table_date t on u.table_date_id = t.id
                                    join users us on u.user_id = us.id
                                    where u.deleted = false
                                    and u.table_date_id = t.id
                                    and u.user_id = us.id group by u.id) uat on u.id = uat.user_id and td.id = uat.table_date_id      
                            where o.id = ${organization.id}
                              and td.deleted = false
                              and u.deleted = false
                              and (${departmentId} is null or $departmentId = e.department_id)
                              and (${searchQuery} is null or u.full_name ilike concat('%', ${searchQuery}, '%'))
                              and td.date between '${startDate.timeWithUTC()}' and '${endDate.timeWithUTC()}'
                            group by u.id, td.id, uat.id)

            select id        as id,
                   full_name as full_name,
                   json_agg(
                           json_build_object(
                                   'workingDate', work_date,
                                   'dayType', day_type,
                                   'workingMinutes', work_minutes,
                                   'eventAmount', event_amount,
                                   'tableDateId', table_date_id,
                                   'userAbsenceTrackerId', user_absence_tracker_id
                           ) order by work_date
                   )         as work_summary
            from result
            group by id, full_name
            order by full_name
            limit ${pageable.pageSize} offset ${pageable.offset}
        """.trimIndent()

        val employeeWorkingList = jdbcTemplate.query(query) { rs, _ ->
            WorkingMinutesResponse(
                rs.getLong("id"),
                rs.getString("full_name"),
                objectMapper.readValue<MutableList<DataResponse>>(
                    rs.getString("work_summary")
                ).map {
                    DataResponse(
                        it.workingDate,
                        it.tableDateId,
                        it.workingMinutes,
                        it.eventAmount,
                        it.dayType,
                        it.userAbsenceTrackerId
                    )
                }
            )
        }
        return PageImpl(employeeWorkingList, pageable, count)
    }

    private fun allEmployeeInfo(
        organization: Organization,
        startDate: Long,
        endDate: Long,
        searchQuery: String?,
        departmentId: Long?,
        pageable: Pageable
    ): PageImpl<WorkingMinutesResponse> {
        val countQuery = """
            select count(distinct u.id)
            from organization o
                     join department d on d.organization_id = o.id
                     join user_employment_history ueh on d.id = ueh.department_id
                     join users u on ueh.user_id = u.id
                     join table_date td on td.organization_id = o.id
            where o.id = ${organization.id}
              and u.deleted = false
              and ($departmentId is null or $departmentId = d.id)
              and (${searchQuery} is null or u.full_name ilike concat('%', ${searchQuery}, '%'))
              and ((ueh.hired_date between '${startDate.timeWithUTC()}' and '${endDate.timeWithUTC()}')
                              or ('${startDate.timeWithUTC()}' between ueh.hired_date and coalesce(ueh.dismissed_date,
                                                                                         current_timestamp at time zone
                                                                                         'Asia/Tashkent'))
                              or ('${endDate.timeWithUTC()}' between ueh.hired_date and coalesce(ueh.dismissed_date,
                                                                                       current_timestamp at time zone
                                                                                       'Asia/Tashkent')))
                            and td.date between '${startDate.timeWithUTC()}' and '${endDate.timeWithUTC()}'
        """.trimIndent()

        val count = jdbcTemplate.queryForObject(countQuery, Long::class.java)!!
        val query = """         
            with unique_user_dates as (select u.id        as id,
                                              u.full_name as full_name,
                                              td.date     as work_date,
                                              td.type     as day_type,
                                              td.id       as table_date_id
                                       from organization o
                                                join table_date td on td.organization_id = o.id
                                                join department d on d.organization_id = o.id
                                                join user_employment_history ueh on d.id = ueh.department_id
                                                join users u on ueh.user_id = u.id
                                       where u.deleted = false
                                         and td.deleted = false
                                         and o.id = ${organization.id}
                                         and (${departmentId} is null or $departmentId = d.id)
                                         and (${searchQuery} is null or u.full_name ilike concat('%', ${searchQuery}, '%'))
                                         and ((ueh.hired_date between '${startDate.timeWithUTC()}' and '${endDate.timeWithUTC()}')
                                           or ('${startDate.timeWithUTC()}' between ueh.hired_date and coalesce(ueh.dismissed_date,
                                                                                                              current_timestamp at time zone
                                                                                                              'Asia/Tashkent'))
                                           or ('${endDate.timeWithUTC()}' between ueh.hired_date and coalesce(ueh.dismissed_date,
                                                                                                            current_timestamp at time zone
                                                                                                            'Asia/Tashkent')))
                                         and td.date between '${startDate.timeWithUTC()}' and '${endDate.timeWithUTC()}'
                                       group by u.id, td.id),
            
                 work_summary AS (SELECT ud.id                                                   AS id,
                                         ud.full_name                                            AS full_name,
                                         cast(extract(epoch from ud.work_date) * 1000 as bigint) as work_date,
                                         ud.day_type                                             as day_type,
                                         ud.table_date_id                                        as table_date_id,
                                         coalesce(sum(user_events.event_amount), 0)              as event_amount,
                                         coalesce(sum(worked_time.work_minutes), 0)              as work_minutes,
                                         uat.id                                                  as user_absence_tracker_id
                                  from unique_user_dates ud
                                           left join (select tr.user_id       as user_id,
                                                             tr.table_date_id as table_date_id,
                                                             sum(tr.amount)   as work_minutes
                                                      from tourniquet_tracker tr
                                                      where deleted = false
                                                      group by user_id, table_date_id) worked_time
                                                     on worked_time.user_id = ud.id and worked_time.table_date_id = ud.table_date_id
                                           left join (select ut.user_id       as user_id,
                                                             ut.table_date_id as table_date_id,
                                                             count(ut.id)     as event_amount
                                                      from user_tourniquet ut
                                                      group by ut.user_id, ut.table_date_id) user_events
                                                     on user_events.user_id = ud.id and user_events.table_date_id = ud.table_date_id
                                                     left join (select u.id,
                                           u.user_id,
                                           u.table_date_id
                                    from user_absence_tracker u
                                    join table_date t on u.table_date_id = t.id
                                    join users us on u.user_id = us.id
                                    where u.deleted = false
                                    and u.table_date_id = t.id
                                    and u.user_id = us.id group by u.id) uat on ud.id = uat.user_id and ud.table_date_id = uat.table_date_id          
                                  group by ud.id, ud.full_name, ud.work_date, ud.day_type, ud.table_date_id,uat.id)
            
            select id        as id,
                   full_name as full_name,
                   json_agg(
                           json_build_object(
                                   'workingDate', work_date,
                                   'dayType', day_type,
                                   'workingMinutes', work_minutes,
                                   'eventAmount', event_amount,
                                   'tableDateId', table_date_id,
                                   'userAbsenceTrackerId',  user_absence_tracker_id
                           ) order by work_date
                   )         as work_summary
            from work_summary
            group by id, full_name
            order by full_name
            limit ${pageable.pageSize} offset ${pageable.offset}
      """.trimIndent()

        val employeeWorkingList = jdbcTemplate.query(query) { rs, _ ->
            WorkingMinutesResponse(
                rs.getLong("id"),
                rs.getString("full_name"),
                objectMapper.readValue<MutableList<DataResponse>>(
                    rs.getString("work_summary")
                ).map {
                    DataResponse(
                        it.workingDate,
                        it.tableDateId,
                        it.workingMinutes,
                        it.eventAmount,
                        it.dayType,
                        it.userAbsenceTrackerId
                    )
                }
            )
        }
        return PageImpl(employeeWorkingList, pageable, count)
    }
}


