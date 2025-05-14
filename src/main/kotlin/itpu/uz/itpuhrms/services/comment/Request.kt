package itpu.uz.itpuhrms.services.comment

data class CommentRequest(
    val taskId: Long,
    val text: String,
    val files: List<String>? = null
)
