package itpu.uz.itpuhrms.services.taskActionHistory

import itpu.uz.itpuhrms.TaskAction
import itpu.uz.itpuhrms.TaskPriority
import java.util.*

data class TaskActionHistoryRequest(
    val taskId: Long,
    val ownerId: Long,
    val action: TaskAction,
    val fileHashIds: List<String>? = null,
    val fromStateId: Long? = null,
    val toStateId: Long? = null,
    val subjectEmployeeIds: List<Long>? = null,
    val taskPriority: TaskPriority? = null,
    val dueDate: Date? = null,
    val startDate: Date? = null,
    val description: String? = null,
    val title: String? = null,
    val order: Short? = null,
    val commentId: Long? = null,
    val timeTrackingIds: List<Long>? = null,
    val timeEstimateAmount: Int? = null,
    val subtasks: MutableList<Long>? = null
)
