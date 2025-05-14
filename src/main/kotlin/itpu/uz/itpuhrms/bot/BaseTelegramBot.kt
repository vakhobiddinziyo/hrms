package itpu.uz.itpuhrms.bot

import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.objects.Update

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