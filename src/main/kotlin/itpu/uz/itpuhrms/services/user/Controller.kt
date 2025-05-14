package itpu.uz.itpuhrms.services.user

import itpu.uz.itpuhrms.*
import itpu.uz.itpuhrms.utils.BASE_API
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("$BASE_API/user")
class UserController(
    private val service: UserService
) {
    @GetMapping("roles")
    fun getRoles() = service.getAllRoles()

    @PreAuthorize("hasAnyAuthority('DEVELOPER', 'ADMIN')")
    @PostMapping("admin")
    fun createUser(@Valid @RequestBody request: UserAdminRequest) = service.createUser(request)

    @PreAuthorize("hasAnyAuthority('DEVELOPER', 'ADMIN')")
    @PutMapping("admin/{id}")
    fun updateUser(@PathVariable id: Long, @RequestBody @Valid request: UserAdminUpdateRequest) =
        service.updateUser(id, request)

    @PutMapping("client/{id}")
    fun update(@PathVariable id: Long, @Valid @RequestBody request: UserUpdateRequest) = service.update(id, request)

    @PreAuthorize("hasAnyAuthority('DEVELOPER', 'ADMIN')")
    @GetMapping("admin")
    fun getAllUserList(
        @RequestParam role: Role?,
        @RequestParam search: String?,
        pageable: Pageable
    ) = service.getAll(role, search, pageable)

    @PreAuthorize("hasAnyAuthority('DEVELOPER', 'ADMIN')")
    @GetMapping("admin/{id}")
    fun getOneAdmin(@PathVariable id: Long) = service.getOneByIdAdmin(id)

    @GetMapping("client")
    fun getAllClients(
        @RequestParam search: String?,
        @RequestParam gender: Gender?,
        pageable: Pageable
    ) = service.getAllClients(search, gender, pageable)

    @PostMapping("client")
    fun createClient(@Valid @RequestBody request: UserRequest) = service.createClient(request)

    @GetMapping("me")
    fun userMe() = service.userMe()

    @PostMapping("search")
    fun getByPinfl(@RequestBody request: PinflRequest) = service.getByPinfl(request)

    @GetMapping("{id}")
    fun getOneById(@PathVariable id: Long) = service.getOneById(id)

    @PostMapping("credentials")
    fun saveOrgAdminCredentials(@RequestBody request: UserCredentialsRequest) = service.saveOrgAdminCredentials(request)

    @PreAuthorize("hasAnyAuthority('DEVELOPER', 'ADMIN')")
    @DeleteMapping("{id}")
    fun delete(@PathVariable id: Long) = service.delete(id)
}