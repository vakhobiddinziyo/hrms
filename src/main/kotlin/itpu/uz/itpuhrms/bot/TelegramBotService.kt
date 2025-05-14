package itpu.uz.itpuhrms.bot

import org.apache.commons.io.IOUtils
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendDocument
import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.api.objects.Update
import java.io.StringWriter
import java.util.*

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