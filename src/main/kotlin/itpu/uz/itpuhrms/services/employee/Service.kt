package itpu.uz.itpuhrms.services.employee

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import itpu.uz.itpuhrms.*
import itpu.uz.itpuhrms.base.BaseMessage
import itpu.uz.itpuhrms.security.userId
import itpu.uz.itpuhrms.services.ExtraService
import itpu.uz.itpuhrms.services.department.DepartmentRepository
import itpu.uz.itpuhrms.services.department.Structure
import itpu.uz.itpuhrms.services.employmentHistory.UserEmploymentHistoryRepository
import itpu.uz.itpuhrms.services.file.FileAssetRepository
import itpu.uz.itpuhrms.services.organization.OrganizationRepository
import itpu.uz.itpuhrms.services.permission.PermissionRepository
import itpu.uz.itpuhrms.services.position.PositionRepository
import itpu.uz.itpuhrms.services.project.ProjectRepository
import itpu.uz.itpuhrms.services.user.UserCredentialsRepository
import itpu.uz.itpuhrms.services.user.UserRepository
import itpu.uz.itpuhrms.services.userOrgStore.UserOrgStoreRepository
import itpu.uz.itpuhrms.services.validation.ValidationService
import itpu.uz.itpuhrms.services.workingDate.WorkingDateConfigRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.ResultSetExtractor
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.YearMonth
import java.util.*

interface EmployeeService {
    fun create(request: EmployeeRequest): EmployeeDto
    fun createVacantEmployee(request: EmployeeVacantRequest): EmployeeDto
    fun delete(id: Long)
    fun update(id: Long, request: EmployeeUpdateRequest): EmployeeDto
    fun changeStatus(id: Long, status: Status): BaseMessage
    fun getOneById(id: Long): EmployeesResponse
    fun getAll(
        status: PositionHolderStatus?,
        search: String?,
        departmentId: Long?,
        positionId: Long?,
        attOffice: Boolean?,
        structureTree: Boolean?,
        pageable: Pageable
    ): EmployeeContentResponse

    fun getAllForProject(
        projectId: Long,
        search: String?,
        pageable: Pageable
    ): Page<EmployeeResponse>

    fun getEmployeesByBirthday(type: BirthdayType, pageable: Pageable): Page<EmployeeBirthdateResponse>
    fun getOrganizationLateEmployees(date: Long, pageable: Pageable): Page<LateEmployeeResponse>
    fun getOrganizationAbsentEmployees(date: Long, pageable: Pageable): Page<EmployeesResponse>
    fun getEmployeeAgeStatistics(): EmployeeAgeStatisticResponse
    fun getUserWorkingDaysStatistics(
        userId: Long,
        startDate: Long,
        endDate: Long
    ): UserWorkingStatisticResponse

    fun getEmployeesAmount(): AmountEmployeesResponse
    fun getLaborActivity(): LaborActivityResponse
    fun getInOfficeEmployees(pageable: Pageable): Page<EmployeesResponseDto>
    fun getOutOfficeEmployees(pageable: Pageable): Page<EmployeesResponseDto>
    fun getEmployeesKPI(
        startDate: Long,
        endDate: Long,
        search: String?,
        departmentId: Long?,
        active: Boolean,
        pageable: Pageable
    ): Page<EmployeeKPIResponse>

    //console
    fun getAllEmployee(orgId: Long, pageable: Pageable): Page<EmployeeAdminResponse>
    fun getEmployeesStatistics(startDate: Long, endDate: Long, pageable: Pageable): Page<EmployeeStatisticsResponse>
    fun getEmployeesAttendance(
        date: Long,
        pageable: Pageable,
        status: AttendanceStatus?): Page<EmployeeAttendanceResponse>?
    fun getEmployeeKPI(
        employeeId: Long,
        startDate: Long,
        endDate: Long
    ): List<KPIDataResponse>?
}


@Service
class EmployeeServiceImpl(
    private val repository: EmployeeRepository,
    private val userRepository: UserRepository,
    private val departmentRepository: DepartmentRepository,
    private val positionRepository: PositionRepository,
    private val permissionRepository: PermissionRepository,
    private val validationService: ValidationService,
    private val workingDateConfigRepository: WorkingDateConfigRepository,
    private val fileAssetRepository: FileAssetRepository,
    private val historyRepository: UserEmploymentHistoryRepository,
    private val userOrgStoreRepository: UserOrgStoreRepository,
    private val jdbcTemplate: JdbcTemplate,
    private val userCredentialsRepository: UserCredentialsRepository,
    private val extraService: ExtraService,
    private val objectMapper: ObjectMapper,
    private val projectRepository: ProjectRepository,
    private val organizationRepository: OrganizationRepository
) : EmployeeService {

    @Transactional
    override fun create(request: EmployeeRequest): EmployeeDto {
        val user = userRepository.findByIdAndDeletedFalse(request.userId)
            ?: throw UserNotFoundException()
        val department = departmentRepository.findByIdAndDeletedFalse(request.departmentId)
            ?: throw DepartmentNotFoundException()
        val position = positionRepository.findByIdAndDeletedFalse(request.positionId)
            ?: throw PositionNotFoundException()
        val file = request.imageHashId?.let {
            fileAssetRepository.findByHashIdAndDeletedFalse(request.imageHashId)
                ?: throw FileNotFoundException()
        }

        val organization = extraService.getOrgFromCurrentUser()

        validationService.validateDifferentOrganizations(department, position, organization)
        validationService.validateExistingEmployee(user, organization)
        validationService.validateUserCredentials(user)
        file?.let {
            //validationService.validateEmployeeImage(it)
            validationService.validateImageTypeAndSize(it)
        }

        val ids = position.permission.map { it.id!! }.toMutableSet()
        val permissions = permissionRepository.findAllByIdInAndDeletedFalse(ids)
        itpu.uz.itpuhrms.security.logger.info { "Permissions: $permissions" }
        val save = repository.save(
            Employee(
                user,
                request.code,
                request.status,
                false,
                position,
                department,
                organization,
                PositionHolderStatus.BUSY,
                request.workRate,
                request.laborRate,
                permissions,
                file
            )
        )
        historyRepository.save(
            UserEmploymentHistory(
                save,
                user,
                position,
                department,
                Date()
            )
        )


        val exists = userOrgStoreRepository.existsByUserIdAndOrganizationIdAndDeletedFalse(user.id!!, organization.id!!)
        if (!exists) {
            //if (user.role == Role.ORG_ADMIN) throw UserOrgStoreNotFoundException()
            userOrgStoreRepository.save(UserOrgStore(user, organization, Role.USER, false))
        }
        return EmployeeDto.toDto(save)
    }


    override fun getEmployeesAmount(): AmountEmployeesResponse {
        val organization = extraService.getOrgFromCurrentUser()
        val query = """
              select
                  coalesce(SUM(case when e.at_office = true then 1 else 0 end ),0) as employees_at_office,
                  coalesce(SUM(case when e.at_office = false then 1 else 0 end ),0) as employees_not_at_office
              from employee e
                join organization o on e.organization_id = o.id
              where e.deleted = false and e.ph_status = 'BUSY'
               and e.status = 'ACTIVE'
               and o.id = ${organization.id!!}
        """.trimIndent()

        val list = jdbcTemplate.query(query) { rs, _ ->
            AmountEmployeesResponse(
                rs.getLong("employees_at_office"),
                rs.getLong("employees_not_at_office")
            )
        }
        return if (list.isEmpty()) {
            AmountEmployeesResponse(0, 0)
        } else {
            list[0]
        }
    }

    override fun getLaborActivity(): LaborActivityResponse {
        val employee = extraService.getEmployeeFromCurrentUser()
        val organization = employee.organization
        val user = employee.user!!

        var firstHiredDate = 0L
        var currentHiredDate = 0L
        var lastFiredDate = 0L

        val query = """
            WITH current_employment AS (
                SELECT eh.hired_date AS current_hire_date, eh.dismissed_date AS current_dismissal_date
                FROM user_employment_history eh
                         JOIN employee e ON e.id = eh.employee_id
                WHERE e.user_id = ${user.id}
                AND e.organization_id = ${organization.id!!}
                AND eh.dismissed_date IS NULL
            )
            SELECT
                 (
                    SELECT MIN(eh.hired_date)
                    FROM user_employment_history eh
                             JOIN employee e ON e.id = eh.employee_id
                    WHERE e.user_id = ${user.id}
                    AND e.organization_id = ${organization.id!!}
                 ) AS first_hired_date,
                 c.current_hire_date AS current_hired_date,
                 c.current_dismissal_date AS last_fired_date
            FROM current_employment c;
            """.trimIndent()

        jdbcTemplate.query(query) { rs, _ ->
            firstHiredDate = rs.getTimestamp("first_hired_date").time
            currentHiredDate = rs.getTimestamp("current_hired_date").time
            lastFiredDate = rs.getTimestamp("last_fired_date")?.time ?: Date().time
        }
        return LaborActivityResponse(firstHiredDate, currentHiredDate, lastFiredDate)
    }

    override fun getInOfficeEmployees(pageable: Pageable): Page<EmployeesResponseDto> {
        val organization = extraService.getOrgFromCurrentUser()

        val query = """
        select e.id                  as employee_id,
               u.id                  as user_id,
               u.full_name           as name,
               fa.hash_id            as hash_id,
               e.at_office           as at_office,
               e.labor_rate          as labor_rate,
               e.work_rate           as work_rate,
               uc.gender             as gender,
               uc.pinfl              as pinfl,
               uc.card_serial_number as card_serial_number,
               u.mail                as mail,
               u.role                as roles,
               u.phone_number        as phone_number,
               u.username            as username,
               u.status              as status,
               e.ph_status           as ph_status,
               p.id                  as position_id,
               p.name                as position_name,
               d.id                  as department_id,
               d.name                as department_name,
               d.description         as department_description,
               d.department_type     as department_type
        from employee e
                 join users u on e.user_id = u.id
                 left join file_asset fa on e.image_asset_id = fa.id
                 join user_credentials uc on u.id = uc.user_id
                 join position p on e.position_id = p.id
                 join department d on e.department_id = d.id
        where e.deleted = false
          and e.ph_status = 'BUSY'
          and e.organization_id = ${organization.id!!}
          and e.at_office = true
        limit ${pageable.pageSize} offset ${pageable.offset}
        """.trimIndent()

        val totalElementsQuery = """
       select count(distinct e.id)
       from employee e
           join users u on e.user_id = u.id
    where e.deleted = false and e.ph_status = 'BUSY'
    and e.organization_id= ${organization.id!!} and  e.at_office=true
    """.trimIndent()

        val totalElements = jdbcTemplate.queryForObject(totalElementsQuery, Int::class.java)!!

        val employees = jdbcTemplate.query(query) { rs, _ ->
            EmployeesResponseDto(
                id = rs.getLong("employee_id"),
                atOffice = rs.getBoolean("at_office"),
                status = Status.valueOf(rs.getString("status")),
                laborRate = rs.getShort("labor_rate"),
                workRate = rs.getDouble("work_rate"),
                phStatus = PositionHolderStatus.valueOf(rs.getString("ph_status")),
                imageHashId = rs.getString("hash_id"),
                user = EmployeesResponseDto.UserResponseDto(
                    id = rs.getLong("user_id"),
                    fullName = rs.getString("name"),
                    mail = rs.getString("mail"),
                    role = Role.valueOf(rs.getString("roles")),
                    phoneNumber = rs.getString("phone_number"),
                    username = rs.getString("username"),
                    userStatus = Status.valueOf(rs.getString("status"))
                ),
                userCredentials = EmployeesResponseDto.UserCredentialsResponse(
                    gender = Gender.valueOf(rs.getString("gender")),
                    cardSerialNumber = rs.getString("card_serial_number"),
                    pinfl = rs.getString("pinfl")
                ),
                position = EmployeesResponseDto.PositionResponseDto(
                    id = rs.getLong("position_id"),
                    name = rs.getString("position_name")
                ),
                department = DepartmentShortResponse(
                    rs.getLong("department_id"),
                    rs.getString("department_name"),
                    rs.getString("department_description"),
                    DepartmentType.valueOf(rs.getString("department_type"))
                )
            )
        }

        return PageImpl(employees, pageable, totalElements.toLong())

    }

    override fun getOutOfficeEmployees(pageable: Pageable): Page<EmployeesResponseDto> {
        val organization = extraService.getOrgFromCurrentUser()

        val query = """
        select e.id                  as employee_id,
               u.id                  as user_id,
               u.full_name           as name,
               fa.hash_id            as hash_id,
               e.at_office           as at_office,
               e.labor_rate          as labor_rate,
               e.work_rate           as work_rate,
               uc.gender             as gender,
               uc.pinfl              as pinfl,
               uc.card_serial_number as card_serial_number,
               u.mail                as mail,
               u.role                as roles,
               u.phone_number        as phone_number,
               u.username            as username,
               u.status              as status,
               e.ph_status           as ph_status,
               p.id                  as position_id,
               p.name                as position_name,
               d.id                  as department_id,
               d.name                as department_name,
               d.description         as department_description,
               d.department_type     as department_type
        from employee e
                 join users u on e.user_id = u.id
                 left join file_asset fa on e.image_asset_id = fa.id
                 join user_credentials uc on u.id = uc.user_id
                 join position p on e.position_id = p.id
                 join department d on e.department_id = d.id
        where e.deleted = false
          and e.ph_status = 'BUSY'
          and e.status = 'ACTIVE'
          and e.organization_id = ${organization.id!!}
          and e.at_office = false
        limit ${pageable.pageSize} offset ${pageable.offset}
        """.trimIndent()

        val totalElementsQuery = """
       select count(distinct e.id)
       from employee e
           join users u on e.user_id = u.id
    where e.deleted = false 
    and e.ph_status = 'BUSY'
    and e.status = 'ACTIVE'
    and e.organization_id= ${organization.id!!} and  e.at_office=false
    """.trimIndent()

        val totalElements = jdbcTemplate.queryForObject(totalElementsQuery, Int::class.java)!!

        val employees = jdbcTemplate.query(query) { rs, _ ->
            EmployeesResponseDto(
                id = rs.getLong("employee_id"),
                atOffice = rs.getBoolean("at_office"),
                status = Status.valueOf(rs.getString("status")),
                laborRate = rs.getShort("labor_rate"),
                workRate = rs.getDouble("work_rate"),
                phStatus = PositionHolderStatus.valueOf(rs.getString("ph_status")),
                imageHashId = rs.getString("hash_id"),
                user = EmployeesResponseDto.UserResponseDto(
                    id = rs.getLong("user_id"),
                    fullName = rs.getString("name"),
                    mail = rs.getString("mail"),
                    role = Role.valueOf(rs.getString("roles")),
                    phoneNumber = rs.getString("phone_number"),
                    username = rs.getString("username"),
                    userStatus = Status.valueOf(rs.getString("status"))
                ),
                userCredentials = EmployeesResponseDto.UserCredentialsResponse(
                    gender = Gender.valueOf(rs.getString("gender")),
                    cardSerialNumber = rs.getString("card_serial_number"),
                    pinfl = rs.getString("pinfl")
                ),
                position = EmployeesResponseDto.PositionResponseDto(
                    id = rs.getLong("position_id"),
                    name = rs.getString("position_name")
                ),
                department = DepartmentShortResponse(
                    rs.getLong("department_id"),
                    rs.getString("department_name"),
                    rs.getString("department_description"),
                    DepartmentType.valueOf(rs.getString("department_type"))
                )
            )
        }

        return PageImpl(employees, pageable, totalElements.toLong())
    }


    override fun getOneById(id: Long) = repository.findByIdAndDeletedFalse(id)?.let { employee ->
        val credentials = employee.user?.id?.let { userCredentialsRepository.findByUserIdAndDeletedFalse(it) }
        EmployeesResponse.toDto(employee, credentials)
    } ?: throw EmployeeNotFoundException()


    override fun getAllForProject(
        projectId: Long,
        search: String?,
        pageable: Pageable
    ): Page<EmployeeResponse> {
        val project = projectRepository.findByIdAndDeletedFalse(projectId)
            ?: throw ProjectNotFoundException()
        val employee = extraService.getEmployeeFromCurrentUser()

        validationService.validateProjectEmployee(
            project,
            employee
        )
        return repository.findAllEmployeeForProject(
            employee.organization.id!!,
            projectId,
            search,
            pageable
        ).map {
            EmployeeResponse.toDto(it)
        }
    }

    override fun getAll(
        status: PositionHolderStatus?,
        search: String?,
        departmentId: Long?,
        positionId: Long?,
        attOffice: Boolean?,
        structureTree: Boolean?,
        pageable: Pageable
    ): EmployeeContentResponse {
        val organization = extraService.getOrgFromCurrentUser()

        val department = departmentId?.let {
            val department = departmentRepository.findByIdAndDeletedFalse(departmentId)
                ?: throw DepartmentNotFoundException()
            validationService.validateDifferentOrganizations(organization, department)
            department
        }

        val position = positionId?.let {
            val position = positionRepository.findByIdAndDeletedFalse(positionId)
                ?: throw PositionNotFoundException()
            validationService.validateDifferentOrganizations(organization, position)
            position
        }

        val searchQuery = search?.let { "'$search'" }
        val pStatus = status?.let { "'${status.name}'" }

        val countQuery = """
        select count(distinct e.id) 
        from employee e
                 left join users u on e.user_id = u.id
                 left join file_asset fa on e.image_asset_id = fa.id
                 left join user_credentials uc on u.id = uc.user_id
                 join position p on e.position_id = p.id
        where e.organization_id = ${organization.id}
        and e.deleted = false
        and (${searchQuery} is null
            or uc.fio ilike concat('%', ${searchQuery}, '%')
            or uc.pinfl ilike concat('%', ${searchQuery}, '%')
            or p.name ilike concat('%', ${searchQuery}, '%'))
        and ((${attOffice} is null) or e.at_office = ${attOffice})
        and ((${positionId} is null) or e.position_id = ${positionId})
        and ((${departmentId} is null) or e.department_id = ${departmentId})
        and ((${pStatus} is null) or e.ph_status = ${pStatus})
        """.trimIndent()
        val count = jdbcTemplate.queryForObject(countQuery, Long::class.java)!!

        val finalQuery = """
       select e.id                  as employee_id,
       u.id                  as user_id,
       u.full_name           as name,
       fa.hash_id            as hash_id,
       e.at_office           as at_office,
       e.status              as status,
       e.labor_rate          as labor_rate,
       e.work_rate           as work_rate,
       uc.gender             as gender,
       uc.pinfl              as pinfl,
       uc.card_serial_number as card_serial_number,
       u.mail                as mail,
       u.role                as roles,
       u.phone_number        as phone_number,
       u.username            as username,
       u.status              as u_status,
       e.ph_status           as ph_status,
       p.id                  as position_id,
       p.name                as position_name,
       d.id                  as department_id,
       d.name                as department_name,
       d.description         as department_description,
       d.department_type     as department_type
from employee e
         left join users u on e.user_id = u.id
         left join file_asset fa on e.image_asset_id = fa.id
         left join user_credentials uc on u.id = uc.user_id
         join position p on e.position_id = p.id
         join department d on e.department_id = d.id
where e.organization_id = ${organization.id}
  and e.deleted = false
  and (${searchQuery} is null
    or uc.fio ilike concat('%', ${searchQuery}, '%')
    or uc.pinfl ilike concat('%', ${searchQuery}, '%')
    or p.name ilike concat('%', ${searchQuery}, '%'))
  and ((${attOffice} is null) or e.at_office = ${attOffice})
  and ((${positionId} is null) or e.position_id = ${positionId})
  and ((${departmentId} is null) or e.department_id = ${departmentId})
  and ((${pStatus} is null) or e.ph_status = ${pStatus})
order by case e.status
    when 'ACTIVE' then 1
        when 'PENDING' then 2
            when 'DEACTIVATED' then 3 end, u.full_name
limit ${pageable.pageSize} offset ${pageable.offset}
    """.trimIndent()

        val employees = jdbcTemplate.query(finalQuery) { rs, _ ->
            EmployeesResponseDto(
                id = rs.getLong("employee_id"),
                atOffice = rs.getBoolean("at_office"),
                status = Status.valueOf(rs.getString("status")),
                laborRate = rs.getShort("labor_rate"),
                workRate = rs.getDouble("work_rate"),
                phStatus = PositionHolderStatus.valueOf(rs.getString("ph_status")),
                imageHashId = rs.getString("hash_id"),
                user = rs.getLong("user_id").takeIf { !rs.wasNull() }?.let {
                    EmployeesResponseDto.UserResponseDto(
                        id = it,
                        fullName = rs.getString("name"),
                        mail = rs.getString("mail"),
                        role = rs.getString("roles")?.let { Role.valueOf(it) } ?: Role.USER,
                        phoneNumber = rs.getString("phone_number"),
                        username = rs.getString("username"),
                        userStatus = rs.getString("u_status")?.let { Status.valueOf(it) } ?: Status.PENDING
                    )
                },
                userCredentials = if (!rs.wasNull()) {
                    EmployeesResponseDto.UserCredentialsResponse(
                        gender = rs.getString("gender")?.let { Gender.valueOf(it) } ?: Gender.MALE,
                        cardSerialNumber = rs.getString("card_serial_number"),
                        pinfl = rs.getString("pinfl")
                    )
                } else null,
                position = EmployeesResponseDto.PositionResponseDto(
                    id = rs.getLong("position_id"),
                    name = rs.getString("position_name")
                ),
                department = DepartmentShortResponse(
                    rs.getLong("department_id"),
                    rs.getString("department_name"),
                    rs.getString("department_description"),
                    DepartmentType.valueOf(rs.getString("department_type"))
                )
            )
        }

        return EmployeeContentResponse.toResponse(
            structures(structureTree, position, department),
            PageImpl(employees, pageable, count)
        )
    }


    override fun getAllEmployee(orgId: Long, pageable: Pageable): Page<EmployeeAdminResponse> {
        organizationRepository.findByIdAndDeletedFalse(orgId)
            ?: throw OrganizationNotFoundException()

        val query = """
            select e.id                    as employee_id,
                   u.id                    as user_id,
                   u.full_name             as name,
                   fa.hash_id              as hash_id,
                   u.role                  as roles,
                   u.phone_number          as phone_number,
                   u.username              as username,
                   u.status                as status,
                   e.ph_status             as ph_status,
                   e.status                as e_status,
                   d.id                    as department_id,
                   d.name                  as department_name,
                   d.description           as department_description,
                   d.department_type       as department_type,
                   json_agg(json_build_object('id', pm.id, 'name', pm.permission_data)) as permissions
            from employee e
                     left join users u on e.user_id = u.id
                     left join file_asset fa on e.image_asset_id = fa.id
                     left join department d on e.department_id = d.id
                     left join position p on e.position_id = p.id
                     left join position_permission pp on p.id = pp.position_id
                     left join permission pm on pp.permission_id = pm.id
            where e.organization_id = $orgId
              and e.deleted = false
            group by u.id, e.id,p.id,d.id,fa.id
            order by u.full_name
            limit ${pageable.pageSize} offset ${pageable.offset}
        """.trimIndent()

        val countQuery = """
            select count(distinct e.id) from employee e
                     left join users u on e.user_id = u.id
                     left join file_asset fa on e.image_asset_id = fa.id
                     left join department d on e.department_id = d.id
                     left join position p on e.position_id = p.id
                     left join position_permission pp on p.id = pp.position_id
                     left join permission pm on pp.permission_id = pm.id
            where e.organization_id = $orgId
              and e.deleted = false   
        """.trimIndent()
        val count = jdbcTemplate.queryForObject(countQuery, Long::class.java)!!

        val employees = jdbcTemplate.query(query) { rs, _ ->
            EmployeeAdminResponse(
                employeeId = rs.getLong("employee_id"),
                user = rs.getLong("user_id").takeIf { !rs.wasNull() }?.let {
                    EmployeeAdminResponse.UserEmployeeResponse(
                        userId = it,
                        fullName = rs.getString("name"),
                        phoneNumber = rs.getString("phone_number"),
                        username = rs.getString("username"),
                        status = rs.getString("status")?.let { Status.valueOf(it) }
                            ?: Status.PENDING)
                },
                department = EmployeeAdminResponse.DepartmentShortResponse(
                    id = rs.getLong("department_id"),
                    name = rs.getString("department_name"),
                    description = rs.getString("department_description").takeIf { !rs.wasNull() }?.let { it },
                    departmentType = DepartmentType.valueOf(rs.getString("department_type"))
                ),
                imageHashId = rs.getString("hash_id"),
                status = rs.getString("e_status")?.let { Status.valueOf(it) } ?: Status.PENDING,
                phStatus = PositionHolderStatus.valueOf(rs.getString("ph_status")),
                empRole = rs.getString("roles")?.let { Role.valueOf(it) } ?: Role.USER,
                permissions = rs.getString("permissions")?.takeIf { it.isNotEmpty() }?.let { json ->
                    objectMapper.readValue(
                        json,
                        object : TypeReference<List<EmployeeAdminResponse.PermissionResponse>>() {})
                }?.map { permission ->
                    EmployeeAdminResponse.PermissionResponse(
                        id = permission.id,
                        name = permission.name
                    )
                } ?: emptyList()
            )
        }
        return PageImpl(employees, pageable, count)
    }

    override fun getEmployeesStatistics(
        startDate: Long,
        endDate: Long,
        pageable: Pageable
    ): Page<EmployeeStatisticsResponse> {
        val organization = extraService.getOrgFromCurrentUser()
        val countQuery =
            """
                with filtered_table_date as (
                    select id,organization_id,type,date
                    from table_date
                    where deleted = false
                      and organization_id = ${organization.id}
                      and date between '${startDate.timeWithUTC()}' and '${endDate.timeWithUTC()}'
                )
                select count(distinct e.id)
                from employee e
                         join users u on e.user_id = u.id
                         join filtered_table_date ftd on e.organization_id = ftd.organization_id
                where e.organization_id = ${organization.id}
                  and e.deleted = false
                  and e.ph_status = 'BUSY'
                  and ((ftd.date  between '${startDate.timeWithUTC()}' and '${endDate.timeWithUTC()}'))
            """.trimIndent()
        val count = jdbcTemplate.queryForObject(countQuery, Long::class.java)!!

        val query =
            """
                with filtered_table_date as (
                    select id,organization_id,type,date
                    from table_date
                    where deleted = false
                      and organization_id = ${organization.id}
                      and date between '${startDate.timeWithUTC()}' and '${endDate.timeWithUTC()}'
                ),
                     tracked_minutes as (
                         select tt.owner_id as owner_id,
                                sum(tt.duration) as worked_task_time
                         from time_tracking tt
                                  join task t on tt.task_id = t.id
                                  join filtered_table_date td on td.id = tt.table_date_id
                         where tt.deleted = false
                           and t.deleted = false
                         group by tt.owner_id
                     ),
                     working_date as (
                         select sum(wdc.required_minutes) as required_minutes
                         from working_date_config wdc
                                  join filtered_table_date td on wdc.organization_id = td.organization_id and td.type = 'WORK_DAY'
                         where wdc.deleted = false
                     ),
                     worked_time as (
                         select tr.user_id as user_id,
                                sum(tr.amount) as work_minutes
                         from tourniquet_tracker tr
                                  join filtered_table_date td on td.id = tr.table_date_id
                         where tr.deleted = false
                         group by tr.user_id
                     ),
                     attendance_data as (
                         select tr.user_id as user_id,
                                min(tr.in_time) as first_in_time,
                                td.date as work_date
                         from tourniquet_tracker tr
                                  join filtered_table_date td on tr.table_date_id = td.id
                         where td.type = 'WORK_DAY'
                         group by tr.user_id, td.date
                     ),
                     task_date as (
                         select tpe.employees_id as project_employee_id,
                                sum(case when s.name = 'CLOSED' and s.immutable = true then 1 else 0 end) as completed_tasks,
                                sum(case when not (s.name = 'CLOSED' and s.immutable = true) then 1 else 0 end) as incomplete_tasks,
                                sum(t.time_estimate_amount) as total_estimate_time
                         from task t
                                  join state s on t.state_id = s.id
                                  join task_project_employee tpe on t.id = tpe.task_id
                         where t.deleted = false
                           and (t.start_date between '${startDate.timeWithUTC()}' and '${endDate.timeWithUTC()}' or 
                           t.end_date between '${startDate.timeWithUTC()}' and '${endDate.timeWithUTC()}')
                         group by tpe.employees_id
                     )
                select
                    e.id as id,
                    u.full_name as full_name,
                    count(distinct pe.project_id) as count_projects,
                    coalesce(task_date.total_estimate_time, 0) as total_task_time,
                    coalesce(tm.worked_task_time, 0) as worked_task_time,
                    coalesce((select required_minutes from working_date), 0) as required_minutes,
                    coalesce(wt.work_minutes, 0) as worked_time,
                    coalesce(sum(case when ad.first_in_time > ad.work_date then 1 else 0 end), 0) as late_days,
                    coalesce(sum(case when ad.first_in_time < ad.work_date then 1 else 0 end), 0) as early_days,
                    coalesce(sum(case when ad.first_in_time is null then 1 else 0 end), 0) as absent_days,
                    coalesce(task_date.completed_tasks, 0) as count_complated_tasks,
                    coalesce(task_date.incomplete_tasks, 0) as count_incomplate_tasks
                from employee e
                         join users u on e.user_id = u.id
                         join user_employment_history ueh on e.id = ueh.employee_id
                         join filtered_table_date ftd on e.organization_id = ftd.organization_id
                         left join project_employee pe on e.id = pe.employee_id
                         left join tracked_minutes tm on tm.owner_id = u.id
                         left join task_date on pe.id = task_date.project_employee_id
                         left join worked_time wt on e.user_id = wt.user_id
                         left join attendance_data ad on e.user_id = ad.user_id
                where e.organization_id = ${organization.id}
                  and e.deleted = false
                  and e.ph_status = 'BUSY'
                  and ((ftd.date  between '${startDate.timeWithUTC()}' and '${endDate.timeWithUTC()}'))
                group by u.full_name, e.id, task_date.total_estimate_time, tm.worked_task_time,
                         wt.work_minutes, task_date.completed_tasks, task_date.incomplete_tasks
                order by u.full_name
                limit ${pageable.pageSize} offset ${pageable.offset}        
            """.trimIndent()
        val response = jdbcTemplate.query(query) { rs, _ ->
            EmployeeStatisticsResponse(
                rs.getLong("id"),
                rs.getString("full_name"),
                rs.getShort("count_projects"),
                rs.getInt("total_task_time"),
                rs.getInt("worked_task_time"),
                rs.getInt("required_minutes"),
                rs.getInt("worked_time"),
                rs.getShort("count_complated_tasks"),
                rs.getShort("count_incomplate_tasks"),
                rs.getShort("absent_days"),
                rs.getShort("late_days"),
                rs.getShort("early_days")
            )
        }

        return PageImpl(response, pageable, count)


    }

    override fun getEmployeesAttendance(
        date: Long,
        pageable: Pageable,
        status: AttendanceStatus?
    ): Page<EmployeeAttendanceResponse>? {

        val organization = extraService.getOrgFromCurrentUser()
        val localDate = date.localDate()
        val statusQuery = status?.let { "'${status}'" }
        val statusCondition = when(status) {
            AttendanceStatus.ON_TIME, AttendanceStatus.LATE  -> "($statusQuery is null) or status = $statusQuery"
            AttendanceStatus.ABSENT -> "status is null"
            null -> "true"
        }

        val query = """
            with attendance_employees as (
                select
                    e.id                                               as employee_id,
                    u.id                                               as user_id,
                    u.full_name                                        as full_name,
                    p.name                                             as position_name,
                    f.hash_id                                          as image_hash_id,
                    min(case when ut.type = 'IN' then ut.time end )    as start_date,
                    max(case  when ut.type = 'IN' then ut.status end)  as status
                from employee e
                    join users u on e.user_id = u.id
                    join position p on p.id = e.position_id
                    left join (select * from user_tourniquet as  inner_ut
                                                  where inner_ut.organization_id = ${organization.id} 
                    and inner_ut.deleted = false) as ut 
                        on ut.user_id = u.id
                        and cast(ut.time as date) = '$localDate'
                    left join table_date td 
                        on ut.table_date_id = td.id
                        and td.type = 'WORK_DAY'
                    left join file_asset f 
                        on e.image_asset_id = f.id
                where e.ph_status = 'BUSY'
                and e.status = 'ACTIVE'
                and e.organization_id = ${organization.id}
                and e.deleted = false
                group by e.id, u.id, f.id, p.name)
            select *
            from attendance_employees
            where $statusCondition
            order by start_date
            limit ${pageable.pageSize} offset ${pageable.offset}
        """.trimIndent()


        val totalElementsQuery = """
                        with total_elements as (
                select
                    e.id                                               as employee_id,
                    min(case when ut.type = 'IN' then ut.time end )    as start_date,
                    max(case  when ut.type = 'IN' then ut.status end)  as status
                from
               employee e
                    join users u on e.user_id = u.id
                    join position p on p.id = e.position_id
                    left join user_tourniquet ut
                        on ut.user_id = u.id
                        and cast(ut.time as date) = '$localDate'
                    left join table_date td 
                        on ut.table_date_id = td.id
                        and td.type = 'WORK_DAY'
                    left join file_asset f 
                        on e.image_asset_id = f.id
                where e.ph_status = 'BUSY'
                and e.status = 'ACTIVE'
                and e.organization_id = ${organization.id}
                and e.deleted = false
                group by e.id, u.id, f.id, p.name)
            
            select count(*)
            from total_elements
            where $statusCondition
        """.trimIndent()

        val totalElements = jdbcTemplate.queryForObject(totalElementsQuery, Int::class.java)!!

        val lateEmployees = jdbcTemplate.query(query, ResultSetExtractor { rs ->
            val list = mutableListOf<EmployeeAttendanceResponse>()
            while (rs.next()) {
               val resultStatus = rs.getString("status") ?: "ABSENT"
                list.add(
                    EmployeeAttendanceResponse(
                        employeeId = rs.getLong("employee_id"),
                        userId = rs.getLong("user_id"),
                        fullName = rs.getString("full_name"),
                        positionName = rs.getString("position_name"),
                        imageHashId = rs.getString("image_hash_id"),
                        time = rs.getTimestamp("start_date"),
                        state = AttendanceStatus.valueOf(resultStatus)
                    )
                )
            }
            list
        })
        return PageImpl(lateEmployees!!.toList(), pageable, totalElements.toLong())
    }

    override fun getEmployeeKPI(employeeId: Long, startDate: Long, endDate: Long): List<KPIDataResponse>? {
        val organization = extraService.getOrgFromCurrentUser()
        if (extraService.getSessionRole() == Role.USER) {
            extraService.getEmployeeFromCurrentUser().let {
                if (it.id != employeeId) throw AccessDeniedException()
            }
        } else if(extraService.getSessionRole() == Role.ORG_ADMIN) {
            userOrgStoreRepository.findByOrganizationIdAndUserIdAndDeletedFalse(organization.id!!, userId())?.let {
                if (!it.granted) throw AccessDeniedException()
            } ?: throw OrganizationNotFoundException()
            repository.findByIdAndDeletedFalse(employeeId)?.let {
                if (it.organization.id != organization.id) throw AccessDeniedException()
            } ?: throw EmployeeNotFoundException()
        }
        sameMonthValidation(startDate, endDate)

        val query = """
            with employee_kpi as (select
                                      cast(extract(epoch from td.date) * 1000 as bigint)     as work_date,
                                      td.type                                                as day_type,
                                      td.id                                                  as table_date_id,
                                      coalesce(sum(worked_t.worked_minutes), 0)              as work_minutes,
                                      coalesce(sum(tracked_m.tracked_minutes), 0)            as tracked_minutes,
                                      coalesce(sum(estimated_m.estimated_minutes), 0)        as estimated_minutes,
                                      coalesce(sum(wdc.required_minutes), 0)                 as required_minutes
                                  from organization o
                                           join table_date td
                                               on td.organization_id = o.id
                                           join (select e.user_id,
                                                        e.organization_id,
                                                        e.department_id
                                                 from employee e
                                                 where e.ph_status = 'BUSY'
                                                   and e.id = ${employeeId}
                                                   and e.status = 'ACTIVE'
                                                   and e.deleted = false) e
                                               on e.organization_id = o.id
                                           join users u
                                               on u.id = e.user_id
                                           left join (select
                                                          t.user_id,
                                                          t.table_date_id,
                                                          sum(t.amount) as worked_minutes
                                                      from tourniquet_tracker t
                                                      where t.deleted = false
                                                      group by t.user_id, t.table_date_id) worked_t
                                               on worked_t.user_id = u.id and worked_t.table_date_id = td.id
                                           left join (select
                                                          t.owner_id,
                                                          t.table_date_id,
                                                          sum(t.duration) as tracked_minutes
                                                      from time_tracking t
                                                          join table_date td on td.id = t.table_date_id
                                                      where t.deleted = false
                                                      and td.deleted = false
                                                      group by t.owner_id, t.table_date_id)  tracked_m
                                               on tracked_m.owner_id = u.id and tracked_m.table_date_id = td.id
                                           left join (select
                                                           tt.owner_id,
                                                           tt.table_date_id,
                                                           sum(coalesce(t.time_estimate_amount, 0)) as estimated_minutes
                                                       from time_tracking tt
                                                           join task t on tt.task_id = t.id
                                                       where tt.deleted = false
                                                       and t.deleted = false
                                                       group by tt.owner_id, tt.table_date_id)  estimated_m
                                                on estimated_m.table_date_id = td.id and estimated_m.owner_id = u.id
                                           left join (select
                                                           w.day                   as day,
                                                           sum(w.required_minutes) as required_minutes
                                                       from working_date_config w
                                                       where w.organization_id = ${organization.id}
                                                       and w.deleted = false
                                                       group by w.day) wdc on wdc.day = trim(to_char(td.date, 'DAY')) and td.type = 'WORK_DAY'
                                           where o.id = '${organization.id}'
                                           and td.deleted = false
                                           and u.deleted = false
                                           and td.date between '${startDate.timeWithUTC()}' and '${endDate.timeWithUTC()}'
                                           group by u.id, td.id
                                  )
            select * from employee_kpi  ek order by ek.work_date desc
        """.trimIndent()

        val result: List<KPIDataResponse> = jdbcTemplate.query(query) { rs, _ ->
            KPIDataResponse(
                workingDate = rs.getLong("work_date"),
                dayType = TableDateType.valueOf(rs.getString("day_type")),
                tableDateId = rs.getLong("table_date_id"),
                workingMinutes = rs.getInt("work_minutes"),
                trackedMinutes = rs.getInt("tracked_minutes"),
                estimatedMinutes = rs.getInt("estimated_minutes"),
                requiredMinutes = rs.getInt("required_minutes")
            )
        }
        return result
    }

    @Transactional
    override fun delete(id: Long) {
        repository.findByIdAndDeletedFalse(id)?.let { employee ->
            validationService.validateDifferentOrganizations(
                employee.organization,
                extraService.getOrgFromCurrentUser()
            )
            if (employee.phStatus != PositionHolderStatus.VACANT) throw EmployeeIsBusyException()
            repository.trash(employee.id!!)
        } ?: throw EmployeeNotFoundException()
    }


    @Transactional
    override fun update(id: Long, request: EmployeeUpdateRequest): EmployeeDto {
        val organization = extraService.getOrgFromCurrentUser()
        return repository.findByIdAndDeletedFalse(id)?.let { oldEmployee ->

            validationService.validateDifferentOrganizations(oldEmployee.organization, organization)

            val employee = updatePositionOrDepartment(organization, oldEmployee, request)
            request.imageHashId?.let {
                if (employee.isVacant()) throw EmployeeIsVacantException()
                val file = fileAssetRepository.findByHashIdAndDeletedFalse(it) ?: throw FileNotFoundException()
                //validationService.validateEmployeeImage(file)
                validationService.validateImageTypeAndSize(file)
                employee.imageAsset = file
            }

            request.apply {
                request.workRate?.let { employee.workRate = it }
                request.laborRate?.let { employee.laborRate = it }
                request.status?.let { employee.status = it }
                employee.code = request.code
            }
            repository.save(employee)
            EmployeeDto.toDto(employee)
        } ?: throw EmployeeNotFoundException()
    }


    override fun changeStatus(id: Long, status: Status): BaseMessage {
        val organization = extraService.getOrgFromCurrentUser()
        val userId = userId()
        userOrgStoreRepository.findByUserIdAndOrganizationIdAndDeletedFalse(userId, organization.id!!)?.let {
            if (it.role != Role.ORG_ADMIN) throw PermissionDeniedException()
        } ?: throw UserOrgStoreNotFoundException()

        repository.findByIdAndDeletedFalse(id)?.let { employee ->
            validationService.validateDifferentOrganizations(employee.organization, organization)
            employee.status = status
            repository.save(employee)
            EmployeeDto.toDto(employee)
        } ?: throw EmployeeNotFoundException()
        return BaseMessage.OK
    }

    override fun getOrganizationLateEmployees(date: Long, pageable: Pageable): Page<LateEmployeeResponse> {
        val organization = extraService.getOrgFromCurrentUser()

        val localDate = date.localDate()
        val workingDateConfig = workingDateConfig(organization.id!!, localDate)
        val startOfOWork = convertToDate(localDate, workingDateConfig.startHour)

        val query = """
            with late_employee as (select e.id                                     as employee_id,
                                          u.id                                     as user_id,
                                          u.full_name                              as name,
                                          MIN(CASE WHEN type = 'IN' THEN time END) as start_date,
                                          fa.hash_id                               as hash_id
                                   from user_tourniquet ut
                                            join employee e on ut.user_id = e.user_id
                                            join users u on ut.user_id = u.id
                                            left join file_asset fa on e.image_asset_id = fa.id
                                   where ut.organization_id = ${organization.id!!}
                                     and e.deleted = false
                                     and e.ph_status = 'BUSY'
                                     and cast(time as date) = '${localDate}'
                                   group by u.id, e.id, fa.id)
            select employee_id,
                   user_id,
                   name,
                   hash_id,
                   cast(extract(epoch from start_date) * 1000 as bigint) as start_date
            from late_employee
            where start_date > '${startOfOWork.plusMin(15)}'
            limit ${pageable.pageSize} offset ${pageable.offset}
       """.trimIndent()

        val totalElementsQuery = """
                 with late_employee as (select e.id                                     as employee_id,
                                               MIN(CASE WHEN type = 'IN' THEN time END) as start_date
                                        from user_tourniquet ut
                                                 join employee e on ut.user_id = e.user_id
                                                 join users u on ut.user_id = u.id
                                                 left join file_asset fa on e.image_asset_id = fa.id
                                        where ut.organization_id = ${organization.id!!}
                                          and cast(time as date) = '${localDate}'
                                          and e.deleted = false
                                        group by e.id)
                 select count(distinct employee_id)
                 from late_employee
                 where start_date > '${startOfOWork.plusMin(15)}'
    """.trimIndent()
        val totalElements = jdbcTemplate.queryForObject(totalElementsQuery, Int::class.java)!!

        val lateEmployees = jdbcTemplate.query(query) { rs, _ ->
            LateEmployeeResponse(
                id = rs.getLong("employee_id"),
                userId = rs.getLong("user_id"),
                name = rs.getString("name"),
                lateDate = rs.getLong("start_date"),
                imageHashId = rs.getString("hash_id")
            )
        }

        return PageImpl(lateEmployees, pageable, totalElements.toLong())

    }

    override fun getOrganizationAbsentEmployees(date: Long, pageable: Pageable): Page<EmployeesResponse> {
        val organization = extraService.getOrgFromCurrentUser()
        val midnight = Date(date).midnight()

        return absentEmployees(midnight, organization.id!!, pageable)
    }

    override fun createVacantEmployee(request: EmployeeVacantRequest): EmployeeDto {
        val department = departmentRepository.findByIdAndDeletedFalse(request.departmentId)
            ?: throw DepartmentNotFoundException()
        val position = positionRepository.findByIdAndDeletedFalse(request.positionId)
            ?: throw PositionNotFoundException()
        val organization = department.organization

        validationService.validateDifferentOrganizations(department, position)
        validationService.validateDifferentOrganizations(organization, extraService.getOrgFromCurrentUser())

        val ids = position.permission.map { it.id!! }.toMutableSet()
        val permissions = permissionRepository.findAllByIdInAndDeletedFalse(ids)
        itpu.uz.itpuhrms.security.logger.info { "Permissions: $permissions" }
        val save = repository.save(
            Employee(
                null,
                null,
                request.status,
                false,
                position,
                department,
                organization,
                PositionHolderStatus.VACANT,
                request.workRate,
                request.laborRate,
                permissions,
                null
            )
        )

        return EmployeeDto.toDto(save)
    }

    override fun getEmployeeAgeStatistics(): EmployeeAgeStatisticResponse {
        val organization = extraService.getOrgFromCurrentUser()
        return repository.findOrganizationEmployeeAgeStatistics(organization.id!!)
    }

    override fun getUserWorkingDaysStatistics(
        userId: Long,
        startDate: Long,
        endDate: Long
    ): UserWorkingStatisticResponse {
        val employee = extraService.getEmployeeFromCurrentUser()

        return repository.findUserWorkingDaysStatistics(
            employee.organization.id!!,
            employee.user!!.id!!,
            Date(startDate),
            Date(endDate)
        )
    }

    override fun getEmployeesByBirthday(type: BirthdayType, pageable: Pageable): Page<EmployeeBirthdateResponse> {
        val organization = extraService.getOrgFromCurrentUser()

        val (startDate, endDate) = when (type) {
            BirthdayType.TODAY -> {
                val today = LocalDate.now()
                today to today
            }

            BirthdayType.TOMORROW -> {
                val tomorrow = LocalDate.now().plusDays(1)
                tomorrow to tomorrow
            }

            BirthdayType.THIS_MONTH -> {
                val today = LocalDate.now()
                val endOfMonth = today.withDayOfMonth(today.lengthOfMonth())
                today to endOfMonth
            }
        }

        val query = """
        select e.id        as id,
               fa.hash_id  as hash_id,
               uc.birthday as birthday,
               u.full_name as full_name,
               d.name      as department_name
        from employee e
                 join users u on e.user_id = u.id
                 join user_credentials uc on e.user_id = uc.user_id
                 left join file_asset fa on e.image_asset_id = fa.id
                 left join department d on e.department_id = d.id
        where e.organization_id = ${organization.id!!}
          and e.ph_status = 'BUSY'
          and u.deleted = false
          and e.deleted = false
          and uc.deleted = false
          and (date_part('month', uc.birthday) = date_part('month', cast('${startDate}' as date)))
               and date_part('day', uc.birthday) >= date_part('day', cast('${startDate}' as date))
               and date_part('day', uc.birthday) <= date_part('day', cast('${endDate}' as date))
        limit ${pageable.pageSize} offset ${pageable.offset}
    """.trimIndent()

        val countQuery = """
        select count(distinct e.id)
        from employee e
           join users u on e.user_id = u.id
           join user_credentials uc on e.user_id = uc.user_id
           left join file_asset fa on e.image_asset_id = fa.id
           left join department d on e.department_id = d.id
        where e.organization_id = ${organization.id!!}
             and e.ph_status = 'BUSY'
             and u.deleted = false
             and e.deleted = false
             and uc.deleted = false
             and (date_part('month', uc.birthday) = date_part('month', cast('${startDate}' as date)))
               and date_part('day', uc.birthday) >= date_part('day', cast('${startDate}' as date))
               and date_part('day', uc.birthday) <= date_part('day', cast('${endDate}' as date))
    """.trimIndent()

        val count = jdbcTemplate.queryForObject(countQuery, Long::class.java)!!

        val employees = jdbcTemplate.query(query) { rs, _ ->
            EmployeeBirthdateResponse(
                id = rs.getLong("id"),
                imageHashId = rs.getString("hash_id"),
                birthday = rs.getDate("birthday"),
                fullName = rs.getString("full_name"),
                departmentName = rs.getString("department_name")
            )
        }

        return PageImpl(employees, pageable, count)
    }

    override fun getEmployeesKPI(
        startDate: Long,
        endDate: Long,
        search: String?,
        departmentId: Long?,
        active: Boolean,
        pageable: Pageable
    ): Page<EmployeeKPIResponse> {

        val organization = extraService.getOrgFromCurrentUser()
        val searchQuery = search?.let { "'$search'" }

        departmentId?.let {
            val department = departmentRepository.findByIdAndDeletedFalse(it)
                ?: throw DepartmentNotFoundException()
            if (department.organization.id != organization.id) throw DifferentOrganizationsException()
        }
        return if (active)
            activeEmployeesKPI(organization, departmentId, searchQuery, startDate, endDate, pageable)
        else
            allEmployeesKPI(organization, departmentId, searchQuery, startDate, endDate, pageable)
    }

    private fun workingDateConfig(organizationId: Long, localDate: LocalDate): WorkingDateConfig {
        return workingDateConfigRepository.findByOrganizationIdAndDayAndDeletedFalse(
            organizationId,
            localDate.weekDay()
        ) ?: throw WorkingDateConfigNotFoundException()
    }


    private fun absentEmployees(
        midnight: Date,
        orgId: Long,
        pageable: Pageable
    ) = repository.findOrganizationAbsentEmployees(midnight, orgId, pageable)
        .map {
            val credentials = it.user?.id?.let { id -> userCredentialsRepository.findByUserIdAndDeletedFalse(id) }
            EmployeesResponse.toDto(it, credentials)
        }

    private fun updatePositionOrDepartment(
        organization: Organization,
        employee: Employee,
        request: EmployeeUpdateRequest
    ): Employee {
        request.run {
            val department = departmentId?.let {
                val department = departmentRepository.findByIdAndDeletedFalse(departmentId)
                    ?: throw DepartmentNotFoundException()
                validationService.validateDifferentOrganizations(organization, department)
                department
            }

            val position = positionId?.let {
                val position = positionRepository.findByIdAndDeletedFalse(positionId)
                    ?: throw PositionNotFoundException()
                validationService.validateDifferentOrganizations(organization, position)
                position
            }

            val changed = (position != null && positionId != employee.position.id) ||
                    (department != null && departmentId != employee.department.id)

            department?.let { employee.department = it }
            position?.let { employee.position = it }
            if (employee.isBusy() && changed) {
                updateHistory(employee, organization)
            }
            return employee
        }
    }


    private fun updateHistory(employee: Employee, organization: Organization) {
        val history = latestHistory(employee.user!!, organization)
        historyRepository.save(
            history.apply { this.dismissedDate = Date() }
        )
        historyRepository.save(
            employee.run {
                UserEmploymentHistory(
                    this,
                    user!!,
                    position,
                    department,
                    Date()
                )
            }
        )
    }

    private fun latestHistory(user: User, organization: Organization): UserEmploymentHistory {
        return historyRepository.findTopByUserIdAndDepartmentOrganizationIdAndDeletedFalseOrderByIdDesc(
            user.id!!, organization.id!!
        ) ?: throw EmployeeIsVacantException()
    }


    private fun structures(structureTree: Boolean?, position: Position?, department: Department?): List<Structure>? {
        if (structureTree == true && department != null) {

            val structures = mutableListOf(
                Structure(
                    department.id!!,
                    department.name,
                    StructureType.DEPARTMENT
                )
            )
            if (position != null) {
                structures.add(
                    0,
                    Structure(
                        position.id!!,
                        position.name,
                        StructureType.POSITION
                    )
                )
            }

            var parentDepartment = department.parentDepartment
            while (parentDepartment != null) {
                structures.add(
                    Structure(
                        parentDepartment.id!!,
                        parentDepartment.name,
                        StructureType.DEPARTMENT,
                        true
                    )
                )
                parentDepartment = parentDepartment.parentDepartment
            }
            return structures
        }
        return null
    }

    private fun activeEmployeesKPI(
        organization: Organization,
        departmentId: Long?,
        searchQuery: String?,
        startDate: Long,
        endDate: Long,
        pageable: Pageable
    ): PageImpl<EmployeeKPIResponse> {
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
            with result as (select u.id                                                  as id,
                                   u.full_name                                           as full_name,
                                   cast(extract(epoch from td.date) * 1000 as bigint)    as work_date,
                                   td.type                                               as day_type,
                                   td.id                                                 as table_date_id,
                                   coalesce(sum(worked_time.work_minutes), 0)            as work_minutes,
                                   coalesce(sum(tracked_minutes.tracked_minutes), 0)     as tracked_minutes,
                                   coalesce(sum(estimated_minutes.estimated_minutes), 0) as estimated_minutes,
                                   coalesce(sum(wdc.required_minutes), 0)                as required_minutes
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
                                     left join (select tr.user_id       as user_id,
                                                       tr.table_date_id as table_date_id,
                                                       sum(tr.amount)   as work_minutes
                                                from tourniquet_tracker tr
                                                where deleted = false
                                                group by user_id, table_date_id) worked_time
                                               on worked_time.user_id = u.id and worked_time.table_date_id = td.id
                                     left join (select tt.owner_id, tt.table_date_id, sum(tt.duration) as tracked_minutes
                                                from time_tracking tt
                                                         join task t on tt.task_id = t.id
                                                where tt.deleted = false
                                                  and t.deleted = false
                                                group by tt.owner_id, tt.table_date_id) tracked_minutes
                                               on tracked_minutes.owner_id = u.id and
                                                  tracked_minutes.table_date_id = td.id
                                     left join (select tt.owner_id,
                                                       tt.table_date_id,
                                                       sum(coalesce(t.time_estimate_amount, 0)) as estimated_minutes
                                                from time_tracking tt
                                                         join task t on tt.task_id = t.id
                                                where tt.deleted = false
                                                  and t.deleted = false
                                                group by tt.owner_id, tt.table_date_id) estimated_minutes
                                               on estimated_minutes.owner_id = u.id and
                                                  estimated_minutes.table_date_id = td.id
                                     left join (select wdc.day                   as day,
                                                       sum(wdc.required_minutes) as required_minutes
                                                from working_date_config wdc
                                                where wdc.organization_id = ${organization.id}
                                                  and wdc.deleted = false
                                                group by wdc.day) wdc
                                               on wdc.day = trim(to_char(td.date, 'DAY')) and td.type = 'WORK_DAY'
                            where o.id = ${organization.id}
                              and td.deleted = false
                              and u.deleted = false
                              and (${departmentId} is null or $departmentId = e.department_id)
                              and (${searchQuery} is null or u.full_name ilike concat('%', ${searchQuery}, '%'))
                              and td.date between '${startDate.timeWithUTC()}' and '${endDate.timeWithUTC()}'
                            group by u.id, td.id)

            select id,
                   full_name,
                   jsonb_agg(
                           jsonb_build_object(
                                   'workingDate', work_date,
                                   'dayType', day_type,
                                   'workingMinutes', work_minutes,
                                   'trackedMinutes', tracked_minutes,
                                   'requiredMinutes', required_minutes,
                                   'estimatedMinutes', estimated_minutes,
                                   'tableDateId', table_date_id
                           ) order by work_date
                   ) as work_summary
            from result
            group by id, full_name
            order by full_name
            limit ${pageable.pageSize} offset ${pageable.offset}
        """.trimIndent()

        val response = jdbcTemplate.query(query) { rs, _ ->
            val kpiJson = rs.getString("work_summary")
            EmployeeKPIResponse(
                rs.getLong("id"),
                rs.getString("full_name"),
                objectMapper.readValue<List<KPIDataResponse>>(kpiJson)
            )
        }

        return PageImpl(response, pageable, count)
    }

    private fun allEmployeesKPI(
        organization: Organization,
        departmentId: Long?,
        searchQuery: String?,
        startDate: Long,
        endDate: Long,
        pageable: Pageable
    ): PageImpl<EmployeeKPIResponse> {

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
                                       group by u.id, td.id),
            
            
                 work_summary AS (SELECT ud.id                                                   AS id,
                                         ud.full_name                                            AS full_name,
                                         cast(extract(epoch from ud.work_date) * 1000 as bigint) as work_date,
                                         ud.day_type                                             as day_type,
                                         ud.table_date_id                                        as table_date_id,
                                         coalesce(sum(worked_time.work_minutes), 0)              as work_minutes,
                                         coalesce(sum(tracked_minutes.tracked_minutes), 0)       as tracked_minutes,
                                         coalesce(sum(estimated_minutes.estimated_minutes), 0)   as estimated_minutes,
                                         coalesce(sum(wdc.required_minutes), 0)                  as required_minutes
                                  FROM unique_user_dates ud
                                           left join (select tr.user_id       as user_id,
                                                             tr.table_date_id as table_date_id,
                                                             sum(tr.amount)   as work_minutes
                                                      from tourniquet_tracker tr
                                                      where deleted = false
                                                      group by user_id, table_date_id) worked_time
                                                     on worked_time.user_id = ud.id and worked_time.table_date_id = ud.table_date_id
                                           left join (select tt.owner_id, tt.table_date_id, sum(tt.duration) as tracked_minutes
                                                      from time_tracking tt
                                                               join task t on tt.task_id = t.id
                                                      where tt.deleted = false
                                                        and t.deleted = false
                                                      group by tt.owner_id, tt.table_date_id) tracked_minutes
                                                     on tracked_minutes.owner_id = ud.id and
                                                        tracked_minutes.table_date_id = ud.table_date_id
                                           left join (select tt.owner_id,
                                                             tt.table_date_id,
                                                             sum(coalesce(t.time_estimate_amount, 0)) as estimated_minutes
                                                      from time_tracking tt
                                                               join task t on tt.task_id = t.id
                                                      where tt.deleted = false
                                                        and t.deleted = false
                                                      group by tt.owner_id, tt.table_date_id) estimated_minutes
                                                     on estimated_minutes.owner_id = ud.id and
                                                        estimated_minutes.table_date_id = ud.table_date_id
                                           left join (select wdc.day                   as day,
                                                             sum(wdc.required_minutes) as required_minutes
                                                      from working_date_config wdc
                                                      where wdc.organization_id = ${organization.id}
                                                        and wdc.deleted = false
                                                      group by wdc.day) wdc
                                                     on wdc.day = trim(to_char(ud.work_date, 'DAY')) and ud.day_type = 'WORK_DAY'
                                  group by ud.id, ud.full_name, ud.work_date, ud.day_type, ud.table_date_id)
            
            select id,
                   full_name,
                   jsonb_agg(
                           jsonb_build_object(
                                   'workingDate', work_date,
                                   'dayType', day_type,
                                   'workingMinutes', work_minutes,
                                   'trackedMinutes', tracked_minutes,
                                   'requiredMinutes', required_minutes,
                                   'estimatedMinutes', estimated_minutes,
                                   'tableDateId', table_date_id
                           ) order by work_date
                   ) as work_summary
            from work_summary
            group by id, full_name
            order by full_name
            limit ${pageable.pageSize} offset ${pageable.offset}
        """.trimIndent()

        val response = jdbcTemplate.query(query) { rs, _ ->
            val kpiJson = rs.getString("work_summary")
            EmployeeKPIResponse(
                rs.getLong("id"),
                rs.getString("full_name"),
                objectMapper.readValue<List<KPIDataResponse>>(kpiJson)
            )
        }

        return PageImpl(response, pageable, count)
    }
    private fun sameMonthValidation(startDate: Long, endDate: Long) {
        if (YearMonth.from(startDate.localDateWithUTC()) != YearMonth.from(endDate.localDateWithUTC()))
            throw NotSameMonthOfYearException()
    }
}