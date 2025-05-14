package itpu.uz.itpuhrms.services.userOrgStore

data class UserOrgStoreRequest(
    val userId: Long,
    val orgId: Long,
    val granted: Boolean
)