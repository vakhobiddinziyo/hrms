package itpu.uz.itpuhrms.utils

object Constants {
    const val USERNAME = "username"
    const val TOKEN_TYPE = "token_type"
    const val USER_ID = "userId"

    const val EMPLOYEE_IMAGE_KILOBYTE_LIMIT = 20_000
    const val DEFAULT_INTERVAL_MIN = 1
    const val ACCESS_TOKEN = "access_token"
    const val REFRESH_TOKEN = "refresh_token"
    const val TELEGRAM_BOT_USERNAME = "hrmsuz_bot"
    const val OTP_MESSAGE_LIMIT = 5
    const val OTP_MESSAGE_INTERVAL_LIMIT_MIN = 5
    const val OTP_MESSAGE_RETRY_INTERVAL_LIMIT_MIN = 1
    const val OTP_EXPIRE_MIN = 3

    // this is for in and out types
    const val NORMAL_TOURNIQUET_INTERVAL_MIN = 1
}

const val BASE_API = "api/v1"