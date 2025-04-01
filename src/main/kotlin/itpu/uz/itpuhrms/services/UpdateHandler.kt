package itpu.uz.itpuhrms.services

import itpu.uz.itpuhrms.*
import org.apache.commons.logging.LogFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.bots.AbsSender
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.util.*

interface UpdateHandler {
    fun handleUpdateMessage(update: Update, absSender: AbsSender)
    fun handleBlockUpdate(update: Update, status: String)
    fun sendDateInlineButton(update: Update?, absSender: AbsSender, weekOffset: Int)
    fun editDateInlineButton(update: Update?, absSender: AbsSender)
    fun handleDateWithCallback(update: Update, absSender: AbsSender)
}


@Service
class UpdateHandlerImpl(
    private val messageRepository: MessageRepository,
    private val subscriberRepository: SubscriberRepository,
    @Value("\${telegram.statistic-bot.username}") private val statisticBotUsername: String,
    private val statisticBotService: StatisticBotService,
    private val botHashIdRepository: BotHashIdRepository,
    private val userOrgStoreRepository: UserOrgStoreRepository
) : UpdateHandler {
    private val logger = LogFactory.getLog(javaClass)

    override fun handleUpdateMessage(update: Update, absSender: AbsSender) {
        val text = update.message.text
        val userName = update.message.from.userName
        val chatId = update.message.chatId.toString()
        if (text.startsWith("/start hash=")) {
            val hashId = text.substringAfter("/start hash=")
            val message = message(hashId) ?: return
            val botUsername = message.botUsername!!
            val existChatId = subscriberRepository.existsByChatIdAndBotUsernameAndDeletedFalse(chatId, botUsername)
            if (hashId.isEmpty() || existChatId) return

            val duration = Date().minuteDuration(message.createdDate!!)
            if (message.used || duration > Constants.OTP_EXPIRE_MIN) return

            messageRepository.save(
                message.apply {
                    this.used = true
                }
            )
            val employee = message.employee
            if (botUsername == statisticBotUsername && (employee.user!!.role != Role.ORG_ADMIN)) return
            if (employee.isVacant()) return

            val user = employee.user!!
            val subscriber = subscriber(user.id!!, botUsername)?.let {
                it.chatId = chatId
                it.username = userName
                it.botUsername = botUsername
                it
            } ?: run {
                Subscriber(
                    chatId,
                    user,
                    Language.UZ,
                    userName,
                    botUsername
                )
            }
            subscriberRepository.save(subscriber)
            absSender.execute(SendMessage(chatId, "Siz botdan muvaffaqiyatli ro'yxatdan o'tdingiz✅"))

        }else if (text.startsWith("/start bot-hash=")) {
            val hashId = text.substringAfter("/start bot-hash=")
            val botHashId = botHashId(hashId) ?: return
            val existChatId = subscriberRepository.existsByChatIdAndBotUsernameAndDeletedFalse(chatId, botHashId.botUsername!!)
            hashIdEmptyValid(hashId, existChatId)
            val duration = Date().minuteDuration(botHashId.createdDate!!)
            if (botHashId.used || duration > Constants.OTP_EXPIRE_MIN) return

            botHashIdRepository.save(
                botHashId.apply {
                    this.used = true
                }
            )
            val organization = botHashId.organization
            val user = botHashId.user
            val exists = userOrgStoreRepository.existsByUserIdAndOrganizationIdAndDeletedFalse(user.id!!, organization.id!!)
            if (!exists) return
            if (user.role != Role.ORG_ADMIN) return
            val subscriber = subscriber(user.id!!, botHashId.botUsername!!)?.let {
                it.chatId = chatId
                it.username = userName
                it.botUsername = botHashId.botUsername!!
                it.organization = organization
                it
            } ?: run {
                Subscriber(
                    chatId,
                    user,
                    Language.UZ,
                    userName,
                    botHashId.botUsername!!,
                    organization = botHashId.organization,
                )
            }
            subscriberRepository.save(subscriber)
            absSender.execute(SendMessage(chatId, "Siz botdan muvaffaqiyatli ro'yxatdan o'tdingiz✅"))

        }
    }

    override fun handleBlockUpdate(update: Update, status: String) {
        val user = update.myChatMember.from
        val subscriber = subscriberRepository.findByChatIdAndDeletedFalse(user.id.toString()) ?: return
        if (subscriber.complaintCount < 10) {
            when (status) {
                "kicked" -> {
                    subscriber.apply {
                        this.active = false
                        this.complaintCount += 1
                    }
                }

                "member" -> {
                    subscriber.apply {
                        this.active = true
                    }
                }
            }
            subscriberRepository.save(subscriber)
        }
    }

    override fun sendDateInlineButton(update: Update?, absSender: AbsSender, weekOffset: Int) {
        statisticBotService.sendDateInlineButton(update, absSender,0)
        }

    override fun editDateInlineButton(update: Update?, absSender: AbsSender) {
        val chatId = update?.callbackQuery?.message?.chatId ?: return
        val messageId = update.callbackQuery?.message!!.messageId ?: return
        val weekOffset = update.callbackQuery?.data?.substringAfter("ARROW_")?.toInt()
        val newKeyboard = statisticBotService.createDateKeyboard(weekOffset!!)
        val edit = EditMessageReplyMarkup().apply {
            this.chatId = chatId.toString()
            this.messageId = messageId
            this.replyMarkup = newKeyboard
        }
        absSender.execute(edit)
    }

    override fun handleDateWithCallback(update: Update, absSender: AbsSender) {
        val callbackDate = update.callbackQuery.data.substringAfter("DATE_")
        val date = LocalDate.parse(callbackDate)
        val epochDate = date.toEpochSecond(LocalTime.of(0, 0), ZoneOffset.UTC) * 1000
        statisticBotService.sendStatistic(update.callbackQuery.message.chatId, epochDate, absSender)
        val removeButton = DeleteMessage.builder()
            .messageId(update.callbackQuery.message.messageId)
            .chatId(update.callbackQuery.message.chatId)
            .build()
        absSender.execute(removeButton)
        sendDateInlineButton(update, absSender, 0)
    }

    private fun message(hashId: String): Message? {
        return messageRepository.findByHashIdAndDeletedFalse(hashId)
    }
    private fun botHashId(hashId: String): BotHashId? {
        return botHashIdRepository.findByHashIdAndDeletedFalse(hashId)
    }

    private fun subscriber(userId: Long, botUsername: String): Subscriber? {
        return subscriberRepository.findByUserIdAndBotUsernameAndDeletedFalse(userId, botUsername)
    }

    private fun hashIdEmptyValid(hashId: String, existChatId: Boolean){
        if (hashId.isEmpty() || existChatId) return
    }
}