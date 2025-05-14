package itpu.uz.itpuhrms.services.userOrgStore

import itpu.uz.itpuhrms.Role
import itpu.uz.itpuhrms.User
import itpu.uz.itpuhrms.UserOrgStore
import itpu.uz.itpuhrms.base.BaseRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
interface UserOrgStoreRepository : BaseRepository<UserOrgStore> {
    fun findByOrganizationIdAndUserIdAndDeletedFalse(orgId: Long, userId: Long): UserOrgStore?
    fun existsByUserIdAndOrganizationIdAndDeletedFalse(userId: Long, orgId: Long): Boolean
    fun findAllByUserIdAndDeletedFalseOrderByIdDesc(userId: Long, pageable: Pageable): Page<UserOrgStore>
    fun findAllByUserAndDeletedFalse(user: User): List<UserOrgStore>
    fun findAllByUserAndRoleAndDeletedFalse(user: User, role: Role): List<UserOrgStore>
    fun findByUserIdAndOrganizationIdAndDeletedFalse(userId: Long, organizationId: Long): UserOrgStore?
    fun findFirstByUserIdAndDeletedFalseOrderById(userId: Long): UserOrgStore?
}
