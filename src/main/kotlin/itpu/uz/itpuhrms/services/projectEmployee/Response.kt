package itpu.uz.itpuhrms.services.projectEmployee


import itpu.uz.itpuhrms.ProjectEmployee
import itpu.uz.itpuhrms.ProjectEmployeeRole
import itpu.uz.itpuhrms.services.employee.EmployeeResponse


data class ProjectEmployeeResponse(
    val id: Long,
    val projectId: Long,
    val employee: EmployeeResponse,
    val role: ProjectEmployeeRole
) {
    companion object {
        fun toDto(entity: ProjectEmployee) = ProjectEmployeeResponse(
            entity.id!!,
            entity.project.id!!,
            EmployeeResponse.toDto(entity.employee),
            entity.projectEmployeeRole
        )
    }
}