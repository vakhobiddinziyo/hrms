package itpu.uz.itpuhrms.services

import itpu.uz.itpuhrms.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*
import org.springframework.security.access.AccessDeniedException

interface UserService {
    fun createUser(request: UserAdminRequest): UserAdminResponse // it is for admin
    fun getOneByIdAdmin(id: Long): UserAdminResponse
    fun updateUser(id: Long, request: UserAdminUpdateRequest): UserAdminResponse
    fun getAll(role: Role?, search: String?, pageable: Pageable): Page<UserResponse>
    fun getByPinfl(request: PinflRequest): UserResponse
    fun createClient(request: UserRequest): UserResponse
    fun getOneById(id: Long): UserResponse
    fun update(id: Long, request: UserUpdateRequest): UserResponse
    fun delete(id: Long)
    fun userMe(): UserMeResponse
    fun getAllClients(search: String?, gender: Gender?, pageable: Pageable): Page<UserResponse>
    fun getAllRoles(): List<Role>
    fun saveOrgAdminCredentials(request: UserCredentialsRequest): UserCredentialsResponse
}


@Service
class UserServiceImpl(
    private val repository: UserRepository,
    private val credentialRepository: UserCredentialsRepository,
    private val passwordEncoder: PasswordEncoder,
    private val fileRepository: FileAssetRepository,
    private val employeeRepository: EmployeeRepository,
    private val userOrgStoreRepository: UserOrgStoreRepository,
    private val phoneNumberConfiguration: PhoneNumberConfiguration,
    private val organizationRepository: OrganizationRepository,
    private val sessionRepository: UserOrgSessionRepository,
    private val extraService: ExtraService,
) : UserService {
    override fun getByPinfl(request: PinflRequest) =
        credentialRepository.findByPinfl(request.pinfl)?.let {
            val organization = extraService.getOrgFromCurrentUser()
            if (employeeRepository.existsByUserIdAndOrganizationIdAndDeletedFalse(it.user.id!!, organization.id!!))
                throw EmployeeExistsWithinOrganizationException()
            UserResponse.toDto(it.user, UserCredentialsResponse.toDto(it))
        } ?: throw UserNotFoundException()

    @Transactional
    override fun createUser(request: UserAdminRequest): UserAdminResponse {
        val role = validateRequestRole(request)
        val avatarPhoto = validateRequestPhoto(request)
        val validPhoneNumber = phoneNumberConfiguration.validatePhoneNumber(request.phoneNumber)

        if (repository.existsByUsername(request.username)) throw UsernameAlreadyExistException()

        val user = repository.save(
            User(
                request.fullName,
                validPhoneNumber,
                request.username,
                passwordEncoder.encode(request.password),
                request.status,
                request.mail,
                role,
                avatarPhoto
            )
        )

        val credentials = validateAndSaveCredentials(request, user)
        val list = saveUserOrgStores(request, user)
        return UserAdminResponse.toDto(user, list, credentials)
    }

    @Transactional
    override fun createClient(request: UserRequest): UserResponse {
        if (repository.existsByUsername(request.username)) throw UsernameAlreadyExistException()
        val avatarPhoto = validateRequestPhoto(request)
        val validPhoneNumber = phoneNumberConfiguration.validatePhoneNumber(request.phoneNumber)
        val user = repository.save(request.toEntity(validPhoneNumber, passwordEncoder, avatarPhoto))
        val credentials = validateAndSaveCredentials(request.credentials, user)
        return UserResponse.toDto(user, UserCredentialsResponse.toDto(credentials))
    }

    override fun getOneById(id: Long) =
        repository.findByIdAndDeletedFalse(id)?.let { user ->
            credentialRepository.findByUserIdAndDeletedFalse(id)?.let {
                UserResponse.toDto(user, UserCredentialsResponse.toDto(it))
            } ?: UserResponse.toDto(user, null)
        } ?: throw UserNotFoundException()

    override fun getOneByIdAdmin(id: Long): UserAdminResponse {
        val user = repository.findByIdAndDeletedFalse(id) ?: throw UserNotFoundException()
        val userOrgStores = userOrgStoreRepository.findAllByUserAndDeletedFalse(user)
        val list = userOrgStores.map {
            OrgAdminResponse.toDto(it.organization, it)
        }
        val credentials = credentialRepository.findByUserIdAndDeletedFalse(id)
        return UserAdminResponse.toDto(user, list, credentials)
    }

    override fun getAll(role: Role?, search: String?, pageable: Pageable) =
        repository.findAllUser(role?.name, search, pageable)
            .map { UserResponse.toDto(it, null) }

    @Transactional
    override fun update(id: Long, request: UserUpdateRequest) =
        repository.findByIdAndDeletedFalse(id)?.let { user ->
            request.run {
                if (role() == Role.USER && id != userId() || user.role !in mutableListOf(Role.USER, Role.ORG_ADMIN))
                    throw AccessDeniedException("Access denied!")
                if (role() == Role.ORG_ADMIN && credentials != null)
                    throw AccessDeniedException("Access denied!")

                phoneNumber?.let { user.phoneNumber = phoneNumberConfiguration.validatePhoneNumber(it) }
                fullName?.let { user.fullName = it }
                username?.let {
                    if (repository.existsByUsername(it) && it != user.username) throw UsernameAlreadyExistException()
                    user.username = it
                }
                mail?.let { user.mail = it }
                avatarPhoto?.let { req ->
                    user.avatarPhoto = updateAvatarPhoto(user, req)
                }
                password?.let { password ->
                    user.password = passwordEncoder.encode(password)
                }
                repository.save(user)
                val userCredentials = if (role() in mutableListOf(Role.DEVELOPER, Role.ADMIN)) {
                    credentials?.let { validateAndUpdateCredentials(it, id) }
                } else null
                UserResponse.toDto(user, userCredentials?.let { UserCredentialsResponse.toDto(it) })
            }
        } ?: throw UserNotFoundException()

    @Transactional
    override fun updateUser(id: Long, request: UserAdminUpdateRequest): UserAdminResponse {

        return repository.findByIdAndDeletedFalse(id)?.let { user ->
            validateRequestRole(user, request)
            if (repository.existsByUsername(request.username) && request.username != user.username)
                throw UsernameAlreadyExistException()

            val validPhoneNumber = phoneNumberConfiguration.validatePhoneNumber(request.phoneNumber)

            request.let { req ->
                user.fullName = req.fullName
                user.phoneNumber = validPhoneNumber
                user.mail = req.mail
                user.username = req.username
                user.status = req.status
                req.avatarPhoto?.let {
                    user.avatarPhoto = updateAvatarPhoto(user, req.avatarPhoto)
                }
                req.password?.let {
                    user.password = passwordEncoder.encode(it)
                }
                req.role?.let {
                    user.role = it
                }
                repository.save(user)
            }

            val credentials = request.credentials?.let {
                validateAndUpdateCredentials(it, user.id!!)
            }

            val list = updateUserOrgStores(request, user)
            return UserAdminResponse.toDto(user, list, credentials)
        } ?: throw UserNotFoundException()
    }

    @Transactional
    override fun delete(id: Long) {
        if (employeeRepository.existsByUserIdAndPhStatusAndDeletedFalse(id, PositionHolderStatus.BUSY))
            throw EmployeeExistsWithinOrganizationException()
        repository.trash(id) ?: UserNotFoundException()
    }

    override fun userMe(): UserMeResponse {

        val user = repository.findByIdAndDeletedFalse(userId()) ?: throw UserNotFoundException()
        val userId = user.id ?: throw IllegalArgumentException("User ID cannot be null") //todo exception to'girlash kk

        return if (user.role == Role.ADMIN || user.role == Role.DEVELOPER) {
            UserMeResponse(userId, user.username, user.fullName, user.role)
        } else {
            val session = sessionRepository.findByIdOrNull(getOSession())
                ?: throw UserSessionNotFoundException()
            val store = userOrgStoreRepository.findByOrganizationIdAndUserIdAndDeletedFalse(
                session.organizationId,
                session.userId
            ) ?: throw UserOrgStoreNotFoundException()

            val organization = organizationRepository.findByIdAndDeletedFalse(session.organizationId)
                ?: throw OrganizationNotFoundException()

            val employee =
                if (session.role == Role.ORG_ADMIN) {
                    employeeRepository.findByUserIdAndOrganizationIdAndDeletedFalse(userId, session.organizationId)
                } else employeeRepository.findByUserIdAndOrganizationIdAndDeletedFalse(userId, session.organizationId)
                    ?: throw EmployeeNotFoundException()


            UserMeResponse(
                userId,
                user.username,
                user.fullName,
                user.role,
                employee != null && employee.status == Status.ACTIVE,
                store.granted,
                UserSessionResponse.toResponse(session, organization),
                UserMeEmployeeResponse.toDto(employee),
                user.avatarPhoto?.hashId
            )
        }
    }

    override fun getAllClients(search: String?, gender: Gender?, pageable: Pageable) =
        credentialRepository.findClientsWithFilter(search, gender?.name, pageable)
            .map { UserResponse.toDto(it.user, UserCredentialsResponse.toDto(it)) }

    override fun getAllRoles(): List<Role> {
        return Role.values().filter { it.name !in listOf(Role.DEVELOPER.name) }
    }

    override fun saveOrgAdminCredentials(request: UserCredentialsRequest): UserCredentialsResponse {
        request.let {
            if (credentialRepository.existsByPinfl(it.pinfl)) throw PinflAlreadyExistException()
            if (credentialRepository.existsByCardSerialNumber(it.cardSerialNumber)) throw SerialNumberAlreadyExistsException()
        }
        val user = repository.findByIdAndDeletedFalse(userId()) ?: throw UserNotFoundException()
        if (user.role != Role.ORG_ADMIN) throw AccessDeniedException("Access denied")
        if (credentialRepository.existsByUserIdAndDeletedFalse(user.id!!)) throw AccessDeniedException()

        return request.let { req ->
            val save = credentialRepository.save(req.toEntity(user))
            UserCredentialsResponse.toDto(save)
        }
    }

    private fun saveUserOrgStores(request: UserAdminRequest, user: User): List<OrgAdminResponse> {
        request.orgStores.forEach { store ->
            val organization = organizationRepository.findByIdAndDeletedFalse(store.orgId)
                ?: throw OrganizationNotFoundException()
            val exists = employeeRepository.existsByUserIdAndOrganizationIdAndDeletedFalse(user.id!!, store.orgId)
            if (!store.granted && !exists) throw EmployeeNotFoundException()
            userOrgStoreRepository.save(UserOrgStore(user, organization, role(store), store.granted))
        }
        val userOrgStores = userOrgStoreRepository.findAllByUserAndDeletedFalse(user)
        return userOrgStores.map { OrgAdminResponse.toDto(it.organization) }
    }

    private fun updateUserOrgStores(request: UserAdminUpdateRequest, user: User): List<OrgAdminResponse> {
        val orgStoreList = mutableListOf<UserOrgStore>()
        val orgStores = userOrgStoreRepository.findAllByUserAndDeletedFalse(user)
        val associateBy = orgStores.associateBy { it.organization.id!! }.toMutableMap()

        request.orgStores.map { req ->
            val exists = employeeRepository.existsByUserIdAndOrganizationIdAndDeletedFalse(user.id!!, req.orgId)
            if (!req.granted && !exists) throw EmployeeNotFoundException()

            associateBy[req.orgId]?.let {
                orgStoreList.add(it.apply {
                    role = role(req)
                    granted = req.granted
                })
                associateBy.remove(req.orgId)
            } ?: run {
                val organization = organizationRepository.findByIdAndDeletedFalse(req.orgId)
                    ?: throw OrganizationNotFoundException()
                orgStoreList.add(UserOrgStore(user, organization, Role.ORG_ADMIN, req.granted))
            }
        }

        associateBy.values.map { store ->
            val exist = employeeRepository.existsByUserIdAndOrganizationIdAndDeletedFalse(
                user.id!!,
                store.organization.id!!
            )
            orgStoreList.add(
                store.apply {
                    this.role = Role.USER
                    this.deleted = !exist
                    this.granted = false
                }
            )
        }
        userOrgStoreRepository.saveAll(orgStoreList)

        val userOrgStores = userOrgStoreRepository.findAllByUserAndRoleAndDeletedFalse(user, Role.ORG_ADMIN)
        return userOrgStores.map {
            OrgAdminResponse.toDto(it.organization)
        }
    }

    private fun updateAvatarPhoto(user: User, newAvatarPhoto: String): FileAsset? {
        newAvatarPhoto.let { newPhoto ->
            if (newPhoto != user.avatarPhoto?.hashId) {
                return fileRepository.findByHashIdAndDeletedFalse(newPhoto)
                    ?: throw FileNotFoundException()
            }
        }
        return user.avatarPhoto
    }

    private fun validateRequestRole(request: UserAdminRequest): Role {
        when {
            request.role in listOf(Role.DEVELOPER, Role.USER) ->
                throw AccessDeniedException("Access denied")

            request.role == Role.ADMIN && (request.orgStores.isNotEmpty() || request.credentials != null) ->
                throw AccessDeniedException("Access denied")

            request.role == Role.ORG_ADMIN && (request.orgStores.isEmpty() || request.credentials == null) ->
                throw AccessDeniedException("Access denied")
        }
        return request.role
    }


    private fun validateRequestRole(user: User, request: UserAdminUpdateRequest) {
        when {
            user.role == Role.DEVELOPER && role() != Role.DEVELOPER ->
                throw AccessDeniedException("Access denied")

            user.role in listOf(Role.ADMIN, Role.DEVELOPER)
                    && (request.orgStores.isNotEmpty() || request.credentials != null) ->
                throw AccessDeniedException("Access denied")

            request.role != null &&
                    (request.role !in listOf(Role.ORG_ADMIN, Role.USER)
                            || user.role !in listOf(Role.ORG_ADMIN, Role.USER)) ->
                throw AccessDeniedException("Access denied")

            request.role == Role.USER && (request.orgStores.isNotEmpty()) ->
                throw AccessDeniedException("Access denied")
        }
    }


    private fun validateRequestPhoto(request: UserAdminRequest): FileAsset? {
        return request.avatarPhoto?.let {
            fileRepository.findByHashIdAndDeletedFalse(it) ?: throw FileNotFoundException()
        }
    }

    private fun validateRequestPhoto(request: UserRequest): FileAsset? {
        return request.avatarPhoto?.let {
            fileRepository.findByHashIdAndDeletedFalse(it) ?: throw FileNotFoundException()
        }
    }

    private fun validateAndSaveCredentials(request: UserAdminRequest, user: User): UserCredentials? {
        return request.credentials?.let {
            if (credentialRepository.existsByPinfl(it.pinfl)) throw PinflAlreadyExistException()
            if (credentialRepository.existsByCardSerialNumber(it.cardSerialNumber)) throw SerialNumberAlreadyExistsException()
            credentialRepository.save(it.toEntity(user))
        }
    }

    private fun validateAndSaveCredentials(credentials: UserCredentialsRequest, user: User): UserCredentials {
        credentials.let {
            if (credentialRepository.existsByPinfl(it.pinfl)) throw PinflAlreadyExistException()
            if (credentialRepository.existsByCardSerialNumber(it.cardSerialNumber)) throw SerialNumberAlreadyExistsException()
            return credentialRepository.save(it.toEntity(user))
        }
    }

    private fun validateAndUpdateCredentials(credentials: UserCredentialsRequest, userId: Long): UserCredentials {
        return credentials.run {
            val userCredentials = credentialRepository.findByUserIdAndDeletedFalse(userId)
                ?: throw UserCredentialsNotFoundException()

            val existsBySerialNumber = credentialRepository.existsByCardSerialNumber(cardSerialNumber)
            if (existsBySerialNumber && userCredentials.cardSerialNumber != cardSerialNumber)
                throw SerialNumberAlreadyExistsException()

            val existsByPinfl = credentialRepository.existsByPinfl(pinfl)
            if (existsByPinfl && userCredentials.pinfl != pinfl) throw PinflAlreadyExistException()

            userCredentials.fio = fio
            userCredentials.gender = gender
            userCredentials.birthday = Date(birthday)
            userCredentials.cardExpireDate = Date(cardExpireDate)
            userCredentials.cardGivenDate = Date(cardGivenDate)
            userCredentials.cardSerialNumber = cardSerialNumber
            userCredentials.pinfl = pinfl
            credentialRepository.save(userCredentials)
        }
    }

    private fun role(store: OrgStoreRequest): Role {
        return if (store.granted) Role.ORG_ADMIN else Role.USER
    }

}

