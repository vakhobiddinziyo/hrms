package itpu.uz.itpuhrms.services.userOrgSession

import itpu.uz.itpuhrms.UserOrgSession
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository


@Repository
interface UserOrgSessionRepository : MongoRepository<UserOrgSession, String> {
    fun findFirstByUserIdAndOrganizationIdOrderByCreatedDateDesc(userId: Long, organizationId: Long): UserOrgSession?
}
