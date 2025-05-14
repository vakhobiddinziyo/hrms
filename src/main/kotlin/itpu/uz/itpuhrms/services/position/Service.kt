package itpu.uz.itpuhrms.services.position

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import itpu.uz.itpuhrms.*
import itpu.uz.itpuhrms.services.ExtraService
import itpu.uz.itpuhrms.services.department.DepartmentRepository
import itpu.uz.itpuhrms.services.department.Structure
import itpu.uz.itpuhrms.services.employee.EmployeeRepository
import itpu.uz.itpuhrms.services.organization.OrganizationRepository
import itpu.uz.itpuhrms.services.permission.PermissionRepository
import itpu.uz.itpuhrms.services.permission.PermissionResponse
import itpu.uz.itpuhrms.services.validation.ValidationService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


interface PositionService {
    fun adminCreate(request: PositionAdminRequest): PositionResponse
    fun adminUpdate(request: PositionAdminRequest, id: Long): PositionAdminResponse
    fun adminDelete(id: Long)
    fun create(request: PositionRequest): PositionResponse
    fun getOneById(id: Long): PositionAdminResponse
    fun update(id: Long, request: PositionRequest): PositionAdminResponse
    fun delete(id: Long)
    fun getAll(pageable: Pageable): Page<PositionAdminResponse>
    fun getAllForConsole(orgId: Long?, pageable: Pageable): Page<PositionResponse>
    fun getPositionEmployees(pageable: Pageable): Page<PositionEmployeesResponse>
    fun getPositionList(departmentId: Long): PositionContentResponse
}

@Service
class PositionServiceImpl(
    private val repository: PositionRepository,
    private val organizationRepository: OrganizationRepository,
    private val permissionRepository: PermissionRepository,
    private val jdbcTemplate: JdbcTemplate,
    private val employeeRepository: EmployeeRepository,
    private val objectMapper: ObjectMapper,
    private val departmentRepository: DepartmentRepository,
    private val validationService: ValidationService,
    private val extraService: ExtraService,
) : PositionService {

    @Transactional
    override fun adminCreate(request: PositionAdminRequest): PositionResponse {
        val organization = organizationRepository.findByIdAndDeletedFalse(request.organizationId) ?: throw OrganizationNotFoundException()
        val permissions = permissionRepository.findAllByIdInAndDeletedFalse(request.permission)
        val newPosition = repository.save(
            Position(
                request.name,
                request.level,
                organization,
                permissions
            )
        )
        return PositionResponse.toDto(newPosition, OrgResponse.toDto(organization))
    }

    @Transactional
    override fun adminUpdate(request: PositionAdminRequest, id: Long): PositionAdminResponse {
        val position = repository.findByIdAndDeletedFalse(id) ?: throw PositionNotFoundException()
        val employeesByPosition = employeeRepository.findAllByPositionAndDeletedFalse(position)
        val permissions = permissionRepository.findAllByIdInAndDeletedFalse(request.permission)

        position.apply {
            this.name = request.name
            this.level = request.level
            this.permission = permissions
        }
        repository.save(position)

        updateEmployeesPermission(employeesByPosition, permissions)
        val employeesAmount = employeeAmount(position.id!!, position.organization.id!!)
        return PositionAdminResponse.toDto(position, employeesAmount, OrgResponse.toDto(position.organization))
    }

    override fun adminDelete(id: Long) {
        repository.findByIdAndDeletedFalse(id)?.let {
            if(employeeRepository.existsByPositionIdAndDeletedFalse(id))
                throw AccessDeniedException("Access denied")
            repository.trash(id)
        } ?: throw PositionNotFoundException()
    }

    @Transactional
    override fun create(request: PositionRequest): PositionResponse {
        val organization = extraService.getOrgFromCurrentUser()
        val permissions = permissionRepository.findAllByIdInAndDeletedFalse(request.permissions)

        val savePosition = repository.save(
            Position(
                request.name,
                request.level,
                organization,
                permissions
            )
        )
        return PositionResponse.toDto(savePosition, OrgResponse.toDto(organization))
    }

    override fun getOneById(id: Long) =
        repository.findByIdAndDeletedFalse(id)?.let {
            val employeeAmount = employeeAmount(it.id!!, it.organization.id!!)
            PositionAdminResponse.toDto(it, employeeAmount, OrgResponse.toDto(it.organization))
        } ?: throw PositionNotFoundException()

    @Transactional
    override fun update(id: Long, request: PositionRequest): PositionAdminResponse {
        val position = repository.findByIdAndDeletedFalse(id) ?: throw PositionNotFoundException()
        validationService.validateDifferentOrganizations(position.organization, extraService.getOrgFromCurrentUser())

        val empPosition = employeeRepository.findAllByPositionAndDeletedFalse(position)
        val employee = mutableListOf<Employee>()
        val permissions = permissionRepository.findAllByIdInAndDeletedFalse(request.permissions)

        position.apply {
            name = request.name
            level = request.level
            permission = permissions
        }
        repository.save(position)

        empPosition.forEach { emp ->
            employee.add(emp.apply { this.permissions = permissions })
        }
        employeeRepository.saveAll(employee)
        val employeeAmount = employeeAmount(position.id!!, position.organization.id!!)
        return PositionAdminResponse.toDto(position, employeeAmount, OrgResponse.toDto(position.organization))
    }


    @Transactional
    override fun delete(id: Long) {
        repository.findByIdAndDeletedFalse(id)?.let {
            if (employeeRepository.existsByPositionIdAndDeletedFalse(id))
                throw AccessDeniedException("Access denied")
            validationService.validateDifferentOrganizations(
                it.organization,
                extraService.getOrgFromCurrentUser()
            )
            repository.trash(id)
        } ?: throw PositionNotFoundException()
    }

    override fun getAll(pageable: Pageable): Page<PositionAdminResponse> {
        val organization = extraService.getOrgFromCurrentUser()
        val countQuery = """
            select count(*) 
            from position p
            where p.organization_id = ${organization.id}
              and p.deleted = false
        """.trimIndent()
        val count = jdbcTemplate.queryForObject(countQuery, Long::class.java)!!

        val query = """
        select p.id                                                  as id,
               p.name                                                as position_name,
               p.level                                               as position_level,
               count(e.id)                                           as total_amount,
               coalesce((select jsonb_agg(
                                jsonb_build_object(
                                        'id', in_p.id,
                                        'name', in_p.permission_data
                                ) order by in_p.id desc
                                         ) filter (where in_p.id is not null)
                         from permission in_p
                                  join position_permission in_pp on in_p.id = in_pp.permission_id
                         where in_pp.position_id = p.id
                           and in_p.deleted = false),
                        '[]'::jsonb)                                 as permissions
        from position p
                 left join employee e on p.id = e.position_id and e.deleted = false
        where p.organization_id = ${organization.id!!}
          and p.deleted = false
        group by p.id
        limit ${pageable.pageSize} offset ${pageable.offset}  
        """.trimIndent()

        val positions = jdbcTemplate.query(query) { rs, _ ->
            PositionAdminResponse(
                rs.getLong("id"),
                rs.getString("position_name"),
                Level.valueOf(rs.getString("position_level")),
                rs.getLong("total_amount"),
                objectMapper.readValue<MutableList<PermissionResponse>>(
                    rs.getString("permissions")
                ).map {
                    PermissionResponse(
                        it.id,
                        it.name
                    )
                },
                OrgResponse.toDto(organization)
            )
        }
        return PageImpl(positions, pageable, count)
    }

    override fun getAllForConsole(orgId: Long?, pageable: Pageable): Page<PositionResponse> {
        val organization = orgId?.let {organizationRepository.findByIdAndDeletedFalse(orgId)
            ?: throw OrganizationNotFoundException()
        }
        return repository.findAllByOrganizationIdAndDeletedFalseOrderByIdDesc(organization?.id, pageable)
            .map {
                PositionResponse.toDto(it, OrgResponse.toDto(it.organization))
            }
    }

    override fun getPositionEmployees(pageable: Pageable): Page<PositionEmployeesResponse> {
        val organization = extraService.getOrgFromCurrentUser()

        val query = """
        select p.id,
               p.name,
               (select count(distinct e.id)
                from employee e
                where e.ph_status = 'BUSY'
                  and e.position_id = p.id)
                  as employees
        from position as p
        where p.organization_id = ${organization.id}
        limit ${pageable.pageSize} offset ${pageable.offset}  
        """.trimIndent()


        val rowMapper = RowMapper<PositionEmployeesResponse> { rs, _ ->
            PositionEmployeesResponse(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getInt("employees")
            )
        }
        val positionEmployees = jdbcTemplate.query(query, rowMapper)
        val countQuery = """
            select count(*) from position p
            where p.organization_id = ${organization.id}
        """.trimIndent()
        val count = jdbcTemplate.queryForObject(countQuery, Long::class.java)!!
        return PageImpl(positionEmployees, pageable, count)!!

    }

    private fun employeeAmount(positionId: Long, organizationId: Long): Long {
        val countQuery = """
                  select count(e.id)
                  from employee e
                  where e.deleted = false
                    and e.organization_id = $organizationId
                    and e.ph_status = 'BUSY'
                    and e.user_id is not null
                    and e.position_id = $positionId
        """.trimIndent()
        return jdbcTemplate.queryForObject(countQuery, Long::class.java)!!
    }

    override fun getPositionList(departmentId: Long): PositionContentResponse {
        val organization = extraService.getOrgFromCurrentUser()
        var department = departmentRepository.findByIdAndDeletedFalse(departmentId)
            ?: throw DepartmentNotFoundException()
        validationService.validateDifferentOrganizations(organization, department)

        val query = """
        select p.id                                                  as id,
               p.name                                                as position_name,
               p.level                                               as position_level,
               count(e.id)                                           as total_amount,
               count(case when e.ph_status = 'VACANT' then e.id end) as vacant_amount,
               count(case when e.ph_status = 'BUSY' then e.id end)   as busy_amount,
               coalesce((select distinct jsonb_agg(
                                         jsonb_build_object(
                                                 'id', in_p.id,
                                                 'name', in_p.permission_data
                                         ) order by in_p.id desc
                                         ) filter (where in_p.id is not null)
                         from permission in_p
                                  join position_permission in_pp on in_p.id = in_pp.permission_id
                         where in_pp.position_id = p.id
                           and in_p.deleted = false),
                        '[]'::jsonb)                                 as permissions
        from position p
                 left join employee e on p.id = e.position_id and e.deleted = false
        where p.organization_id = ${organization.id!!}
          and p.deleted = false
          and e.department_id = $departmentId
        group by p.id
        """.trimIndent()

        val content = jdbcTemplate.query(query) { rs, _ ->
            PositionDto(
                rs.getLong("id"),
                rs.getString("position_name"),
                Level.valueOf(rs.getString("position_level")),
                rs.getLong("total_amount"),
                rs.getLong("vacant_amount"),
                rs.getLong("busy_amount")
            )
        }
        return PositionContentResponse(
            structures(department),
            content = content
        )
    }

    private fun structures(department: Department): List<Structure> {
        val structureList = mutableListOf(
            Structure(
                department.id!!,
                department.name,
                StructureType.DEPARTMENT
            )
        )

        var parentDepartment = department.parentDepartment
        while (parentDepartment != null) {
            structureList.add(
                Structure(
                    parentDepartment.id!!,
                    parentDepartment.name,
                    StructureType.DEPARTMENT,
                    true
                )
            )
            parentDepartment = parentDepartment.parentDepartment
        }
        return structureList
    }
    private fun updateEmployeesPermission(employees: List<Employee>, permissions: MutableSet<Permission> ) {
        val newEmployees = mutableListOf<Employee>()
        employees.forEach { employee ->
            newEmployees.add(employee.apply {this.permissions = permissions})
        }
        employeeRepository.saveAll(newEmployees)
    }
}