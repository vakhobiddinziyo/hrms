package itpu.uz.itpuhrms.services.projectEmployee

import itpu.uz.itpuhrms.ProjectEmployeeRole


data class ProjectEmployeeRequest(
    val projectId: Long,
    val employeeIds: MutableSet<Long>,
    val role: ProjectEmployeeRole
)

data class ProjectEmployeeEditRequest(
    val role: ProjectEmployeeRole
)