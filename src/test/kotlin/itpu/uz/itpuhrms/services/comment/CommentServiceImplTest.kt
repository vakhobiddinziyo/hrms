package itpu.uz.itpuhrms.services.comment

import io.mockk.*
import itpu.uz.itpuhrms.*
import itpu.uz.itpuhrms.services.*
import itpu.uz.itpuhrms.services.task.TaskRepository
import itpu.uz.itpuhrms.services.taskActionHistory.*
import itpu.uz.itpuhrms.services.file.FileAssetRepository
import itpu.uz.itpuhrms.services.user.UserDto
import itpu.uz.itpuhrms.services.validation.ValidationService
import org.junit.jupiter.api.*
import java.util.*

class CommentServiceImplTest {

    private lateinit var commentRepository: CommentRepository
    private lateinit var fileAssetRepository: FileAssetRepository
    private lateinit var taskRepository: TaskRepository
    private lateinit var extraService: ExtraService
    private lateinit var validationService: ValidationService
    private lateinit var actionHistoryService: TaskActionHistoryService
    private lateinit var taskActionHistoryRepository: TaskActionHistoryRepository

    private lateinit var commentService: CommentServiceImpl

    @BeforeEach
    fun setup() {
        commentRepository = mockk()
        fileAssetRepository = mockk()
        taskRepository = mockk()
        extraService = mockk()
        validationService = mockk()
        actionHistoryService = mockk()
        taskActionHistoryRepository = mockk()

        commentService = CommentServiceImpl(
            commentRepository,
            fileAssetRepository,
            taskRepository,
            extraService,
            validationService,
            actionHistoryService,
            taskActionHistoryRepository
        )
    }

    @Test
    fun `create should save comment and return task action history response`() {
        val request = CommentRequest(
            taskId = 100L,
            text = "Test comment",
            files = listOf("file123")
        )

        val organization = Organization("Org", null, Status.ACTIVE, "123456").apply { id = 1L }
        val user = User("Full Name", "998901234567", "username", "pass", Status.ACTIVE, "mail@mail.com", Role.USER).apply { id = 10L }
        val file = FileAsset("file.jpg", "jpg", 1000L, "image/jpeg", "file123", "uploads", "file.jpg").apply { id = 20L }
        val department = Department("Dep", null, DepartmentType.IT, organization, null).apply { id = 30L }
        val position = Position("Dev", Level.LEAD, organization, mutableSetOf()).apply { id = 40L }
        val employee = Employee(user, "CODE", Status.ACTIVE, true, position, department, organization, PositionHolderStatus.BUSY, 1.0, 100, mutableSetOf()).apply { id = 50L }

        val project = Project("Project", employee, department).apply { id = 60L }
        val board = Board("Board", project, employee).apply { id = 70L }
        val state = State("TO DO", 1, board)
        val task = Task("Task", mutableSetOf(), state, board, mutableListOf(), 1, TaskPriority.HIGH, user).apply { id = 100L }

        val comment = Comment(task, "Test comment", user, mutableListOf(file)).apply { id = 200L }

        val createdAt = Date(10000000L)

        val action = TaskActionHistory(
            task = task,
            owner = user,
            action = TaskAction.ADD_COMMENT,
            files = mutableListOf(file),
            fromState = null,
            toState = null,
            subjectEmployee = null,
            subTask = null,
            comment = comment,
            dueDate = Date(),
            startDate = Date(),
            title = "Comment title",
            priority = TaskPriority.HIGH,
            timeEstimateAmount = 120,
        ).apply {
            id = 300L
        }

        val expectedResponse = TaskActionHistoryResponse(
            id = 300L,
            taskId = task.id!!,
            owner = UserDto.toResponse(user),
            action = TaskAction.ADD_COMMENT,
            fileHashIds = listOf(file.hashId),
            fromState = null,
            toState = null,
            subjectEmployee = null,
            createdAt = createdAt.time,
            taskPriority = PriorityResponse.toResponse(task.priority),
            dueDate = action.dueDate,
            startDate = action.startDate,
            title = action.title,
            comment = CommentResponse.toDto(comment),
            timeTracking = null,
            timeEstimateAmount = 100
        )

        every { extraService.getEmployeeFromCurrentUser() } returns employee
        every { taskRepository.findByIdAndDeletedFalse(100L) } returns task
        every { validationService.validateProjectEmployee(project, employee) } just Runs
        every { fileAssetRepository.findByHashIdAndDeletedFalse("file123") } returns file
        every { commentRepository.save(any()) } returns comment
        every { actionHistoryService.create(any()) } just Runs
        every { taskActionHistoryRepository.findByCommentIdAndDeletedFalse(200L) } returns action

        val actualResponse = commentService.create(request)

        Assertions.assertEquals(expectedResponse.id, actualResponse.id)
        Assertions.assertEquals(expectedResponse.taskId, actualResponse.taskId)
        Assertions.assertEquals(expectedResponse.comment?.id, actualResponse.comment?.id)

        verify { commentRepository.save(any()) }
        verify { actionHistoryService.create(any()) }
        verify { validationService.validateProjectEmployee(project, employee) }
    }
}