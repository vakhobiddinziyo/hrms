package itpu.uz.itpuhrms.bot


import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.starter.TelegramBotInitializer
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession

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