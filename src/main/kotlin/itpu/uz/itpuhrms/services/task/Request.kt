package itpu.uz.itpuhrms.services.task

import itpu.uz.itpuhrms.TaskPriority
import jakarta.validation.constraints.Size

data class TaskCreateRequest(
    @field:Size(min = 1, max = 225, message = "title length should be between 1 and 225")
    val title: String,
    val employeeIds: List<Long>,
    val stateId: Long,
    val priority: TaskPriority,
    val startDate: Long?,
    val endDate: Long?,
    val filesHashIds: List<String>,
    val description: String? = null,
    val timeEstimateAmount: Int?,
    val parentTaskId: Long? = null
)

data class TaskUpdateRequest(
    @field:Size(min = 1, max = 225, message = "title length should be between 1 and 225")
    val title: String?,
    val priority: TaskPriority?,
    val filesHashIds: List<String>?,
    val employeeIds: List<Long>?,
    val startDate: Long?,
    val endDate: Long?,
    val description: String? = null,
    val timeEstimateAmount: Int?
)

data class TaskDeleteRequest(
    val isSubtask: Boolean,
    val taskIds: MutableList<Long>
)