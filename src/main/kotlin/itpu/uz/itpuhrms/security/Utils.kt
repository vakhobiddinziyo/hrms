package itpu.uz.itpuhrms.security


import itpu.uz.itpuhrms.UserSessionNotFoundException
import mu.KotlinLogging
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.util.*
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
