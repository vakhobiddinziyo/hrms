package itpu.uz.itpuhrms.services.organization


import itpu.uz.itpuhrms.Status
import itpu.uz.itpuhrms.utils.BASE_API
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("$BASE_API/organization")
class OrganizationController(
    private val service: OrganizationService
) {
    @PreAuthorize("hasAnyAuthority('DEVELOPER', 'ADMIN')")
    @PostMapping
    fun createOrganization(@Valid @RequestBody request: OrgRequest) = service.create(request)

    @PreAuthorize("hasAnyAuthority('DEVELOPER', 'ADMIN')")
    @PutMapping("{id}")
    fun update(@PathVariable id: Long, @Valid @RequestBody request: OrgRequest) = service.update(id, request)

    @PreAuthorize("hasAnyAuthority('DEVELOPER', 'ADMIN')")
    @DeleteMapping("{id}")
    fun delete(@PathVariable id: Long) = service.delete(id)

    @GetMapping("{id}")
    fun getById(@PathVariable id: Long) = service.getById(id)

    @PreAuthorize("hasAnyAuthority('DEVELOPER', 'ADMIN')")
    @GetMapping
    fun getList(
        @RequestParam search: String?,
        @RequestParam status: Status?,
        @RequestParam active: Boolean?,
        pageable: Pageable
    ) = service.getList(search, status, active, pageable)

    @PreAuthorize("hasAnyAuthority('DEVELOPER', 'ADMIN','ORG_ADMIN')")
    @GetMapping("statistics")
    fun getStatistics() = service.getOrganizationStatistics()

    @PreAuthorize("hasAnyAuthority('DEVELOPER', 'ADMIN')")
    @PutMapping("active/{id}")
    fun changeOrgActive(@PathVariable id: Long) = service.changeOrgActive(id)
}