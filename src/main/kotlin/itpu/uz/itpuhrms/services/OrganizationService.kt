package itpu.uz.itpuhrms.services

import itpu.uz.itpuhrms.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalTime

interface OrganizationService {
    fun create(request: OrgRequest): OrgAdminResponse
    fun update(id: Long, request: OrgRequest): OrgAdminResponse
    fun delete(id: Long)
    fun getById(id: Long): OrgAdminResponse
    fun getList(search: String?, status: Status?, active: Boolean?, pageable: Pageable): Page<OrgAdminResponse>
    fun changeOrgActive(id: Long): OrgAdminResponse
    fun getOrganizationStatistics(): StatisticResponse
}


@Service
class OrganizationServiceImpl(
    private val repository: OrganizationRepository,
    private val departmentRepository: DepartmentRepository,
    private val workingDateConfigRepository: WorkingDateConfigRepository,
    private val extraService: ExtraService,
) : OrganizationService {
    @Transactional
    override fun create(request: OrgRequest): OrgAdminResponse {
        if (repository.existsByTinAndDeletedFalse(request.tin)) {
            throw OrganizationAlreadyExistException()
        }
        val organization = repository.save(
            Organization(
                name = request.name,
                description = request.description,
                status = request.status,
                tin = request.tin
            )
        )
        departmentRepository.save(
            Department(
                name = request.name,
                description = request.description,
                departmentType = DepartmentType.HEAD,
                organization = organization
            )
        )
        val workingDateConfigList = mutableListOf<WorkingDateConfig>()

        for (dayOfWeek in 1..5) {
            workingDateConfigList.add(
                WorkingDateConfig(
                    organization,
                    LocalTime.of(9, 0),
                    LocalTime.of(18, 0),
                    8,
                    DayOfWeek.of(dayOfWeek.toShort())
                )
            )
        }

        workingDateConfigRepository.saveAll(workingDateConfigList)


        return OrgAdminResponse.toDto(organization)
    }

    @Transactional
    override fun update(id: Long, request: OrgRequest): OrgAdminResponse {
        val organization = repository.findByIdAndDeletedFalse(id) ?: throw OrganizationNotFoundException()
        val headDepartment = departmentRepository
            .findByOrganizationIdAndDepartmentTypeAndDeletedFalse(organization.id!!, DepartmentType.HEAD)
            ?: throw DepartmentNotFoundException()
        val updatedOrg = repository.save(
            organization.apply {
                name = request.name
                description = request.description
                status = request.status
                tin = request.tin
            }
        )

        headDepartment.apply {
            name = updatedOrg.name
            description = updatedOrg.description
        }
        departmentRepository.save(headDepartment)
        return OrgAdminResponse.toDto(updatedOrg)
    }

    @Transactional
    override fun delete(id: Long) {
        val department =
            departmentRepository.findByOrganizationIdAndDepartmentTypeAndDeletedFalse(id, DepartmentType.HEAD)
                ?: throw DepartmentNotFoundException()
        department.apply { deleted = true }
        departmentRepository.save(department)
        repository.trash(id) ?: throw OrganizationNotFoundException()
    }

    override fun getById(id: Long) = repository.findByIdAndDeletedFalse(id)?.let {
        OrgAdminResponse.toDto(it)
    } ?: throw OrganizationNotFoundException()

    override fun getList(search: String?, status: Status?, active: Boolean?, pageable: Pageable) =
        repository.findAllByFilter(search, status, active, pageable).map { org -> OrgAdminResponse.toDto(org) }

    override fun changeOrgActive(id: Long) =
        repository.findByIdAndDeletedFalse(id)?.let { org ->
            repository.save(org.apply { this.isActive = !this.isActive })
            OrgAdminResponse.toDto(org)
        } ?: throw OrganizationNotFoundException()


    override fun getOrganizationStatistics(): StatisticResponse {
        val organization = extraService.getOrgFromCurrentUser()
        return repository.findOrganizationStatistics(organization.id!!)
    }
}