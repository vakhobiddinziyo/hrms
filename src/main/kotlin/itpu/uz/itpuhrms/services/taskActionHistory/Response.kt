package itpu.uz.itpuhrms.services.taskActionHistory

import itpu.uz.itpuhrms.*
import itpu.uz.itpuhrms.services.comment.CommentResponse
import itpu.uz.itpuhrms.services.employee.EmployeeResponse
import itpu.uz.itpuhrms.services.state.StateResponse
import itpu.uz.itpuhrms.services.timeTracking.TimeTrackingResponse
import itpu.uz.itpuhrms.services.user.UserDto
import java.util.*

data class TaskActionHistoryResponse(
    val id: Long,
    val taskId: Long,
    val owner: UserDto,
    val action: TaskAction,
    val fileHashIds: List<String>? = null,
    val fromState: StateResponse? = null,
    val toState: StateResponse? = null,
    val subjectEmployee: EmployeeResponse? = null,
    val createdAt: Long,
    val taskPriority: PriorityResponse? = null,
    val dueDate: Date? = null,
    val startDate: Date? = null,
    val title: String? = null,
    val comment: CommentResponse? = null,
    val timeTracking: TimeTrackingResponse? = null,
    val timeEstimateAmount: Int? = null,
) {
    companion object {
        fun toDto(entity: TaskActionHistory) = entity.run {
            TaskActionHistoryResponse(
                id!!,
                task.id!!,
                UserDto.toResponse(owner),
                action,
                files?.map { it.hashId },
                fromState?.let { StateResponse.toDto(it) },
                toState?.let { StateResponse.toDto(it) },
                subjectEmployee?.let { EmployeeResponse.toDto(it) },
                createdDate!!.time,
                priority?.let { PriorityResponse.toResponse(it) },
                dueDate,
                startDate,
                title,
                comment?.let { CommentResponse.toDto(it) },
                timeTracking?.let { TimeTrackingResponse.toDto(it) },
                timeEstimateAmount
            )
        }
    }
}



data class PriorityResponse(
    val priority: TaskPriority,
    val localizedName: String
) {
    companion object {
        fun toResponse(priority: TaskPriority) = priority.run {
            PriorityResponse(this, localizedName.localized())
        }
    }
}