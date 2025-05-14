package itpu.uz.itpuhrms.security

import itpu.uz.itpuhrms.*
import itpu.uz.itpuhrms.bot.BotSender
import itpu.uz.itpuhrms.services.user.UserRepository
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.hashids.Hashids
import org.jetbrains.annotations.NotNull
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.context.support.ResourceBundleMessageSource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession



@Component
class JwtTokenFilter(
    private val jwtService: JwtService,
    private val customUserDetailsService: CustomUserDetailsService
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        @NotNull request: HttpServletRequest,
        @NotNull response: HttpServletResponse,
        @NotNull filterChain: FilterChain
    ) {
        val header = request.getHeader(HttpHeaders.AUTHORIZATION)
        val token = if (header != null && header.startsWith("Bearer ")) header.removePrefix("Bearer ") else null

        if (token == null) {
            filterChain.doFilter(request, response)
        } else {
            val claims = jwtService.getClaims(token)
            if (claims == null) {
                response.status = HttpStatus.UNAUTHORIZED.value()
                return
            }

            if (SecurityContextHolder.getContext().authentication == null) {
                val userDetails = customUserDetailsService.loadUserByUsername(claims.username)
                val emptyContext = SecurityContextHolder.createEmptyContext()
                val authentication = UsernamePasswordAuthenticationToken(
                    userDetails.myUserName, null, listOf(SimpleGrantedAuthority(userDetails.role.name))
                )
                authentication.details = UserAuthenticationDetails(userDetails.role, claims.userId)
                emptyContext.authentication = authentication
                SecurityContextHolder.setContext(emptyContext)
            }
            filterChain.doFilter(request, response)
        }
    }


}


@Component
class ContextRefreshEvent(
    @Qualifier("passwordEncoder") private val passwordEncoder: BCryptPasswordEncoder,
    private val userRepository: UserRepository,
    private val botSender: BotSender,
) {
    @EventListener(ContextRefreshedEvent::class)
    fun contextRefreshedEvent() {


        if (userRepository.findByUsernameAndDeletedFalse("developer") == null) {
            val user = User(
                fullName = "John Doe",
                phoneNumber = "998997858857",
                username = "developer",
                password = passwordEncoder.encode("password123"),
                status = Status.ACTIVE,
                mail = "john.doe@example.com",
                role = Role.DEVELOPER
            )
            userRepository.save(user)
            logger().info("User initialized")
        }

        if (userRepository.findByUsernameAndDeletedFalse("admin") == null) {
            val user = User(
                fullName = "John Doe Admin",
                phoneNumber = "998997858857",
                username = "admin",
                password = passwordEncoder.encode("password123"),
                status = Status.ACTIVE,
                mail = "john.doe@example.com",
                role = Role.ADMIN
            )
            userRepository.save(user)
            logger().info("User initialized")
        }

        try {
            val telegramBotsApi = TelegramBotsApi(DefaultBotSession::class.java)
            botSender.registerBots(telegramBotsApi)
        } catch (e: TelegramApiException) {
            logger().error(e.message)
        }
    }
}

@Component
class Components {
    @Bean
    fun messageMessageSource() = ResourceBundleMessageSource().apply {
        setDefaultEncoding(Charsets.UTF_8.name())
        setBasenames("error", "taskAction", "helperWords")
    }
}

@Component
class HashIdUtil(
    @Value("\${hashId.salt}") private val salt: String,
    @Value("\${hashId.length}") private val length: Int,
    @Value("\${hashId.alphabet}") private val alphabet: String,
) {
    private val hashIds = Hashids(salt, length, alphabet)
    fun encode(id: Long): String = hashIds.encode(id)
}