package itpu.uz.itpuhrms.bot

data class OtpDto(
    val hash: String,
    val botUsername: String,
    val retryTimeMin: Int,
    val expireOtpMin: Int
)
