package itpu.uz.itpuhrms.services

import itpu.uz.itpuhrms.*
import org.springframework.context.support.ResourceBundleMessageSource
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.telegram.telegrambots.meta.api.methods.ParseMode
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException
import itpu.uz.itpuhrms.Constants.OTP_EXPIRE_MIN
import itpu.uz.itpuhrms.Constants.OTP_MESSAGE_INTERVAL_LIMIT_MIN
import itpu.uz.itpuhrms.Constants.OTP_MESSAGE_LIMIT
import itpu.uz.itpuhrms.Constants.OTP_MESSAGE_RETRY_INTERVAL_LIMIT_MIN
import itpu.uz.itpuhrms.TaskAction.*
import java.time.LocalDateTime
import java.util.*

interface TelegramNotificationService {
    fun getHash(): OtpDto
    fun sendMessage(
        request: TaskActionHistoryRequest,
        date: LocalDateTime
    )
}


const val BOT_USERNAME = "hrms_uzbot"

@Service
class TelegramNotificationServiceImpl(
    private val extraService: ExtraService,
    private val messageRepository: MessageRepository,
    private val messageSourceService: MessageSourceService,
    private val taskRepository: TaskRepository,
    private val userRepository: UserRepository,
    private val botSender: BotSender,
    private val employeeRepository: EmployeeRepository,
    private val fileAssetRepository: FileAssetRepository,
    private val commentRepository: CommentRepository,
    private val timeTrackingRepository: TimeTrackingRepository,
    private val stateRepository: StateRepository,
    private val boardSettingsRepository: BoardNotificationSettingsRepository,
    private val taskSubscriberRepository: TaskSubscriberRepository,
) : TelegramNotificationService {

    @Transactional
    override fun getHash(): OtpDto {
        val employee = extraService.getEmployeeFromCurrentUser()
        val hashId = generateSecureToken()
        val count = messageRepository.getLastMessagesAmount(employee.id!!, OTP_MESSAGE_INTERVAL_LIMIT_MIN, BOT_USERNAME)
        if (count >= OTP_MESSAGE_LIMIT) throw OtpMessageLimitException()

        val message = Message(
            hashId,
            BOT_USERNAME,
            employee,
            false,
            1
        )
        val lastMessage = messageRepository.findLastMessageAtInterval(employee.id!!, OTP_MESSAGE_INTERVAL_LIMIT_MIN, BOT_USERNAME)
        lastMessage?.let {
            if (!it.used) {
                if (it.createdDate!!.time + it.retryCount * 60 * 1000 > Date().time) throw OtpProhibitedException()
                message.retryCount = it.retryCount
            }
        }
        messageRepository.save(message)
        return OtpDto(
            hashId,
            BOT_USERNAME,
            OTP_MESSAGE_RETRY_INTERVAL_LIMIT_MIN * message.retryCount,
            OTP_EXPIRE_MIN
        )
    }

    override fun sendMessage(
        request: TaskActionHistoryRequest,
        date: LocalDateTime
    ) {
        val task = taskRepository.findByIdAndDeletedFalse(request.taskId)
            ?: throw TaskNotFoundException()
        val owner = userRepository.findByIdAndDeletedFalse(request.ownerId)
            ?: throw UserNotFoundException()
        val settings = boardSettingsRepository.findByBoardIdAndDeletedFalse(task.board.id!!)
        val subscribers = taskSubscriberRepository.findAllByTaskIdAndDeletedFalse(task.id!!)
            .map {
                it.subscriber
            }

        if (settings != null && settings.actions.contains(request.action)) {
            when (request.action) {
                CREATE_TASK -> sendCreateTaskMessage(subscribers, request, date, owner, task)
                CHANGE_TITLE -> sendTitleChangeMessage(subscribers, request, date, owner, task)

                ASSIGN_USER,
                REMOVE_USER -> sendAssignAndRemoveUserMessage(subscribers, request, date, owner, task)

                CREATE_SUB_TASK -> sendCreateSubTaskMessage(subscribers, request, date, owner, task)
                REMOVE_SUB_TASK -> sendRemoveSubTaskMessage(subscribers, request, date, owner, task)

                SET_START_DATE,
                CHANGE_START_DATE,
                REMOVE_START_DATE -> sendStartDateMessage(subscribers, request, date, owner, task)

                SET_DUE_DATE,
                CHANGE_DUE_DATE,
                REMOVE_DUE_DATE -> sendDueDateMessage(subscribers, request, date, owner, task)

                SET_TASK_PRIORITY,
                CHANGE_TASK_PRIORITY,
                REMOVE_TASK_PRIORITY -> sendTaskPriorityMessage(subscribers, request, date, owner, task)

                CHANGE_STATE -> sendStateChangeMessage(subscribers, request, date, owner, task)

                FILE_UPLOAD,
                REMOVE_FILE -> sendFileUploadAndRemoveMessage(subscribers, request, date, owner, task)

                ADD_COMMENT -> sendTaskCommentMessage(subscribers, request, date, owner, task)
                TIME_TRACKED -> sendTimeTrackedMessage(subscribers, request, date, owner, task)

                TIME_AMOUNT_ESTIMATED,
                ESTIMATED_TIME_AMOUNT_REMOVED -> sendEstimatedTimeMessage(subscribers, request, date, owner, task)
            }
        }
    }


    private fun sendCreateTaskMessage(
        subscribers: List<Subscriber>,
        request: TaskActionHistoryRequest,
        date: LocalDateTime,
        actionOwner: User,
        task: Task
    ) {
        val employees = task.employees.map { it.employee }.toMutableList()

        subscribers.forEach { subscriber ->
            val baseMessage = defaultMessage(subscriber, date, task, actionOwner, request.action)

            val fullMessage = buildString {
                append(baseMessage)
                if (task.priority != TaskPriority.NONE) {
                    append(priorityWord(subscriber))
                    append(" ${task.priority.localizedName.localizedNameForBot(subscriber)}\n")
                }
                if (employees.isNotEmpty()) {
                    append(employeeWord(subscriber))
                    append("${expandableBackQuoteLn(employeesNames(employees))}\n")
                }
                if (task.startDate != null) {
                    append(startDateWord(subscriber))
                    append(" ${task.startDate!!.prettyDateTimeString()}\n")
                }
                if (task.endDate != null) {
                    append(dueDateWord(subscriber))
                    append(" ${task.endDate!!.prettyDateTimeString()}\n")
                }
                if (task.timeEstimateAmount != null) {
                    append(estimatedTimeWord(subscriber))
                    append(" ${formatMinutes(task.timeEstimateAmount!!.toLong(), subscriber)}\n")
                }
                if (task.files.isNotEmpty()) {
                    append(filesWord(subscriber))
                    append("${filesNames(task.files, subscriber)}\n")
                }
            }

            val sendMessage = SendMessage(subscriber.chatId, fullMessage)
            sendMessage.parseMode = ParseMode.HTML
            sendMessage(sendMessage)
        }
    }

    private fun sendTitleChangeMessage(
        subscribers: List<Subscriber>,
        request: TaskActionHistoryRequest,
        date: LocalDateTime,
        actionOwner: User,
        task: Task
    ) {
        subscribers.forEach { subscriber ->
            val organizationName = task.board.project.department.organization.name
            val message = buildString {
                append("${bold(localizedActionMessage(request.action, subscriber))}:\n\n")
                append("${date.prettyString()}\n")
                append(organizationWord(subscriber)).append(" ${trimHTMLTags(organizationName)}\n")
                append(projectWord(subscriber)).append(" ${trimHTMLTags(task.board.project.name)}\n")
                append(ownerWord(subscriber)).append(" ${trimHTMLTags(actionOwner.fullName)}\n")
                append(titleWord(subscriber)).append(" ${expandableBackQuote(request.title!!)}\n")
            }

            val sendMessage = SendMessage(subscriber.chatId, message)
            sendMessage.parseMode = ParseMode.HTML
            sendMessage(sendMessage)
        }
    }

    private fun sendCreateSubTaskMessage(
        subscribers: List<Subscriber>,
        request: TaskActionHistoryRequest,
        date: LocalDateTime,
        actionOwner: User,
        task: Task
    ) {
        subscribers.forEach { subscriber ->
            val baseMessage = defaultMessage(subscriber, date, task.parentTask!!, actionOwner, request.action)

            val fullMessage = buildString {
                append(baseMessage)
                append(subtasksWord(subscriber))
                append("${expandableBackQuoteLn(subtasksNames(mutableListOf(task)))}\n")
            }

            val sendMessage = SendMessage(subscriber.chatId, fullMessage)
            sendMessage.parseMode = ParseMode.HTML
            sendMessage(sendMessage)
        }
    }


    private fun sendRemoveSubTaskMessage(
        subscribers: List<Subscriber>,
        request: TaskActionHistoryRequest,
        date: LocalDateTime,
        actionOwner: User,
        task: Task
    ) {
        val subtasks = taskRepository.findAllById(request.subtasks!!)

        subscribers.forEach { subscriber ->
            val baseMessage = defaultMessage(subscriber, date, task, actionOwner, request.action)

            val fullMessage = buildString {
                append(baseMessage)
                append(subtasksWord(subscriber))
                append("${expandableBackQuoteLn(subtasksNames(subtasks))}\n")
            }

            val sendMessage = SendMessage(subscriber.chatId, fullMessage)
            sendMessage.parseMode = ParseMode.HTML
            sendMessage(sendMessage)
        }
    }

    private fun sendAssignAndRemoveUserMessage(
        subscribers: List<Subscriber>,
        request: TaskActionHistoryRequest,
        date: LocalDateTime,
        actionOwner: User,
        task: Task
    ) {
        val employees = employeeRepository.findAllById(request.subjectEmployeeIds!!)

        subscribers.forEach { subscriber ->
            val baseMessage = defaultMessage(subscriber, date, task, actionOwner, request.action)

            val fullMessage = buildString {
                append(baseMessage)
                append(employeeWord(subscriber))
                append("${expandableBackQuoteLn(employeesNames(employees))}\n")
            }

            val sendMessage = SendMessage(subscriber.chatId, fullMessage)
            sendMessage.parseMode = ParseMode.HTML
            sendMessage(sendMessage)
        }
    }


    private fun sendFileUploadAndRemoveMessage(
        subscribers: List<Subscriber>,
        request: TaskActionHistoryRequest,
        date: LocalDateTime,
        actionOwner: User,
        task: Task
    ) {
        val files = fileAssetRepository.findAllByHashIdInAndDeletedFalse(request.fileHashIds!!)

        subscribers.forEach { subscriber ->
            val baseMessage = defaultMessage(subscriber, date, task, actionOwner, request.action)

            val fullMessage = buildString {
                append(baseMessage)
                append(filesWord(subscriber))
                append("${filesNames(files, subscriber)}\n")
            }

            val sendMessage = SendMessage(subscriber.chatId, fullMessage)
            sendMessage.parseMode = ParseMode.HTML
            sendMessage(sendMessage)
        }
    }

    private fun sendStateChangeMessage(
        subscribers: List<Subscriber>,
        request: TaskActionHistoryRequest,
        date: LocalDateTime,
        actionOwner: User,
        task: Task
    ) {
        subscribers.forEach { subscriber ->
            val baseMessage = defaultMessage(subscriber, date, task, actionOwner, request.action)

            val fullMessage = buildString {
                append(baseMessage)
                append(stateWord(subscriber))
                append(" ${stateNames(request)}\n")
            }

            val sendMessage = SendMessage(subscriber.chatId, fullMessage)
            sendMessage.parseMode = ParseMode.HTML
            sendMessage(sendMessage)
        }
    }


    private fun sendTaskCommentMessage(
        subscribers: List<Subscriber>,
        request: TaskActionHistoryRequest,
        date: LocalDateTime,
        actionOwner: User,
        task: Task
    ) {
        val comment = commentRepository.findByIdAndDeletedFalse(request.commentId!!) ?: return

        subscribers.forEach { subscriber ->
            val baseMessage = defaultMessage(subscriber, date, task, actionOwner, request.action)
            val fileNames = comment.files?.let { filesNames(it, subscriber) } ?: ""

            val fullMessage = buildString {
                append(baseMessage)
                append(commentWord(subscriber))
                append(" ${expandableBackQuoteLn(comment.text)}\n")

                if (fileNames.isNotEmpty()) {
                    append(filesWord(subscriber))
                    append("$fileNames\n")
                }
            }

            val sendMessage = SendMessage(subscriber.chatId, fullMessage)
            sendMessage.parseMode = ParseMode.HTML
            sendMessage(sendMessage)
        }
    }

    private fun sendStartDateMessage(
        subscribers: List<Subscriber>,
        request: TaskActionHistoryRequest,
        date: LocalDateTime,
        actionOwner: User,
        task: Task
    ) {
        val startDate = request.startDate ?: return

        subscribers.forEach { subscriber ->
            val baseMessage = defaultMessage(subscriber, date, task, actionOwner, request.action)

            val fullMessage = buildString {
                append(baseMessage)
                append(startDateWord(subscriber))
                append(" ${startDate.prettyDateTimeString()}\n")
            }

            val sendMessage = SendMessage(subscriber.chatId, fullMessage)
            sendMessage.parseMode = ParseMode.HTML
            sendMessage(sendMessage)
        }
    }


    private fun sendDueDateMessage(
        subscribers: List<Subscriber>,
        request: TaskActionHistoryRequest,
        date: LocalDateTime,
        actionOwner: User,
        task: Task
    ) {
        val dueDate = request.dueDate ?: return

        subscribers.forEach { subscriber ->
            val baseMessage = defaultMessage(subscriber, date, task, actionOwner, request.action)

            val fullMessage = buildString {
                append(baseMessage)
                append(dueDateWord(subscriber))
                append(" ${dueDate.prettyDateTimeString()}\n")
            }

            val sendMessage = SendMessage(subscriber.chatId, fullMessage)
            sendMessage.parseMode = ParseMode.HTML
            sendMessage(sendMessage)
        }
    }


    private fun sendTaskPriorityMessage(
        subscribers: List<Subscriber>,
        request: TaskActionHistoryRequest,
        date: LocalDateTime,
        actionOwner: User,
        task: Task
    ) {
        val priority = request.taskPriority ?: return

        subscribers.forEach { subscriber ->
            val baseMessage = defaultMessage(subscriber, date, task, actionOwner, request.action)

            val fullMessage = buildString {
                append(baseMessage)
                append(priorityWord(subscriber))
                append(" ${priority.localizedName.localizedNameForBot(subscriber)}\n")
            }

            val sendMessage = SendMessage(subscriber.chatId, fullMessage)
            sendMessage.parseMode = ParseMode.HTML
            sendMessage(sendMessage)
        }
    }

    private fun sendTimeTrackedMessage(
        subscribers: List<Subscriber>,
        request: TaskActionHistoryRequest,
        date: LocalDateTime,
        actionOwner: User,
        task: Task
    ) {
        val timeTrackingList = timeTrackingRepository.findAllById(request.timeTrackingIds ?: return)

        subscribers.forEach { subscriber ->
            val baseMessage = defaultMessage(subscriber, date, task, actionOwner, request.action)

            val fullMessage = buildString {
                append(baseMessage)
                append(timeTrackingWord(subscriber))
                append(" ${expandableBackQuoteLn(timeTrackingNames(timeTrackingList, subscriber))}\n")
            }

            val sendMessage = SendMessage(subscriber.chatId, fullMessage)
            sendMessage.parseMode = ParseMode.HTML
            sendMessage(sendMessage)
        }
    }

    private fun sendEstimatedTimeMessage(
        subscribers: List<Subscriber>,
        request: TaskActionHistoryRequest,
        date: LocalDateTime,
        actionOwner: User,
        task: Task
    ) {
        val timeEstimateAmount = request.timeEstimateAmount ?: return

        subscribers.forEach { subscriber ->
            val baseMessage = defaultMessage(subscriber, date, task, actionOwner, request.action)

            val fullMessage = buildString {
                append(baseMessage)
                append(estimatedTimeWord(subscriber))
                append(" ${formatMinutes(timeEstimateAmount.toLong(), subscriber)}\n")
            }

            val sendMessage = SendMessage(subscriber.chatId, fullMessage)
            sendMessage.parseMode = ParseMode.HTML
            sendMessage(sendMessage)
        }
    }


    private fun organizationWord(subscriber: Subscriber): String {
        return taskActionMessage(TaskActionMessage.ORGANIZATION_NAME, subscriber)
    }

    private fun projectWord(subscriber: Subscriber): String {
        return taskActionMessage(TaskActionMessage.PROJECT_NAME, subscriber)
    }

    private fun ownerWord(subscriber: Subscriber): String {
        return taskActionMessage(TaskActionMessage.ACTION_OWNER, subscriber)
    }

    private fun startDateWord(subscriber: Subscriber): String {
        return taskActionMessage(TaskActionMessage.START_DATE, subscriber)
    }

    private fun dueDateWord(subscriber: Subscriber): String {
        return taskActionMessage(TaskActionMessage.DUE_DATE, subscriber)
    }

    private fun priorityWord(subscriber: Subscriber): String {
        return taskActionMessage(TaskActionMessage.PRIORITY, subscriber)
    }

    private fun stateWord(subscriber: Subscriber): String {
        return taskActionMessage(TaskActionMessage.STATE, subscriber)
    }

    private fun filesWord(subscriber: Subscriber): String {
        return "${taskActionMessage(TaskActionMessage.TASK_FILE, subscriber)}\n"
    }

    private fun titleWord(subscriber: Subscriber): String {
        return taskActionMessage(TaskActionMessage.TITLE, subscriber)
    }

    private fun timeTrackingWord(subscriber: Subscriber): String {
        return "${taskActionMessage(TaskActionMessage.TIME_TRACKED, subscriber)}\n"
    }

    private fun estimatedTimeWord(subscriber: Subscriber): String {
        return taskActionMessage(TaskActionMessage.ESTIMATED_TIME, subscriber)
    }

    private fun commentWord(subscriber: Subscriber): String {
        return "${taskActionMessage(TaskActionMessage.TASK_COMMENT, subscriber)}\n"
    }

    private fun subtasksWord(subscriber: Subscriber): String {
        return "${taskActionMessage(TaskActionMessage.SUB_TASKS, subscriber)}\n"
    }

    private fun employeeWord(subscriber: Subscriber): String {
        return "${taskActionMessage(TaskActionMessage.EMPLOYEE, subscriber)}\n"
    }

    private fun taskActionMessage(action: TaskActionMessage, subscriber: Subscriber): String {
        return messageSourceService.getMessage(action, subscriber.language)
    }

    private fun localizedActionMessage(action: TaskAction, subscriber: Subscriber): String {
        return messageSourceService.getMessage(action, subscriber.language)
    }

    private fun subtasksNames(subtasks: MutableList<Task>): String {
        val buffer = StringBuffer()
        for (index in subtasks.indices) {
            if (index > 9) {
                buffer.append("...")
                break
            }
            buffer.append("${index + 1}.").append("${subtasks[index].title}\n")
        }
        return buffer.toString()
    }

    private fun stateNames(request: TaskActionHistoryRequest): String {
        val toState = stateRepository.findByIdAndDeletedFalse(request.toStateId!!)
        val fromState = stateRepository.findByIdAndDeletedFalse(request.fromStateId!!)
        return "${fromState?.name ?: ""} >> ${toState?.name ?: ""}"
    }

    private fun employeesNames(employees: MutableList<Employee>): String {
        val buffer = StringBuffer()
        for (index in employees.indices) {
            if (index > 9) {
                buffer.append("...")
                break
            }
            buffer.append(("${index + 1}.")).append("${employees[index].user?.fullName}\n")
        }
        return buffer.toString()
    }

    private fun timeTrackingNames(timeTrackingList: MutableList<TimeTracking>, subscriber: Subscriber): String {
        val buffer = StringBuffer()
        for (index in timeTrackingList.indices) {
            if (index > 9) {
                buffer.append("...")
                break
            }
            val timeTracking = timeTrackingList[index]
            val date = timeTracking.tableDate.date
            buffer
                .append(("${index + 1}."))
                .append("${date.prettyDateString()}: ${formatMinutes(timeTracking.duration, subscriber)}\n")
        }
        return buffer.toString()
    }

    private fun formatMinutes(minutes: Long, subscriber: Subscriber): String {
        val hours = minutes / 60
        val remainingMinutes = minutes % 60
        return when (subscriber.language) {
            Language.ENG -> {
                "${if (hours >= 1) "$hours hours" else ""} ${if (remainingMinutes >= 1) "$remainingMinutes minutes" else ""}"
            }

            Language.RU -> {
                "${if (hours >= 1) "$hours час" else ""} ${if (remainingMinutes >= 1) "$remainingMinutes минут" else ""}"
            }

            Language.UZ -> {
                "${if (hours >= 1) "$hours soat" else ""} ${if (remainingMinutes >= 1) "$remainingMinutes minut" else ""}"
            }
        }
    }


    private fun filesNames(files: MutableList<FileAsset>, subscriber: Subscriber): String {
        val buffer = StringBuffer()
        for (index in files.indices) {
            if (index > 9) {
                buffer.append("...")
            }
            buffer.append("${link(files[index].hashId, subscriber)}\n")
        }
        return buffer.toString()
    }

    private fun url(hashId: String): String {
        return "https://hrpro.uz/api/v1/file/${hashId}"
    }

    private fun link(hashId: String, subscriber: Subscriber): String {
        return "<a href=\"${url(hashId)}\">${fileKeyWord(subscriber)}</a>"
    }

    private fun fileKeyWord(subscriber: Subscriber): String {
        return when (subscriber.language) {
            Language.UZ -> "Fayl"
            Language.ENG -> "File"
            Language.RU -> "Файл"
        }
    }


    private fun trimHTMLTags(text: String): String {
        return text.replace("<[^>]+>|&nbsp;".toRegex(), "")
    }

    private fun expandableBackQuoteLn(text: String): String {
        return "<blockquote expandable>${trimHTMLTags(text)}</blockquote>"
    }

    private fun expandableBackQuote(text: String): String {
        return "<blockquote expandable>${trimHTMLTags(text)}</blockquote>"
    }

    private fun bold(text: String): String {
        return "<b>${trimHTMLTags(text)}</b>"
    }

    private fun defaultMessage(
        subscriber: Subscriber,
        date: LocalDateTime,
        task: Task,
        actionOwner: User,
        action: TaskAction
    ): String {
        val organizationName = task.board.project.department.organization.name
        return buildString {
            append("${bold(localizedActionMessage(action, subscriber))}:\n\n")
            append("${date.prettyString()}\n")
            append(organizationWord(subscriber)).append(" ${trimHTMLTags(organizationName)}\n")
            append(projectWord(subscriber)).append(" ${trimHTMLTags(task.board.project.name)}\n")
            append(ownerWord(subscriber)).append(" ${trimHTMLTags(actionOwner.fullName)}\n")
            append(titleWord(subscriber)).append(" ${expandableBackQuote(task.title)}\n")
        }
    }

    private fun sendMessage(sendMessage: SendMessage) {
        try {
            botSender.sendMessage(BOT_USERNAME, sendMessage)
        } catch (ex: TelegramApiRequestException) {
            ex.printStackTrace()
        }
    }
}


interface MessageSourceService {
    fun getMessage(sourceKey: TaskAction, language: Language): String
    fun getMessage(sourceKey: TaskActionMessage, language: Language): String
    fun getMessage(sourceKey: TaskAction, any: Array<String>, language: Language): String
}

@Service
class MessageSourceServiceImpl(
    val messageResourceBundleMessageSource: ResourceBundleMessageSource
) :
    MessageSourceService {

    override fun getMessage(sourceKey: TaskAction, language: Language): String {
        return messageResourceBundleMessageSource.getMessage(sourceKey.name, null, Locale(language.name))
    }

    override fun getMessage(sourceKey: TaskActionMessage, language: Language): String {
        return messageResourceBundleMessageSource.getMessage(sourceKey.name, null, Locale(language.name))
    }

    override fun getMessage(sourceKey: TaskAction, any: Array<String>, language: Language): String {
        return messageResourceBundleMessageSource.getMessage(sourceKey.name, any, Locale(language.name))
    }
}
