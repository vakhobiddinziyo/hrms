package itpu.uz.itpuhrms.services

import itpu.uz.itpuhrms.*
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service


interface UserOrgSessionService {
    fun getSession(organizationId: Long?): UserSessionResponse
    fun getSessionInfo(id: String): UserSessionResponse
}

@Service
class UserOrgSessionServiceImpl(
    private val repository: UserOrgSessionRepository,
    private val userOrgStoreRepository: UserOrgStoreRepository,
) : UserOrgSessionService {

    override fun getSession(organizationId: Long?): UserSessionResponse {
        val userId = userId()
        val userOrgStore = userOrgStore(userId, organizationId)

        repository.findFirstByUserIdAndOrganizationIdOrderByCreatedDateDesc(userId, userOrgStore.organization.id!!)
            ?.let {
                it.role = role(userOrgStore)
                return UserSessionResponse.toResponse(repository.save(it), userOrgStore.organization)
            }

        val newSession = UserOrgSession(userId, userOrgStore.organization.id!!, role(userOrgStore))
        return UserSessionResponse.toResponse(repository.save(newSession), userOrgStore.organization)
    }

    override fun getSessionInfo(id: String): UserSessionResponse {
        return repository.findByIdOrNull(id)?.let {
            val userOrgStore =
                userOrgStoreRepository.findByUserIdAndOrganizationIdAndDeletedFalse(userId(), it.organizationId)
                    ?: throw OrganizationNotFoundException()
            UserSessionResponse.toResponse(it, userOrgStore.organization)
        } ?: throw UserSessionNotFoundException()
    }

    private fun role(store: UserOrgStore): Role {
        return if (store.role == Role.ORG_ADMIN && store.granted) Role.ORG_ADMIN else Role.USER
    }

    private fun userOrgStore(userId: Long, organizationId: Long?): UserOrgStore {
        return organizationId?.let {
            userOrgStoreRepository.findByUserIdAndOrganizationIdAndDeletedFalse(userId, organizationId)
        } ?: userOrgStoreRepository.findFirstByUserIdAndDeletedFalseOrderById(userId)
        ?: throw OrganizationNotFoundException()
    }
}