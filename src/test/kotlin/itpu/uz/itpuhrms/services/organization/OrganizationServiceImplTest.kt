
package itpu.uz.itpuhrms.services.organization

import io.mockk.*
import itpu.uz.itpuhrms.*
import itpu.uz.itpuhrms.services.ExtraService
import itpu.uz.itpuhrms.services.department.DepartmentRepository
import itpu.uz.itpuhrms.services.workingDate.WorkingDateConfigRepository
import org.junit.jupiter.api.*


class OrganizationServiceImplTest {

    private lateinit var organizationRepository: OrganizationRepository
    private lateinit var departmentRepository: DepartmentRepository
    private lateinit var workingDateConfigRepository: WorkingDateConfigRepository
    private lateinit var extraService: ExtraService

    private lateinit var service: OrganizationServiceImpl

    @BeforeEach
    fun setup() {
        organizationRepository = mockk()
        departmentRepository = mockk()
        workingDateConfigRepository = mockk()
        extraService = mockk()

        service = OrganizationServiceImpl(
            organizationRepository,
            departmentRepository,
            workingDateConfigRepository,
            extraService
        )
    }

    @Test
    fun `create should save new organization and return response`() {

        val request = OrgRequest(
            name = "Test Org",
            description = "Test Description",
            status = Status.ACTIVE,
            tin = "123456789"
        )

        val savedOrg = Organization(
            name = request.name,
            description = request.description,
            status = request.status,
            tin = request.tin
        ).apply { id = 1L }

        every { organizationRepository.existsByTinAndDeletedFalse(request.tin) } returns false
        every { organizationRepository.save(any()) } returns savedOrg
        every { departmentRepository.save(any()) } returnsArgument 0
        every { workingDateConfigRepository.saveAll(any<List<WorkingDateConfig>>()) } returnsArgument 0
        mockkObject(OrgAdminResponse.Companion)
        every { OrgAdminResponse.toDto(savedOrg) } returns OrgAdminResponse(1L, request.name, request.description, request.status, request.tin, true,
            granted = true
        )


        val result = service.create(request)


        Assertions.assertEquals(request.name, result.name)
        Assertions.assertEquals(request.tin, result.tin)

        verify { organizationRepository.existsByTinAndDeletedFalse(request.tin) }
        verify { organizationRepository.save(any()) }
        verify { departmentRepository.save(any()) }
        verify { workingDateConfigRepository.saveAll(any<List<WorkingDateConfig>>()) }
    }
}
