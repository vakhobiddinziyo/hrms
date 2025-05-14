package itpu.uz.itpuhrms.services.department

import itpu.uz.itpuhrms.*
import itpu.uz.itpuhrms.services.position.OrgResponse

data class DepartmentResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val departmentType: DepartmentType,
    var organization: OrgResponse,
    var status: Status,
    var headDepartment: ParentDepartmentResponse? = null,
    var parentDepartment: ParentDepartmentResponse? = null
) {
    companion object {
        fun toDto(department: Department) = department.run {
            DepartmentResponse(
                id!!,
                name,
                description,
                departmentType,
                OrgResponse.toDto(organization),
                status,
                headDepartment?.let { ParentDepartmentResponse.toResponse(it) },
                parentDepartment?.let { ParentDepartmentResponse.toResponse(it) },
            )
        }
    }

    data class ParentDepartmentResponse(
        val id: Long,
        val name: String,
        val description: String?,
        val departmentType: DepartmentType,
        var status: Status,
    ) {
        companion object {
            fun toResponse(department: Department) = department.run {
                ParentDepartmentResponse(
                    id!!,
                    name,
                    description,
                    departmentType,
                    status
                )
            }
        }
    }
}

data class Structure(
    val id: Long,
    val name: String,
    val type: StructureType,
    val hasChild: Boolean = false
)



data class DepartmentAllContentResponse(
    val structureTree: List<Structure>,
    var content: List<DepartmentAllAdminResponse> = mutableListOf()
)

data class DepartmentAllAdminResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val departmentType: DepartmentType,
    var totalEmployee: Long,
    var vacantEmployee: Long,
    var busyEmployee: Long,
    var hasChild: Boolean,
    var childDepartmentIds: MutableList<Long> = mutableListOf(),
    val parentId: Long?
)



data class DepartmentContentResponse(
    val structureTree: List<Structure>,
    var content: List<DepartmentAdminResponse> = mutableListOf()
)

data class DepartmentAdminResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val departmentType: DepartmentType,
    var totalEmployee: Long,
    var vacantEmployee: Long,
    var busyEmployee: Long,
    var hasChild: Boolean,
)
