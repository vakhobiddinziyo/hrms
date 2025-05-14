package itpu.uz.itpuhrms.services.project

import itpu.uz.itpuhrms.Project
import itpu.uz.itpuhrms.ProjectStatus

data class ProjectResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val ownerName: String? = null,
    val department: String,
    val employeeAmount: Long,
    val status: ProjectStatus,
    val ownerId: Long? = null
) {
    companion object {
        fun toDto(project: Project, employeeAmount: Long) = project.run {
            ProjectResponse(
                id!!,
                name,
                description,
                owner.user?.fullName,
                owner.department.name,
                employeeAmount,
                status,
                project.owner.id
            )
        }
    }
}