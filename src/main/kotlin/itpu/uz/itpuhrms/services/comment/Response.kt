package itpu.uz.itpuhrms.services.comment

import itpu.uz.itpuhrms.Comment
import itpu.uz.itpuhrms.services.user.UserDto

data class CommentResponse(
    val id: Long,
    val taskId: Long,
    val text: String,
    val owner: UserDto,
    val files: List<String>? = mutableListOf()
) {
    companion object {
        fun toDto(entity: Comment) = entity.run {
            CommentResponse(
                id!!,
                task.id!!,
                text,
                UserDto.toResponse(owner),
                files?.map { it.hashId }
            )
        }
    }
}