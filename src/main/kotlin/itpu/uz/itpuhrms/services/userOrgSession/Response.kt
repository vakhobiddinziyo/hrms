package itpu.uz.itpuhrms.services.userOrgSession

import com.fasterxml.jackson.annotation.JsonFormat
import itpu.uz.itpuhrms.Organization
import itpu.uz.itpuhrms.Role
import itpu.uz.itpuhrms.UserOrgSession
import itpu.uz.itpuhrms.services.position.OrgResponse
import java.time.Duration
import java.util.*


data class UserSessionResponse(
    val sessionId: String,
    val userId: Long,
    val orgResponse: OrgResponse? = null,
    val role: Role,
    @JsonFormat(shape = JsonFormat.Shape.NUMBER) val expiresAt: Date
) {
    companion object {
        fun toResponse(session: UserOrgSession, organization: Organization?) = session.run {
            UserSessionResponse(
                id!!,
                userId,
                organization?.let { OrgResponse.toDto(it) },
                role,
                Date(session.createdDate.time + Duration.ofMinutes(2).toMillis())
            )
        }
    }
}
