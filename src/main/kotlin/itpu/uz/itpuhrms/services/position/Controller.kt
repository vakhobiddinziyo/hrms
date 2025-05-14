package itpu.uz.itpuhrms.services.position


import itpu.uz.itpuhrms.utils.BASE_API
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("$BASE_API/position")
class PositionController(
    private val service: PositionService
) {

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @PostMapping
    fun create(@Valid @RequestBody request: PositionRequest) = service.create(request)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @PutMapping("{id}")
    fun update(@PathVariable id: Long, @Valid @RequestBody request: PositionRequest) = service.update(id, request)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @DeleteMapping("{id}")
    fun delete(@PathVariable id: Long) = service.delete(id)

    @PreAuthorize("hasAnyAuthority('ADMIN')")
    @PostMapping("admin")
    fun createAdmin(@Valid @RequestBody request: PositionAdminRequest) = service.adminCreate(request)

    @PreAuthorize("hasAnyAuthority('ADMIN')")
    @PutMapping("admin/{id}")
    fun updateAdmin(@PathVariable id: Long, @Valid @RequestBody request: PositionAdminRequest) =
        service.adminUpdate(request, id)

    @PreAuthorize("hasAnyAuthority('ADMIN')")
    @DeleteMapping("admin/{id}")
    fun deleteAdmin(@PathVariable id: Long) = service.adminDelete(id)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @GetMapping("{id}")
    fun getOneById(@PathVariable id: Long) = service.getOneById(id)

    @PreAuthorize("hasAnyAuthority('ADMIN','DEVELOPER')")
    @GetMapping("console/{id}")
    fun getOneByIdForConsole(@PathVariable id: Long) = service.getOneById(id)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @GetMapping
    fun getAll(pageable: Pageable) = service.getAll(pageable)

    @PreAuthorize("hasAnyAuthority('DEVELOPER', 'ADMIN')")
    @GetMapping("console")
    fun getAllForConsole(@RequestParam orgId: Long?, pageable: Pageable) = service.getAllForConsole(orgId, pageable)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @GetMapping("list")
    fun getPositionList(@RequestParam departmentId: Long) = service.getPositionList(departmentId)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @GetMapping("employee")
    fun getPositionEmployees(pageable: Pageable) = service.getPositionEmployees(pageable)
}