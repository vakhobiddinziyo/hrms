package itpu.uz.itpuhrms.services

import itpu.uz.itpuhrms.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


interface DepartmentService {
    fun create(request: DepartmentRequest): DepartmentResponse
    fun update(id: Long, request: DepartmentRequest): DepartmentResponse
    fun delete(id: Long)
    fun getListForStructure(parentId: Long?): DepartmentContentResponse
    fun getListForStructureByParent(parentId: Long?): DepartmentContentResponse
    fun getById(id: Long): DepartmentResponse
    fun getPage(pageable: Pageable): Page<DepartmentResponse>
    fun getListByType(type: DepartmentType, orgId: Long): List<DepartmentResponse>
}


@Service
class DepartmentServiceImpl(
    private val repository: DepartmentRepository,
    private val organizationRepository: OrganizationRepository,
    private val validationService: ValidationService,
    private val jdbcTemplate: JdbcTemplate,
    private val extraService: ExtraService,
    private val employeeRepository: EmployeeRepository,
    private val departmentRepository: DepartmentRepository,
) : DepartmentService {
    @Transactional
    override fun create(request: DepartmentRequest): DepartmentResponse {
        val organization = extraService.getOrgFromCurrentUser()
        val headDepartment =
            repository.findByOrganizationIdAndDepartmentTypeAndDeletedFalse(organization.id!!, DepartmentType.HEAD)
                ?: throw DepartmentNotFoundException()

        val parentDepartment = request.parentDepartmentId?.let {
            val parent = repository.findByIdAndDeletedFalse(it) ?: throw DepartmentNotFoundException()
            validationService.validateDifferentOrganizations(organization, parent.organization)
            parent
        }
//        if (role() != Role.DEVELOPER && role() != Role.ADMIN) {
//            val employee = employeeRepository.findByIdAndDeletedFalse(userId()) ?: throw EmployeeNotFoundException()
//            parentDepartment = employee.department
//        }
        if (request.departmentType == DepartmentType.HEAD) throw org.springframework.security.access.AccessDeniedException("Access denied")
        val department = Department(
            request.name,
            request.description,
            request.departmentType,
            organization,
            parentDepartment, // department qo'shyotgan xodim departmenti yangi qo'shilyotgan departmentga parent bo'lib tushadi.
            headDepartment
        )
        val savedDepartment = repository.save(department)
        return DepartmentResponse.toDto(savedDepartment)
    }

    @Transactional
    override fun update(id: Long, request: DepartmentRequest): DepartmentResponse {
        val department = repository.findByIdAndDeletedFalse(id) ?: throw DepartmentNotFoundException()
        val sessionOrg = extraService.getOrgFromCurrentUser()
        validationService.validateDifferentOrganizations(department.organization, sessionOrg)

        val parentDepartment = request.parentDepartmentId?.let {
            val parent = repository.findByIdAndDeletedFalse(it) ?: throw DepartmentNotFoundException()
            if (parent.id == department.id) throw itpu.uz.itpuhrms.AccessDeniedException()
            validationService.validateDifferentOrganizations(sessionOrg, parent.organization)
            parent
        }

        department.apply {
            this.name = request.name
            this.description = request.description
            this.departmentType = request.departmentType
            this.parentDepartment = parentDepartment
        }
        val savedDepartment = repository.save(department)
        return DepartmentResponse.toDto(savedDepartment)
    }

    @Transactional
    override fun delete(id: Long) {
        repository.findByIdAndDeletedFalse(id)?.let {
            if (it.departmentType == DepartmentType.HEAD)
                throw itpu.uz.itpuhrms.AccessDeniedException()
            if (employeeRepository.existsByDepartmentIdAndDeletedFalse(id))
                throw itpu.uz.itpuhrms.AccessDeniedException()
            if (repository.existsByParentDepartmentIdAndDeletedFalse(id))
                throw itpu.uz.itpuhrms.AccessDeniedException()

            validationService.validateDifferentOrganizations(
                it.organization,
                extraService.getOrgFromCurrentUser()
            )

            repository.trash(it.id!!)
        } ?: throw DepartmentNotFoundException()
    }

    override fun getById(id: Long) = repository.findByIdAndDeletedFalse(id)?.let {
        DepartmentResponse.toDto(it)
    } ?: throw DepartmentNotFoundException()

    override fun getPage(pageable: Pageable) =
        repository.findAllByOrganizationIdAndDeletedFalseOrderByIdDesc(
            extraService.getOrgFromCurrentUser().id!!,
            pageable
        ).map { DepartmentResponse.toDto(it) }

    override fun getListForStructure(parentId: Long?): DepartmentContentResponse {
        val organization = extraService.getOrgFromCurrentUser()
        val structureList = mutableListOf<Structure>()

        parentId?.let {
            val parent = departmentRepository.findByIdAndDeletedFalse(it)
            parent?.let { dept ->
                structureList.add(
                    Structure(
                        dept.id!!,
                        dept.name,
                        StructureType.DEPARTMENT,
                        true
                    )
                )
            }
        }

        val whereClause = if (parentId == null) {
            "where dh.parent_id is null"
        } else {
            "where dh.parent_id = ${parentId}"
        }

        val query = """
        WITH RECURSIVE department_hierarchy AS (
            SELECT
                d.id AS department_id,
                d.name,
                d.description,
                d.department_type,
                d.parent_department_id AS parent_id,
                COUNT(e.id) AS total_employee,
                COUNT(CASE WHEN e.ph_status = 'VACANT' THEN e.id END) AS vacant_employee,
                COUNT(CASE WHEN e.ph_status = 'BUSY' THEN e.id END) AS busy_employee
            FROM department d
            LEFT JOIN employee e ON d.id = e.department_id AND e.deleted = false
            WHERE d.organization_id = ${organization.id!!} AND d.deleted = false
            GROUP BY d.id, d.name, d.description, d.department_type, d.parent_department_id
            UNION ALL
            SELECT
                d.id AS department_id,
                d.name,
                d.description,
                d.department_type,
                d.parent_department_id AS parent_id,
                dh.total_employee,
                dh.vacant_employee,
                dh.busy_employee
            FROM department d
            JOIN department_hierarchy dh ON d.id = dh.parent_id
        )
        SELECT 
            dh.department_id AS id,
            dh.name,
            dh.description,
            dh.department_type,
            SUM(dh.total_employee) AS total_employee,
            SUM(dh.vacant_employee) AS vacant_employee,
            SUM(dh.busy_employee) AS busy_employee,
            EXISTS (SELECT 1 FROM department d WHERE d.parent_department_id = dh.department_id) AS has_child
        FROM department_hierarchy dh
        $whereClause
        GROUP BY dh.department_id, dh.name, dh.description, dh.department_type
        ORDER BY dh.department_id;
    """.trimIndent()

        val content = jdbcTemplate.query(query) { rs, _ ->
            DepartmentAdminResponse(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("description"),
                DepartmentType.valueOf(rs.getString("department_type")),
                rs.getLong("total_employee"),
                rs.getLong("vacant_employee"),
                rs.getLong("busy_employee"),
                rs.getBoolean("has_child")
            )
        }

        return DepartmentContentResponse(
            structureList.toList(),
            content
        )
    }

    override fun getListForStructureByParent(parentId: Long?): DepartmentContentResponse {
        val organization = extraService.getOrgFromCurrentUser()
        val structureList = mutableListOf<Structure>()

        parentId?.let {
            var parent = departmentRepository.findByIdAndDeletedFalse(it)
            while (parent != null) {
                structureList.add(
                    Structure(
                        parent.id!!,
                        parent.name,
                        StructureType.DEPARTMENT,
                        true
                    )
                )
                parent = parent.parentDepartment
            }
        }

        val query = """
        WITH RECURSIVE department_hierarchy AS (
            SELECT
                d.id AS department_id,
                d.name,
                d.description,
                d.department_type,
                d.parent_department_id AS parent_id,
                COUNT(e.id) AS total_employee,
                COUNT(CASE WHEN e.ph_status = 'VACANT' THEN e.id END) AS vacant_employee,
                COUNT(CASE WHEN e.ph_status = 'BUSY' THEN e.id END) AS busy_employee
            FROM department d
            LEFT JOIN employee e ON d.id = e.department_id AND e.deleted = false
            WHERE d.organization_id = ${organization.id!!} AND d.deleted = false
            GROUP BY d.id, d.name, d.description, d.department_type, d.parent_department_id
            UNION ALL
            SELECT
                d.id AS department_id,
                d.name,
                d.description,
                d.department_type,
                d.parent_department_id AS parent_id,
                dh.total_employee,
                dh.vacant_employee,
                dh.busy_employee
            FROM department d
            JOIN department_hierarchy dh ON d.id = dh.parent_id
        )
        SELECT 
            dh.department_id AS id,
            dh.name,
            dh.description,
            dh.department_type,
            SUM(dh.total_employee) AS total_employee,
            SUM(dh.vacant_employee) AS vacant_employee,
            SUM(dh.busy_employee) AS busy_employee,
            EXISTS (SELECT 1 FROM department d WHERE d.parent_department_id = dh.department_id) AS has_child
        FROM department_hierarchy dh
        WHERE dh.parent_id IS NULL
        GROUP BY dh.department_id, dh.name, dh.description, dh.department_type
        ORDER BY dh.department_id;
    """.trimIndent()

        val content = jdbcTemplate.query(query) { rs, _ ->
            DepartmentAdminResponse(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("description"),
                DepartmentType.valueOf(rs.getString("department_type")),
                rs.getLong("total_employee"),
                rs.getLong("vacant_employee"),
                rs.getLong("busy_employee"),
                rs.getBoolean("has_child")
            )
        }

        return DepartmentContentResponse(
            structureList.toList(),
            content
        )
    }


    override fun getListByType(type: DepartmentType, orgId: Long): List<DepartmentResponse> {
    val organization = organizationRepository.findByIdAndDeletedFalse(orgId)
        ?: throw OrganizationNotFoundException()
    return repository.findAllByOrganizationIdAndDepartmentTypeAndDeletedFalse(organization.id!!, type).map {
        DepartmentResponse.toDto(it)
    }
}
}
