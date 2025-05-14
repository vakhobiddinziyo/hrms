package itpu.uz.itpuhrms.services.permission

import itpu.uz.itpuhrms.Permission

data class PermissionAdminResponse(
    val id: Long,
    val permissionData: String,
    val description: String?
) {
    companion object {
        fun toDto(permission: Permission): PermissionAdminResponse {
            return permission.let {
                PermissionAdminResponse(
                    it.id!!,
                    it.permissionData,
                    it.description
                )
            }
        }
    }
}

data class PermissionResponse(
    val id: Long,
    val name: String,
)