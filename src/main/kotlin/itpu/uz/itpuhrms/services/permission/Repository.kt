package itpu.uz.itpuhrms.services.permission

import itpu.uz.itpuhrms.Permission
import itpu.uz.itpuhrms.base.BaseRepository
import org.springframework.stereotype.Repository

@Repository
interface PermissionRepository : BaseRepository<Permission> {
    fun findAllByIdInAndDeletedFalse(ids: MutableSet<Long>): MutableSet<Permission>
}