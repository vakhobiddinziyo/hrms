package itpu.uz.itpuhrms.base

data class BaseMessage(
    val code: Int? = null,
    val message: String? = null,
) {
    companion object {
        var OK = BaseMessage(0, "OK")
    }
}