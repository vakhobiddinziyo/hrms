package itpu.uz.itpuhrms.services.tourniquet


import itpu.uz.itpuhrms.utils.BASE_API
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("$BASE_API/tourniquet")
class TourniquetController(
    private val service: TourniquetService
) {
    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @PostMapping
    fun create(@Valid @RequestBody request: TourniquetRequest) = service.create(request)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @PutMapping("{id}")
    fun update(@PathVariable id: Long, @Valid @RequestBody request: TourniquetUpdateRequest) =
        service.update(id, request)

    @PreAuthorize("hasAnyAuthority('DEVELOPER', 'ADMIN')")
    @GetMapping
    fun getByOrganization(pageable: Pageable, @RequestParam orgId: Long?) =
        service.get(orgId, pageable)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @GetMapping("organization")
    fun getByOrganization(pageable: Pageable) = service.getByOrganization(pageable)

    @PreAuthorize("hasAnyAuthority('DEVELOPER', 'ADMIN') or (hasAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin)")
    @GetMapping("{id}")
    fun getById(@PathVariable id: Long) = service.getOneById(id)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @DeleteMapping("{id}")
    fun delete(@PathVariable id: Long) = service.delete(id)
}