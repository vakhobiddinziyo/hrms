package itpu.uz.itpuhrms.services.workingDate

import io.mockk.*
import itpu.uz.itpuhrms.*
import itpu.uz.itpuhrms.services.ExtraService
import itpu.uz.itpuhrms.services.organization.OrgAdminResponse
import itpu.uz.itpuhrms.services.validation.ValidationService
import org.junit.jupiter.api.*
import java.time.LocalTime

class WorkingDateConfigServiceImplTest {

    private lateinit var repository: WorkingDateConfigRepository
    private lateinit var extraService: ExtraService
    private lateinit var validationService: ValidationService

    private lateinit var service: WorkingDateConfigServiceImpl

    @BeforeEach
    fun setup() {
        repository = mockk()
        extraService = mockk()
        validationService = mockk()

        service = WorkingDateConfigServiceImpl(repository, extraService, validationService)
    }

    @Test
    fun `create should save valid working date config`() {
        val organization = Organization("Org", null, Status.ACTIVE, "123456").apply { id = 1L }

        val request = WorkingDateConfigRequest(
            startHour = LocalTime.of(9, 0),
            endHour = LocalTime.of(18, 0),
            requiredMinutes = 480,
            day = 1
        )

        val saved = WorkingDateConfig(
            organization,
            request.startHour,
            request.endHour,
            request.requiredMinutes,
            DayOfWeek.MONDAY
        ).apply { id = 1L }

        every { extraService.getOrgFromCurrentUser() } returns organization
        every { repository.existsByOrganizationIdAndDayAndDeletedFalse(1L, DayOfWeek.MONDAY) } returns false
        every { repository.save(any()) } returns saved
        mockkObject(WorkingDateConfigResponse)
        every {
            WorkingDateConfigResponse.toResponse(saved)
        } returns WorkingDateConfigResponse(
            id = 1L,
            startHour = "09:00",
            endHour = "18:00",
            requiredMinutes = request.requiredMinutes,
            day = DayOfWeek.MONDAY,
            organization = OrgAdminResponse.toDto(organization)
        )

        val result = service.create(request)

        Assertions.assertEquals(1L, result.id)
        Assertions.assertEquals(request.startHour, LocalTime.parse(result.startHour))
        Assertions.assertEquals(request.endHour, LocalTime.parse(result.endHour))
        Assertions.assertEquals(DayOfWeek.MONDAY, result.day)
    }

    @Test
    fun `create should throw if startHour is after endHour`() {
        val organization = Organization("Org", null, Status.ACTIVE, "123456").apply { id = 1L }

        val request = WorkingDateConfigRequest(
            startHour = LocalTime.of(18, 0),
            endHour = LocalTime.of(9, 0),
            requiredMinutes = 480,
            day = 1
        )

        every { extraService.getOrgFromCurrentUser() } returns organization

        Assertions.assertThrows(InvalidStartTimeException::class.java) {
            service.create(request)
        }
    }
}