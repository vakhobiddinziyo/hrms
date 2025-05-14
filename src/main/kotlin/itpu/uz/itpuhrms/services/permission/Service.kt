package itpu.uz.itpuhrms.services.permission

import itpu.uz.itpuhrms.*
import itpu.uz.itpuhrms.base.BaseMessage
import org.springframework.stereotype.Service


interface PermissionService {
    fun create(request: PermissionRequest): PermissionAdminResponse
    fun getById(id: Long): PermissionAdminResponse
    fun getAll(): List<PermissionAdminResponse>
    fun update(id: Long, request: PermissionRequest): PermissionAdminResponse
    fun delete(id: Long): BaseMessage
}

@Service
class PermissionServiceImpl(
    private val permissionRepository: PermissionRepository
) : PermissionService {
    override fun create(request: PermissionRequest): PermissionAdminResponse {
        val entity = permissionRepository.save(
            Permission(
                request.permissionData,
                request.description
            )
        )
        val savedPermission = permissionRepository.save(entity)
        return PermissionAdminResponse.toDto(savedPermission)
    }

    override fun getById(id: Long): PermissionAdminResponse {
        val permission = permissionRepository.findByIdAndDeletedFalse(id) ?: throw PermissionNotFoundException()
        return PermissionAdminResponse.toDto(permission)
    }

    override fun getAll(): List<PermissionAdminResponse> {
        return permissionRepository.findAllNotDeleted().map { PermissionAdminResponse.toDto(it) }
    }

    override fun update(id: Long, request: PermissionRequest): PermissionAdminResponse {
        val existingPermission = permissionRepository.findByIdAndDeletedFalse(id) ?: throw PermissionNotFoundException()

        existingPermission.permissionData = request.permissionData
        existingPermission.description = request.description

        val updatedPermissionEntity = permissionRepository.save(existingPermission)
        return PermissionAdminResponse.toDto(updatedPermissionEntity)
    }

    override fun delete(id: Long): BaseMessage {
        permissionRepository.trash(id) ?: throw PermissionNotFoundException()
        return BaseMessage.OK
    }
}
