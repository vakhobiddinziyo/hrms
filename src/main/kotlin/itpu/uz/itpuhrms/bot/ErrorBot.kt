package itpu.uz.itpuhrms.bot

import org.springframework.core.env.Environment
import org.springframework.core.io.FileSystemResource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestTemplate
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Component
class ErrorBot(
    private val environment: Environment
) {
    var token = "token"
    var groupId = "-1002129661271"


    fun sendLog(e: Throwable) {
        try {
            val log = writeLog(e)
            val url = "https://api.telegram.org/bot$token/sendDocument"
            val caption = """
        📎Project : Zhrms Service
        👤Profile : ${environment.activeProfiles.first()}
        ⏱️Time: ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy"))}
    """.trimIndent()
            val headers = HttpHeaders().apply { contentType = MediaType.MULTIPART_FORM_DATA }
            val body: MultiValueMap<String, Any> = LinkedMultiValueMap()
            body["chat_id"] = groupId
            body["caption"] = caption
            val fileOutputStream = FileOutputStream(File("./error.txt"))
            fileOutputStream.write(log.toByteArray(), 0, log.length)
            fileOutputStream.close()
            body["document"] = FileSystemResource("./error.txt")
            val requestEntity = HttpEntity(body, headers)
            val restTemplate = RestTemplate()
            restTemplate.exchange(url, HttpMethod.POST, requestEntity, String::class.java)
        } catch (ex: Exception) {
            itpu.uz.itpuhrms.security.logger.error { e.localizedMessage }
        }
    }


    private fun writeLog(exception: Throwable): String {
        val stringWriter = StringWriter()
        exception.printStackTrace(PrintWriter(stringWriter))
        stringWriter.close()
        return stringWriter.toString()
    }

}