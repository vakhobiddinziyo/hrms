package itpu.uz.itpuhrms.services.project

import itpu.uz.itpuhrms.Employee
import itpu.uz.itpuhrms.Project
import itpu.uz.itpuhrms.ProjectStatus
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class ProjectCreateRequest(
    @field:NotNull(message = "Project name must not be null!")
    @field:Size(message = "Project name length must be 1-50 letters", max = 50, min = 1)
    val name: String? = null,
    @field:Size(message = "Project description length must be maximum 175", max = 175)
    val description: String? = null,
) {
    fun toEntity(employee: Employee) =
        Project(name!!, employee, employee.department, description, ProjectStatus.ACTIVE)
}

data class ProjectUpdateRequest(
    @field:NotNull(message = "Project name must not be null!")
    @field:Size(message = "Project name length must be 1-50 letters", max = 50, min = 1)
    val name: String? = null,
    @field:Size(message = "Project description length must be maximum 175", max = 175)
    val description: String? = null,
    val status: ProjectStatus
)
