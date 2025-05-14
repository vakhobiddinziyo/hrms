package itpu.uz.itpuhrms.services.subscriber

import itpu.uz.itpuhrms.Language
import itpu.uz.itpuhrms.TaskAction

data class SubscriberRequest(
    val language: Language
)

data class TaskSubscriberUpdateRequest(
    val id: Long,
    val actions: MutableSet<TaskAction>
)

data class TaskSubscriberRequest(
    val taskId: Long,
    val actions: MutableSet<TaskAction>
)


