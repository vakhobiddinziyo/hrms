package itpu.uz.itpuhrms.services.userOrgStore

import itpu.uz.itpuhrms.*
import itpu.uz.itpuhrms.security.userId
import itpu.uz.itpuhrms.services.employee.EmployeeRepository
import itpu.uz.itpuhrms.services.organization.OrganizationRepository
import itpu.uz.itpuhrms.services.user.UserRepository
import itpu.uz.itpuhrms.services.validation.ValidationService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service


interface UserOrgStoreService {
    fun createUserOrgStore(request: UserOrgStoreRequest)
    fun getUserOrganizations(pageable: Pageable): Page<UserOrgStoreResponse>
    fun updateUserOrgStoreRole(orgId: Long)
}

@Service
class UserOrgStoreServiceImpl(
    private val userRepository: UserRepository,
    private val organizationRepository: OrganizationRepository,
    private val repository: UserOrgStoreRepository,
    private val validationService: ValidationService,
    private val employeeRepository: EmployeeRepository,
) : UserOrgStoreService {
    override fun createUserOrgStore(request: UserOrgStoreRequest) {
        // Fetch both user and organization in parallel if supported, or fetch lazily to avoid unnecessary queries
        val user = userRepository.findByIdAndDeletedFalse(request.userId)
            ?: throw UserNotFoundException()
        val organization = organizationRepository.findByIdAndDeletedFalse(request.orgId)
            ?: throw OrganizationNotFoundException()

        if (user.role == Role.USER) validationService.validateOrganizationUser(organization, user)
        if (repository.existsByUserIdAndOrganizationIdAndDeletedFalse(user.id!!, organization.id!!))
            throw UserOrgStoreAlreadyExistException()
        if (user.role !in mutableListOf(Role.USER, Role.ORG_ADMIN)) throw AccessDeniedException("Access denied")

        val granted = if (user.role == Role.ORG_ADMIN) request.granted else false
        repository.save(
            UserOrgStore(
                user = user,
                organization = organization,
                role = role(granted),
                granted
            )
        )
    }

    override fun updateUserOrgStoreRole(orgId: Long) {
        val user = userRepository.findByIdAndDeletedFalse(userId())
            ?: throw UserNotFoundException()
        val store = repository.findByUserIdAndOrganizationIdAndDeletedFalse(user.id!!, orgId)
            ?: throw UserOrgStoreNotFoundException()
        val employeeExist =
            employeeRepository.existsByUserIdAndOrganizationIdAndDeletedFalse(user.id!!, store.organization.id!!)
        if (store.role == Role.ORG_ADMIN && !employeeExist) throw EmployeeNotFoundException()
        repository.save(
            store.apply {
                this.role = role(store, user)
            }
        )
    }


    override fun getUserOrganizations(pageable: Pageable) =
        repository.findAllByUserIdAndDeletedFalseOrderByIdDesc(userId(), pageable)
            .map { UserOrgStoreResponse.toResponse(it) }

    private fun role(store: UserOrgStore, user: User): Role {
        return if (user.role == Role.ORG_ADMIN && store.granted && store.role == Role.USER) Role.ORG_ADMIN else Role.USER
    }

    private fun role(granted: Boolean): Role {
        return if (granted) Role.ORG_ADMIN else Role.USER
    }

}