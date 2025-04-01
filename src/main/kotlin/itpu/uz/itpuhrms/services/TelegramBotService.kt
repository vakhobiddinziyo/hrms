package itpu.uz.itpuhrms.services

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.api.methods.pinnedmessages.PinChatMessage
import org.telegram.telegrambots.meta.api.methods.pinnedmessages.UnpinChatMessage
import org.telegram.telegrambots.meta.api.methods.send.SendDocument
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto
import org.telegram.telegrambots.meta.api.methods.send.SendVideo
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update

interface BotSender {
    fun sendMessage(username: String, sendMessage: SendMessage): Message?
    fun sendPhoto(username: String, sendPhoto: SendPhoto): Message?
    fun sendVideo(username: String, sendVideo: SendVideo): Message?
    fun sendDocument(username: String, sendDocument: SendDocument): Message?
    fun pinChatMessage(username: String, pinChatMessage: PinChatMessage): Boolean?
    fun unpinChatMessage(username: String, unpinChatMessage: UnpinChatMessage): Boolean?
    fun deleteMessage(username: String, deleteMessage: DeleteMessage): Boolean?
    fun registerBots(telegramApi: TelegramBotsApi)
}


@Service
class BotSenderImpl(
    @Value("\${telegram.hrms-bot.username}") private  var hrmsUsername: String,
    @Value("\${telegram.hrms-bot.token}") private  var hrmsToken: String,
    @Value("\${telegram.statistic-bot.username}") private  var statisticUsername: String,
    @Value("\${telegram.statistic-bot.token}") private  var statisticToken: String,
    updateHandler: UpdateHandler
) : BotSender {

    private val botsMap: Map<String, BaseTelegramBot> = mapOf(
        hrmsUsername to BaseTelegramBot(hrmsUsername, updateHandler, hrmsToken),
        statisticUsername to BaseTelegramBot(statisticUsername, updateHandler, statisticToken)
    )

    override fun sendMessage(username: String, sendMessage: SendMessage): Message? {
        return botsMap[username]?.execute(sendMessage)
    }

    override fun sendPhoto(username: String, sendPhoto: SendPhoto): Message? {
        return botsMap[username]?.execute(sendPhoto)
    }

    override fun sendVideo(username: String, sendVideo: SendVideo): Message? {
        return botsMap[username]?.execute(sendVideo)
    }

    override fun sendDocument(username: String, sendDocument: SendDocument): Message? {
        return botsMap[username]?.execute(sendDocument)
    }

    override fun pinChatMessage(username: String, pinChatMessage: PinChatMessage): Boolean? {
        return botsMap[username]?.execute(pinChatMessage)
    }

    override fun unpinChatMessage(username: String, unpinChatMessage: UnpinChatMessage): Boolean? {
        return botsMap[username]?.execute(unpinChatMessage)
    }

    override fun deleteMessage(username: String, deleteMessage: DeleteMessage): Boolean? {
        return botsMap[username]?.execute(deleteMessage)
    }

    override fun registerBots(telegramApi: TelegramBotsApi) {
        botsMap.values.forEach {
            telegramApi.registerBot(it)
        }
    }

}


class BaseTelegramBot(
    private val username: String,
    private val updateHandler: UpdateHandler,
    token: String,
) : TelegramLongPollingBot(token) {

    override fun getBotUsername() = username
    override fun onUpdateReceived(update: Update) {
        if (update.hasMessage() && update.message.hasText()) {
            updateHandler.handleUpdateMessage(update, this)
        }

        if (update.hasMyChatMember() && update.myChatMember.newChatMember.status != null) {
            updateHandler.handleBlockUpdate(update, update.myChatMember.newChatMember.status)
        }

        if (update.hasMessage() && update.message.hasText() && update.message.text == "/statistics") {
             updateHandler.sendDateInlineButton(update,this, 0)
        }
        if (update.hasCallbackQuery() && update.callbackQuery.data.startsWith("ARROW_")) {
            updateHandler.editDateInlineButton(update, this)
        }
        if (update.hasCallbackQuery() && update.callbackQuery.data.startsWith("DATE_")) {
            updateHandler.handleDateWithCallback(update, this)
        }
    }
}