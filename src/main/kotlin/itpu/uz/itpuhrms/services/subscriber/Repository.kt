package itpu.uz.itpuhrms.services.subscriber

import itpu.uz.itpuhrms.Subscriber
import itpu.uz.itpuhrms.base.BaseRepository
import org.springframework.stereotype.Repository


@Repository
interface SubscriberRepository : BaseRepository<Subscriber> {

    fun findByIdAndUserIdAndDeletedFalse(id: Long, userId: Long): Subscriber?
    fun findByUserIdAndBotUsernameAndDeletedFalse(userId: Long, botUsername: String): Subscriber?
    fun existsByChatIdAndBotUsernameAndDeletedFalse(chatId: String, botUsername: String): Boolean
    fun findByChatIdAndDeletedFalse(chatId: String): Subscriber?
    fun findByChatIdAndBotUsernameAndDeletedFalse(chatId: String, botUsername: String): Subscriber?

}
