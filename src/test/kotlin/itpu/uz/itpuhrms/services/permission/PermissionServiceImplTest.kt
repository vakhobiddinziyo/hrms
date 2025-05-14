package itpu.uz.itpuhrms.services.permission


import io.mockk.*
import itpu.uz.itpuhrms.*

import org.junit.jupiter.api.*

class PermissionServiceImplTest {

    private lateinit var repository: PermissionRepository
    private lateinit var service: PermissionServiceImpl

    @BeforeEach
    fun setup() {
        repository = mockk()
        service = PermissionServiceImpl(repository)
    }

    @Test
    fun `create should save and return permission`() {
        val request = PermissionRequest("USER_READ", "Read access")
        val saved = Permission("USER_READ", "Read access").apply { id = 1L }

        every { repository.save(any()) } returns saved
        mockkObject(PermissionAdminResponse.Companion)
        every { PermissionAdminResponse.toDto(saved) } returns PermissionAdminResponse(1L, "USER_READ", "Read access")

        val result = service.create(request)

        Assertions.assertEquals("USER_READ", result.permissionData)
        verify(exactly = 2) { repository.save(any()) }
    }

    @Test
    fun `getById should return permission if exists`() {
        val permission = Permission("USER_DELETE", "Delete access").apply { id = 2L }

        every { repository.findByIdAndDeletedFalse(2L) } returns permission
        mockkObject(PermissionAdminResponse.Companion)
        every { PermissionAdminResponse.toDto(permission) } returns PermissionAdminResponse(2L, "USER_DELETE", "Delete access")

        val result = service.getById(2L)

        Assertions.assertEquals("USER_DELETE", result.permissionData)
    }

    @Test
    fun `getAll should return list of permissions`() {
        val list = listOf(
            Permission("USER_CREATE", "Create user").apply { id = 3L },
            Permission("USER_UPDATE", "Update user").apply { id = 4L }
        )

        every { repository.findAllNotDeleted() } returns list
        mockkObject(PermissionAdminResponse.Companion)
        every { PermissionAdminResponse.toDto(any()) } answers {
            val p = it.invocation.args[0] as Permission
            PermissionAdminResponse(p.id!!, p.permissionData, p.description)
        }

        val result = service.getAll()

        Assertions.assertEquals(2, result.size)
        Assertions.assertEquals("USER_CREATE", result[0].permissionData)
    }

    @Test
    fun `update should modify and return updated permission`() {
        val existing = Permission("OLD_DATA", "Old desc").apply { id = 5L }
        val request = PermissionRequest("NEW_DATA", "New desc")

        every { repository.findByIdAndDeletedFalse(5L) } returns existing
        every { repository.save(existing) } returns existing
        mockkObject(PermissionAdminResponse.Companion)
        every { PermissionAdminResponse.toDto(existing) } returns PermissionAdminResponse(5L, "NEW_DATA", "New desc")

        val result = service.update(5L, request)

        Assertions.assertEquals("NEW_DATA", result.permissionData)
        Assertions.assertEquals("New desc", result.description)
    }

}