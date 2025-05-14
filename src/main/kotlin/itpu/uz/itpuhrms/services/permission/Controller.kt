package itpu.uz.itpuhrms.services.permission


import itpu.uz.itpuhrms.utils.BASE_API
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("$BASE_API/permissions")
class PermissionController(
    private val permissionService: PermissionService
) {
    @PreAuthorize("hasAnyAuthority('DEVELOPER', 'ADMIN')")
    @PostMapping
    fun create(@Valid @RequestBody request: PermissionRequest) = permissionService.create(request)

    //    @PreAuthorize("hasAnyAuthority('DEVELOPER', 'ADMIN')")
    @GetMapping("{id}")
    fun getById(@PathVariable id: Long) = permissionService.getById(id)

    //    @PreAuthorize("hasAnyAuthority('DEVELOPER', 'ADMIN')")
    @GetMapping
    fun getAll() = permissionService.getAll()

    @PreAuthorize("hasAnyAuthority('DEVELOPER', 'ADMIN')")
    @PutMapping("{id}")
    fun update(@PathVariable id: Long, @Valid @RequestBody updateRequest: PermissionRequest) =
        permissionService.update(id, updateRequest)

    @PreAuthorize("hasAnyAuthority('DEVELOPER', 'ADMIN')")
    @DeleteMapping("{id}")
    fun delete(@PathVariable id: Long) = permissionService.delete(id)
}

