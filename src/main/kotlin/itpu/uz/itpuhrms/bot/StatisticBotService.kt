package itpu.uz.itpuhrms.bot

import itpu.uz.itpuhrms.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.support.ResourceBundleMessageSource
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.ResultSetExtractor
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.telegram.telegrambots.meta.api.methods.ParseMode
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import java.util.*
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.bots.AbsSender
import itpu.uz.itpuhrms.security.generateSecureToken
import itpu.uz.itpuhrms.security.userId
import itpu.uz.itpuhrms.services.ExtraService
import itpu.uz.itpuhrms.services.employee.EmployeeAttendanceResponse
import itpu.uz.itpuhrms.services.organization.OrganizationRepository
import itpu.uz.itpuhrms.services.subscriber.SubscriberRepository
import itpu.uz.itpuhrms.services.user.UserRepository
import itpu.uz.itpuhrms.services.workingDate.WorkingDateConfigRepository
import itpu.uz.itpuhrms.utils.Constants
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter

interface StatisticBotService{
    fun getHash(): OtpDto
    fun sendStatistic(chatId: Long, date: Long, absSender: AbsSender)
    fun sendDateInlineButton(update: Update?, absSender: AbsSender, weekOffset: Int)
    fun createDateKeyboard(weekOffset: Int): InlineKeyboardMarkup
}

@Service
class StatisticBotServiceImpl(
    @Value("\${telegram.statistic-bot.username}") private val botUsername: String,
    private val subscriberRepository: SubscriberRepository,
    private val messageSource: ResourceBundleMessageSource,
    private val extraService: ExtraService,
    private val botHashIdRepository: BotHashIdRepository,
    private val userRepository: UserRepository,
    private val jdbcTemplate: JdbcTemplate,
    private val organizationRepository: OrganizationRepository,
    private val workingDateConfigRepository: WorkingDateConfigRepository,
): StatisticBotService {

    @Transactional
    override fun getHash(): OtpDto {
            val organization = extraService.getOrgFromCurrentUser()
            val user = userRepository.findByIdAndDeletedFalse(userId())?: throw UserNotFoundException()
            val hashId = generateSecureToken()
            val count = botHashIdRepository.getLastHashAmount(organization.id!!, Constants.OTP_MESSAGE_INTERVAL_LIMIT_MIN, botUsername)
            if (count >= Constants.OTP_MESSAGE_LIMIT) throw OtpMessageLimitException()

            val botHashId = BotHashId(
                hashId = hashId,
                botUsername = botUsername,
                organization= organization,
                user = user,
                used = false,
                retryCount = 1
            )
            val lastMessage = botHashIdRepository.findLastHashAtInterval(organization.id!!, Constants.OTP_MESSAGE_INTERVAL_LIMIT_MIN, botUsername)
            lastMessage?.let {
                if (!it.used) {
                    if (it.createdDate!!.time + it.retryCount * 60 * 1000 > Date().time) throw OtpProhibitedException()
                    botHashId.retryCount = it.retryCount
                }
            }

            botHashIdRepository.save(botHashId)
            return OtpDto(
                hashId,
                botUsername,
                Constants.OTP_MESSAGE_RETRY_INTERVAL_LIMIT_MIN * botHashId.retryCount,
                Constants.OTP_EXPIRE_MIN
            )
    }

    override fun sendStatistic(chatId: Long, date: Long, absSender: AbsSender) {

        val subscriber =  subscriberRepository.findByChatIdAndBotUsernameAndDeletedFalse(chatId.toString(), botUsername)
        subscriber?.let {
            if (it.user.role != Role.ORG_ADMIN || !it.active ) return
        } ?: return
        val list:List<EmployeeAttendanceResponse>? = getEmployeesAttendance(date, subscriber.organization!!.id)
        if (list == null){
            val errorMessage = SendMessage.builder()
                .text("${date.localDate()} : " + localization(ErrorCode.WORKING_DATE_CONFIG_NOT_FOUND.name, subscriber.language))
                .chatId(chatId)
                .build()
            absSender.execute(errorMessage)
            return
        }

        var msg = StringBuilder()
            .append("${quote(bold("\uD83D\uDCC6${date.localDate()}  " +
                    " \uD83D\uDCDD${localization(AttendanceDataMessage.ATTENDANCE.name, subscriber.language)}"))}\n\n")


        for (employee in list) {
            val employeeMessage = makeMessage(employee, subscriber.language)

            if (msg.length + employeeMessage.length > 4096) {
                absSender.execute(
                    SendMessage.builder()
                        .text(msg.toString())
                        .parseMode(ParseMode.HTML)
                        .chatId(chatId)
                        .build()
                )
                msg = StringBuilder()
            }

            msg.append(employeeMessage)
        }
        if (msg.isNotEmpty()) {
            absSender.execute(
                SendMessage.builder()
                    .text(msg.toString())
                    .parseMode(ParseMode.HTML)
                    .chatId(chatId)
                    .build()
            )
        }
    }

    override fun createDateKeyboard(weekOffset: Int): InlineKeyboardMarkup {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val today = LocalDate.now()

        val currentMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

        val startOfWeek = currentMonday.plusWeeks(weekOffset.toLong())

        val dateButtons = (6 downTo 0).map {
            val date = startOfWeek.plusDays(it.toLong())
            InlineKeyboardButton.builder()
                .text(date.format(formatter))
                .callbackData("DATE_${date}")
                .build()
        }.chunked(3)

        val prevButton = InlineKeyboardButton.builder()
            .text("⬅")
            .callbackData("ARROW_${weekOffset + 1}")
            .build()

        val nextButton = InlineKeyboardButton.builder()
            .text("➡")
            .callbackData("ARROW_${weekOffset - 1}")
            .build()
        val homeButton = InlineKeyboardButton.builder()
            .text("now")
            .callbackData("ARROW_${0}")
            .build()

        val keyboard = mutableListOf<List<InlineKeyboardButton>>()
        keyboard.addAll(dateButtons)

        if (weekOffset == 0) {
            keyboard.add(listOf(nextButton))
        } else {
            keyboard.add(listOf(prevButton,homeButton, nextButton))
        }
        return InlineKeyboardMarkup.builder().keyboard(keyboard).build()
    }

    override fun sendDateInlineButton(update: Update?, absSender: AbsSender, weekOffset: Int) {
        var chatId = update?.message?.chatId
        if (update!!.hasCallbackQuery()) chatId = update.callbackQuery.message.chatId
        val subscriber = subscriberRepository.findByChatIdAndBotUsernameAndDeletedFalse(chatId.toString(), botUsername)?: return
        val message = SendMessage().apply {
            this.chatId = chatId.toString()
            this.text = localization(AttendanceDataMessage.SELECT_DATE.name, subscriber.language)
            this.replyMarkup = createDateKeyboard(weekOffset)
        }
        absSender.execute(message)
    }

    private fun makeMessage(statistic: EmployeeAttendanceResponse, language: Language) : String{
        return buildString {
            append(bold(localization(AttendanceDataMessage.USER_FULL_NAME.name, language))+" ${statistic.fullName}\n")
            append(bold(localization(AttendanceDataMessage.POSITION_NAME.name, language))+" ${statistic.positionName}\n")
            if (statistic.time != null)
            append(bold(localization(AttendanceDataMessage.DATE_TIME.name, language))+" ${statistic.time}\n")
            else append(bold(localization(AttendanceDataMessage.DATE_TIME.name, language))+"❌\n")
            append(bold(localization(AttendanceDataMessage.STATUS.name, language))+" ${localization(statistic.state.name, language)}\n")
            append(bold(localization(AttendanceDataMessage.EMPLOYEE_ID.name, language))+" ${statistic.employeeId}\n\n")
        }
    }

    private fun localization(key: String, language: Language) : String{
        return messageSource.getMessage(key, null, Locale(language.name))
    }

    private fun trimHTMLTags(text: String): String {
        return text.replace("<[^>]+>|&nbsp;".toRegex(), "")
    }

    private fun bold(text: String): String {
        return "<b>${trimHTMLTags(text)}</b>"
    }

    private fun quote(text: String): String {
        return "<blockquote>${trimHTMLTags(text)}</blockquote>"
    }

    private fun getEmployeesAttendance(
        date: Long,
        organizationId: Long?,
    ): List<EmployeeAttendanceResponse>? {

        val organization = organizationRepository.findByIdAndDeletedFalse(organizationId!!) ?: throw OrganizationNotFoundException()
        val localDate = date.localDate()
        val workingDateConfig =  workingDateConfigRepository.findByOrganizationIdAndDayAndDeletedFalse(
            organizationId,
            localDate.weekDay()) ?: return null

        val startWorkTime = convertToDate(localDate, workingDateConfig.startHour)
        val lateTime = startWorkTime.plusMin(15)

        val query = """
            with attendance_employees as (
                select
                    e.id                                               as employee_id,
                    u.id                                               as user_id,
                    u.full_name                                        as full_name,
                    p.name                                             as position_name,
                    f.hash_id                                          as image_hash_id,
                    min(case when ut.type = 'IN' then ut.time end )    as start_date
                from
               employee e
                    join users u on e.user_id = u.id
                    join position p on p.id = e.position_id
                    left join (select * from user_tourniquet as  inner_ut
                                                  where inner_ut.organization_id = ${organization.id} 
                    and inner_ut.deleted = false) as ut 
                        on ut.user_id = u.id
                        and cast(ut.time as date) = '$localDate'
                    left join table_date td 
                        on ut.table_date_id = td.id
                        and td.type = 'WORK_DAY'
                    left join file_asset f 
                        on e.image_asset_id = f.id
                where e.ph_status = 'BUSY'
                and e.status = 'ACTIVE'
                and e.organization_id = ${organization.id}
                and e.deleted = false
                group by e.id, u.id, f.id, p.name)
            
            select *
            from attendance_employees
            order by start_date
        """.trimIndent()

        val employeestatistics = jdbcTemplate.query(query, ResultSetExtractor { rs ->
            val list = mutableListOf<EmployeeAttendanceResponse>()
            while (rs.next()) {
                val startDate = rs.getTimestamp("start_date")?.toInstant()
                val employeeStatus = when {
                    startDate == null -> AttendanceStatus.ABSENT
                    startDate <= lateTime.toInstant() -> AttendanceStatus.ON_TIME
                    else -> AttendanceStatus.LATE
                }
                list.add(
                    EmployeeAttendanceResponse(
                        employeeId = rs.getLong("employee_id"),
                        userId = rs.getLong("user_id"),
                        fullName = rs.getString("full_name"),
                        positionName = rs.getString("position_name"),
                        imageHashId = rs.getString("image_hash_id"),
                        time = rs.getTimestamp("start_date"),
                        state = employeeStatus
                    )
                )
            }
            list
        })
        return employeestatistics
    }
}