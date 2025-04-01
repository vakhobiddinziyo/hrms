package itpu.uz.itpuhrms

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import mu.KotlinLogging
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.util.*
import com.fasterxml.jackson.databind.SerializerProvider
import java.security.SecureRandom

val logger = KotlinLogging.logger { }


fun userId() = getUserId()!!
fun username() = getUserName()!!
fun role() = getRole()!!
fun sessionId() = SecurityContextHolder.getContext().toString()
fun generateSecureToken(): String {
    val random = SecureRandom()
    val bytes = ByteArray(16)
    random.nextBytes(bytes)
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
}

fun getOSession(headerName: String = "O-Session") = getHeader(headerName) ?: throw UserSessionNotFoundException()

fun getHeader(headerName: String): String? {
    return try {
        val request = (RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes).request
        request.getHeader(headerName)
    } catch (e: java.lang.Exception) {
        println("${Date()} - e.message = ${e.message}")
        null
    }
}


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

class DateToLongSerializer : JsonSerializer<Date?>() {
    override fun serialize(value: Date?, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeNumber(
            value?.time ?: return gen.writeNull())
    }
}