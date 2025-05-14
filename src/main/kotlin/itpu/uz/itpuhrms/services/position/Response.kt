package itpu.uz.itpuhrms.services.position

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include
import itpu.uz.itpuhrms.*
import itpu.uz.itpuhrms.services.department.Structure
import itpu.uz.itpuhrms.services.permission.PermissionResponse

@JsonInclude(Include.NON_NULL)
data class PositionResponse(
    val id: Long,
    val name: String,
    var level: Level,
    val permissions: List<PermissionResponse>,
    var organization: OrgResponse? = null,
) {
    companion object {
        fun toDto(position: Position, organization: OrgResponse?) = PositionResponse(
            position.id!!,
            position.name,
            position.level,
            position.permission.map { PermissionResponse(it.id!!, it.permissionData) },
            organization
        )
    }
}
@JsonInclude(Include.NON_NULL)
data class PositionAdminResponse(
    val id: Long,
    val name: String,
    var level: Level,
    val employeeAmount: Long,
    val permissions: List<PermissionResponse>,
    var organization: OrgResponse? = null,
) {
    companion object {
        fun toDto(position: Position, amount: Long, organization: OrgResponse?) = PositionAdminResponse(
            position.id!!,
            position.name,
            position.level,
            amount,
            position.permission.map { PermissionResponse(it.id!!, it.permissionData) },
            organization
        )
    }
}

data class PositionEmployeesResponse(
    val id: Long,
    val name: String,
    val employeeAmount: Int
)


data class PositionContentResponse(
    val structureTree: List<Structure>,
    val content: List<PositionDto>
)



data class PositionDto(
    val id: Long,
    val name: String,
    val level: Level,
    val totalEmployee: Long,
    val vacantEmployee: Long,
    val busyEmployee: Long,
)


data class OrgResponse(
    val id: Long,
    val name: String,
    val status: Status,
    val isActive: Boolean
) {
    companion object {
        fun toDto(org: Organization): OrgResponse {
            return org.let {
                OrgResponse(
                    it.id!!,
                    it.name,
                    it.status,
                    it.isActive
                )
            }
        }
    }
}
