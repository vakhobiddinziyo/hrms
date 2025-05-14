package itpu.uz.itpuhrms.services.task

import itpu.uz.itpuhrms.*
import itpu.uz.itpuhrms.services.userAbsenceTracker.FileDataResponse
import java.util.*

data class TaskResponse(
    val id: Long,
    val title: String,
    var priority: TaskPriority,
    var order: Short,
    var stateId: Long?,
    var stateOrder: Short?,
    var boardId: Long?,
    var boardName: String?,
    var files: List<FileDataResponse>? = null,
    val ownerName: String? = null,
    val ownerId: Long? = null,
    val ownerPhotoHashId: String? = null,
    var startDate: Date? = null,
    var endDate: Date? = null,
    var description: String? = null,
    var parentTaskId: Long? = null,
    val employees: List<TaskEmployeeResponse>? = null,
    val timeEstimateAmount: Int? = null,
    val subTasks: List<TaskResponse>? = null,
) {
    companion object {
        fun toDto(
            task: Task,
            fileResponse: List<FileDataResponse>? = null,
            subtasks: List<TaskResponse>? = null,
            ownerPhotoHashId: String? = null
        ) =
            TaskResponse(
                task.id!!,
                task.title,
                task.priority,
                task.order,
                task.state.id,
                task.state.order,
                task.board.id,
                task.board.name,
                fileResponse,
                task.owner.fullName,
                task.owner.id,
                ownerPhotoHashId,
                task.startDate,
                task.endDate,
                task.description,
                task.parentTask?.id,
                task.employees.map { TaskEmployeeResponse.toResponse(it) },
                task.timeEstimateAmount,
                subtasks,
            )
    }
}


data class TaskStatResponse(
    val id: Long,
    val taskTitle: String,
    val taskOrder: Short,
    val projectId: Long,
    val taskPriority: TaskPriority?,
    val projectName: String?,
    val boardId: Long,
    val boardName: String,
    val stateId: Long,
    val stateName: String,
    val stateOrder: Short?,
    val startDate: Date?,
    val endDate: Date?
) {
    companion object {
        fun toDto(task: Task) =
            TaskStatResponse(
                task.id!!,
                task.title,
                task.order,
                task.board.project.id!!,
                task.priority,
                task.board.project.name,
                task.board.id!!,
                task.board.name,
                task.state.id!!,
                task.state.name,
                task.state.order,
                task.startDate,
                task.endDate
            )
    }
}


data class TypedTaskResponse(
    var openedTasks: List<TaskStatResponse> = mutableListOf(),
    var closedTasks: List<TaskStatResponse> = mutableListOf(),
    var upcomingTasks: List<TaskStatResponse> = mutableListOf()
)



data class TaskEmployeeResponse(
    val id: Long? = null,
    val projectEmployeeId: Long? = null,
    val fullName: String? = null,
    val imageHashId: String? = null
) {
    companion object {
        fun toResponse(projectEmployee: ProjectEmployee) = projectEmployee.run {
            TaskEmployeeResponse(
                employee.id,
                id,
                employee.user?.fullName,
                employee.imageAsset?.hashId
            )
        }
    }
}

