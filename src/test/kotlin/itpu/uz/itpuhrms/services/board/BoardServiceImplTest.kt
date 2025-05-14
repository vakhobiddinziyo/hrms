package itpu.uz.itpuhrms.services.board

import io.mockk.*
import itpu.uz.itpuhrms.*
import itpu.uz.itpuhrms.services.ExtraService
import itpu.uz.itpuhrms.services.project.ProjectRepository
import itpu.uz.itpuhrms.services.projectEmployee.ProjectEmployeeRepository
import itpu.uz.itpuhrms.services.stateTemplate.StateTemplateService
import itpu.uz.itpuhrms.services.validation.ValidationService
import org.junit.jupiter.api.*
import org.springframework.jdbc.core.JdbcTemplate

class BoardServiceImplTest {

    private lateinit var boardRepository: BoardRepository
    private lateinit var projectRepository: ProjectRepository
    private lateinit var extraService: ExtraService
    private lateinit var validationService: ValidationService
    private lateinit var jdbcTemplate: JdbcTemplate
    private lateinit var stateTemplateService: StateTemplateService
    private lateinit var projectEmployeeRepository: ProjectEmployeeRepository

    private lateinit var boardService: BoardServiceImpl

    @BeforeEach
    fun setup() {
        boardRepository = mockk()
        projectRepository = mockk()
        extraService = mockk()
        validationService = mockk()
        jdbcTemplate = mockk()
        stateTemplateService = mockk()
        projectEmployeeRepository = mockk()

        boardService = BoardServiceImpl(
            boardRepository,
            projectRepository,
            extraService,
            validationService,
            jdbcTemplate,
            stateTemplateService,
            projectEmployeeRepository
        )
    }

    @Test
    fun `create should save board and return response`() {
        // given
        val request = BoardRequest(name = "New Board", projectId = 1L)

        val organization = Organization(name = "Test Org", description = null, status = Status.ACTIVE, tin = "123456")
        organization.id = 99L

        val department = Department(
            name = "IT",
            description = null,
            departmentType = DepartmentType.IT,
            organization = organization,
            status = Status.ACTIVE
        )
        department.id = 77L

        val user = User(
            fullName = "John Doe",
            phoneNumber = "998901234567",
            username = "johndoe",
            password = "secret",
            status = Status.ACTIVE,
            mail = "john@example.com",
            role = Role.USER
        ).apply { id = 88L }

        val employee = Employee(
            user = user,
            code = "EMP001",
            status = Status.ACTIVE,
            atOffice = true,
            position = Position("Dev", Level.LEAD, organization, mutableSetOf()).apply { id = 55L },
            department = department,
            organization = organization,
            phStatus = PositionHolderStatus.BUSY,
            workRate = 1.0,
            laborRate = 100,
            permissions = mutableSetOf(),
            imageAsset = null
        ).apply { id = 101L }

        val project = Project(
            name = "HRMS",
            owner = employee,
            department = department
        ).apply { id = 1L }

        val savedBoard = Board(
            name = request.name,
            project = project,
            owner = employee,
            status = BoardStatus.ACTIVE
        ).apply { id = 10L }

        every { extraService.getEmployeeFromCurrentUser() } returns employee
        every { projectRepository.findByIdAndDeletedFalse(1L) } returns project
        every { projectEmployeeRepository.findByProjectIdAndEmployeeIdAndDeletedFalse(1L, 101L) } returns mockk()
        every { validationService.validateDifferentOwners(any()) } just Runs
        every { boardRepository.save(any()) } returns savedBoard
        every { stateTemplateService.saveDefaultStates(savedBoard) } just Runs

        // when
        val response = boardService.create(request)

        // then
        Assertions.assertEquals(savedBoard.name, response.name)
        Assertions.assertEquals(savedBoard.status, response.status)
        Assertions.assertEquals(savedBoard.project.id, response.projectId)
        Assertions.assertEquals(savedBoard.owner.id, response.ownerId)

        verify { boardRepository.save(any()) }
        verify { validationService.validateDifferentOwners(any()) }
        verify { stateTemplateService.saveDefaultStates(savedBoard) }
    }
}