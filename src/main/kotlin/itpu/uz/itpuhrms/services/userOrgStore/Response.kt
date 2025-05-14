package itpu.uz.itpuhrms.services.userOrgStore


import itpu.uz.itpuhrms.Role
import itpu.uz.itpuhrms.UserOrgStore
import itpu.uz.itpuhrms.services.position.OrgResponse



data class UserOrgStoreResponse(
    val id: Long,
    val orgResponse: OrgResponse,
    val granted: Boolean,
    val role: Role
) {
    companion object {
        fun toResponse(userOrgStore: UserOrgStore) =
            UserOrgStoreResponse(
                userOrgStore.id!!,
                OrgResponse.toDto(userOrgStore.organization),
                userOrgStore.granted,
                userOrgStore.role
            )
    }
}