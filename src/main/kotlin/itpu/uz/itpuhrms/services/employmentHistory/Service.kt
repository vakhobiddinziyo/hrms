package itpu.uz.itpuhrms.services.employmentHistory

import itpu.uz.itpuhrms.*
import itpu.uz.itpuhrms.services.ExtraService
import itpu.uz.itpuhrms.services.employee.EmployeeRepository
import itpu.uz.itpuhrms.services.file.FileAssetRepository
import itpu.uz.itpuhrms.services.user.UserCredentialsRepository
import itpu.uz.itpuhrms.services.user.UserRepository
import itpu.uz.itpuhrms.services.userOrgStore.UserOrgStoreRepository
import itpu.uz.itpuhrms.services.validation.ValidationService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

interface EmploymentHistoryService {
    fun hire(request: EmploymentHistoryRequest): EmploymentHistoryResponse
    fun dismiss(employeeId: Long): EmploymentHistoryResponse
    fun getUserHistory(userId: Long, pageable: Pageable): Page<EmploymentHistoryResponse>
    fun getOne(id: Long): EmploymentHistoryResponse
}

@Service
class EmploymentHistoryServiceImpl(
    private val userRepository: UserRepository,
    private val employeeRepository: EmployeeRepository,
    private val userCredentialsRepository: UserCredentialsRepository,
    private val repository: UserEmploymentHistoryRepository,
    private val validationService: ValidationService,
    private val fileAssetRepository: FileAssetRepository,
    private val userOrgStoreRepository: UserOrgStoreRepository,
    private val extraService: ExtraService,
) : EmploymentHistoryService {

    @Transactional
    override fun hire(request: EmploymentHistoryRequest): EmploymentHistoryResponse {
        return request.run {
            val userCredentials = userCredentialsRepository.findByPinflAndDeletedFalse(userPinfl)
                ?: throw UserCredentialsNotFoundException()

            val file = fileAssetRepository.findByHashIdAndDeletedFalse(imageHashId) ?: throw FileNotFoundException()

            // validationService.validateEmployeeImage(file)
            validationService.validateImageTypeAndSize(file)

            val history = updateEmployee(employeeId, userCredentials.user, file)
            EmploymentHistoryResponse.toResponse(history)
        }
    }

    @Transactional
    override fun dismiss(employeeId: Long): EmploymentHistoryResponse {
        val employee = employeeRepository.findByIdAndDeletedFalse(employeeId)
            ?: throw EmployeeNotFoundException()
        if (employee.phStatus == PositionHolderStatus.VACANT)
            throw EmployeeIsVacantException()

        validationService.validateDifferentOrganizations(
            employee.organization,
            extraService.getOrgFromCurrentUser()
        )

        val user = employee.user!!
        val history = history(user, employee.organization)
        val store = store(user, employee.organization)

        if (user.role != Role.ORG_ADMIN) {
            userOrgStoreRepository.trash(store.id!!)
                ?: throw UserOrgStoreNotFoundException()
        }

        employeeRepository.save(employee.apply {
            this.user = null
            this.imageAsset = null
            this.phStatus = PositionHolderStatus.VACANT
            this.permissions.clear()
            this.permissions.addAll(position.permission)
        })

        return EmploymentHistoryResponse.toResponse(
            repository.save(
                history.apply {
                    this.dismissedDate = Date()
                }
            )
        )
    }

    override fun getUserHistory(userId: Long, pageable: Pageable): Page<EmploymentHistoryResponse> {
        val user = userRepository.findByIdAndDeletedFalse(userId) ?: throw UserNotFoundException()
        return repository.findAllByUserIdAndDeletedFalse(user.id!!, pageable).map {
            EmploymentHistoryResponse.toResponse(it)
        }
    }

    override fun getOne(id: Long) = repository.findByIdAndDeletedFalse(id)?.let {
        EmploymentHistoryResponse.toResponse(it)
    } ?: throw EmploymentHistoryNotFoundException()

    private fun updateEmployee(employeeId: Long, user: User, file: FileAsset): UserEmploymentHistory {
        val employee = employeeRepository.findByIdAndDeletedFalse(employeeId) ?: throw EmployeeNotFoundException()
        val organization = employee.organization

        validationService.validateDifferentOrganizations(organization, extraService.getOrgFromCurrentUser())
        validationService.validateExistingEmployee(user, organization)

        val exists = userOrgStoreRepository.existsByUserIdAndOrganizationIdAndDeletedFalse(user.id!!, organization.id!!)

        //if (!exists && user.role == Role.ORG_ADMIN) throw UserOrgStoreNotFoundException()
        if (employee.phStatus != PositionHolderStatus.VACANT) throw EmployeeIsBusyException()

        employeeRepository.save(employee.apply {
            this.user = user
            this.phStatus = PositionHolderStatus.BUSY
            this.permissions.clear()
            this.permissions.addAll(position.permission)
            this.imageAsset = file
        })

        if (!exists) {
            userOrgStoreRepository.save(UserOrgStore(user, organization, Role.USER, false))
        }

        return repository.save(
            UserEmploymentHistory(
                employee,
                user,
                employee.position,
                employee.department,
                Date()
            )
        )
    }

    private fun history(user: User, organization: Organization): UserEmploymentHistory {
        return repository.findTopByUserIdAndDepartmentOrganizationIdAndDeletedFalseOrderByIdDesc(
            user.id!!, organization.id!!
        ) ?: throw EmploymentHistoryNotFoundException()
    }

    private fun store(user: User, organization: Organization): UserOrgStore {
        return userOrgStoreRepository.findByOrganizationIdAndUserIdAndDeletedFalse(
            organization.id!!,
            user.id!!
        ) ?: throw UserOrgStoreNotFoundException()
    }
}