package itpu.uz.itpuhrms.services.subscriber

import itpu.uz.itpuhrms.Language
import itpu.uz.itpuhrms.Subscriber
import itpu.uz.itpuhrms.TaskSubscriber
import itpu.uz.itpuhrms.services.task.TaskResponse
import itpu.uz.itpuhrms.services.user.UserDto

data class TaskSubscriberResponse(
    val id: Long,
    val user: UserDto,
    val task: TaskResponse,
) {
    companion object {
        fun toResponse(taskSubscriber: TaskSubscriber) = taskSubscriber.run {
            TaskSubscriberResponse(
                id!!,
                UserDto.toResponse(taskSubscriber.subscriber.user),
                TaskResponse.toDto(task)
            )
        }
    }
}


data class SubscriberResponse(
    val id: Long,
    val chatId: String,
    val username: String? = null,
    val language: Language,
    val user: UserDto,
) {
    companion object {
        fun toResponse(subscriber: Subscriber) = subscriber.run {
            SubscriberResponse(
                id!!,
                chatId,
                username,
                language,
                UserDto.toResponse(user)
            )
        }
    }
}
