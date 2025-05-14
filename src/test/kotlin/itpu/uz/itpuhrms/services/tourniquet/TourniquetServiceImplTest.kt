package itpu.uz.itpuhrms.services.tourniquet

import io.mockk.*
import itpu.uz.itpuhrms.*
import itpu.uz.itpuhrms.services.ExtraService
import itpu.uz.itpuhrms.services.organization.OrgAdminResponse
import itpu.uz.itpuhrms.services.tourniquetClient.TourniquetClientRepository
import itpu.uz.itpuhrms.services.validation.ValidationService
import org.junit.jupiter.api.*
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.security.crypto.password.PasswordEncoder


class TourniquetServiceImplTest {

    private lateinit var repository: TourniquetRepository
    private lateinit var aesEncryptionService: AESEncryptionService
    private lateinit var validationService: ValidationService
    private lateinit var clientRepository: TourniquetClientRepository
    private lateinit var passwordEncoder: PasswordEncoder
    private lateinit var extraService: ExtraService

    private lateinit var service: TourniquetServiceImpl

    @BeforeEach
    fun setUp() {
        repository = mockk()
        aesEncryptionService = mockk()
        validationService = mockk()
        clientRepository = mockk()
        passwordEncoder = mockk()
        extraService = mockk()

        mockkObject(TourniquetResponse)
        service = TourniquetServiceImpl(repository, aesEncryptionService, validationService, clientRepository, passwordEncoder, extraService)
    }

    @Test
    fun `create should save tourniquet and return response`() {
        val request = TourniquetRequest("192.168.1.1", "T1", "admin", "12345", TourniquetType.DEFAULT, "desc")
        val org = Organization("Org", null, Status.ACTIVE, "123456").apply { id = 1L }
        val encodedPass = "encodedPass"

        val saved = Tourniquet(org, request.ip, request.name, request.username, encodedPass, request.type, request.description).apply { id = 10L }
        val response = TourniquetResponse(
            id = saved.id!!,
            ip = saved.ip,
            name = saved.name,
            username = saved.username,
            type = saved.type,
            description = saved.description,
            organization = OrgAdminResponse.toDto(org)
        )

        every { extraService.getOrgFromCurrentUser() } returns org
        every { repository.existsByName(request.name) } returns false
        every { aesEncryptionService.encode(request.password) } returns encodedPass
        every { repository.save(any()) } returns saved
        every { TourniquetResponse.toResponse(saved) } returns response

        val result = service.create(request)

        Assertions.assertEquals(response.id, result.id)
        Assertions.assertEquals(response.name, result.name)
    }

    @Test
    fun `getByOrganization should return tourniquet page`() {
        val org = Organization("Org", null, Status.ACTIVE, "123456").apply { id = 1L }
        val pageable = PageRequest.of(0, 10)
        val entity = Tourniquet(org, "192.168.1.1", "T1", "admin", "pass", TourniquetType.DEFAULT, "desc").apply { id = 20L }
        val response = TourniquetResponse(entity.id!!, entity.ip, entity.name, entity.username, entity.description, entity.type, OrgAdminResponse.toDto(org))

        every { extraService.getOrgFromCurrentUser() } returns org
        every { repository.findAllByOrganizationIdAndDeletedFalse(org.id!!, pageable) } returns PageImpl(listOf(entity))
        every { TourniquetResponse.toResponse(entity) } returns response

        val result = service.getByOrganization(pageable)

        Assertions.assertEquals(1, result.totalElements)
        Assertions.assertEquals("T1", result.content[0].name)
    }

}