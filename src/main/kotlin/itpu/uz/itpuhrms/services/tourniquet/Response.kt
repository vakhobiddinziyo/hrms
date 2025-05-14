package itpu.uz.itpuhrms.services.tourniquet

import itpu.uz.itpuhrms.Tourniquet
import itpu.uz.itpuhrms.TourniquetType
import itpu.uz.itpuhrms.services.organization.OrgAdminResponse


data class TourniquetResponse(
    val id: Long,
    val ip: String,
    val name: String,
    val username: String,
    val description: String?,
    val type: TourniquetType,
    val organization: OrgAdminResponse
) {
    companion object {
        fun toResponse(tourniquet: Tourniquet) = tourniquet.run {
            TourniquetResponse(
                id!!,
                ip,
                name,
                username,
                description,
                type,
                OrgAdminResponse.toDto(tourniquet.organization)
            )
        }
    }
}



data class TourniquetDto(
    val id: Long,
    val ip: String,
    val name: String,
    val username: String,
    val password: String,
) {
    companion object {
        fun toDto(tourniquet: Tourniquet, password: String) = tourniquet.run {
            TourniquetDto(
                id!!,
                ip,
                name,
                username,
                password
            )
        }
    }
}