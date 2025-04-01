package itpu.uz.itpuhrms

import org.apache.commons.io.IOUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
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
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.api.methods.send.SendDocument
import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.starter.TelegramBotInitializer
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

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
            logger.error { e.localizedMessage }
        }
    }


    private fun writeLog(exception: Throwable): String {
        val stringWriter = StringWriter()
        exception.printStackTrace(PrintWriter(stringWriter))
        stringWriter.close()
        return stringWriter.toString()
    }

}


@Configuration
class TelegramBotConfiguration {

    @Value("\${telegram.error-bot.token}")
    private lateinit var token: String
    @Value("\${telegram.error-bot.username}")
    private lateinit var username: String

    @Bean
    fun telegramBotInitializer(
    ): TelegramBotInitializer {
        return TelegramBotInitializer(
            TelegramBotsApi(DefaultBotSession::class.java),
            listOf(TelegramBotService(username, token)),
            emptyList()
        )
    }
}


class TelegramBotService(
    val username: String,
    token: String
) : TelegramLongPollingBot(token) {
    override fun getBotUsername(): String = username
    override fun onUpdateReceived(update: Update) {
        if (update.hasMessage() && update.message.hasText()) {
            val message = update.message
            val chatId = message.chatId.toString()
            val text = message.text
            if (text.contains("get-log"))
                return senLog(text, chatId)
        }
    }

    private fun senLog(text: String, chatId: String) {
        try {
            val split = text.split(" ")
            val serviceName = "HRMS service"
            val line = split.getOrNull(1)?.toInt() ?: 1000
            val process = Runtime.getRuntime().exec("tail -n $line /opt/hrms/out.log")
            val writer = StringWriter()
            IOUtils.copy(process.inputStream, writer, Charsets.UTF_8)
            val log = writer.toString()
            process.waitFor()
            val sendDocument = SendDocument()
            sendDocument.chatId = chatId
            sendDocument.document = InputFile(log.byteInputStream(), "$serviceName.log")
            sendDocument.caption = "$serviceName log from ${Date()}"
            execute(sendDocument)
        } catch (e: Exception) {
            val log = e.stackTraceToString()
            val sendDocument = SendDocument()
            sendDocument.chatId = chatId
            sendDocument.document = InputFile(log.byteInputStream(), "error.log")
            sendDocument.caption = "error log from ${Date()}"
            execute(sendDocument)
        }
    }
}
