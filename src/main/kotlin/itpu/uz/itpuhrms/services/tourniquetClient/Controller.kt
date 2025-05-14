package itpu.uz.itpuhrms.services.tourniquetClient


import itpu.uz.itpuhrms.utils.BASE_API
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("$BASE_API/tourniquet-client")
class TourniquetClientController(
    private val service: TourniquetClientService,
) {
    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @PostMapping
    fun create(@Valid @RequestBody request: ClientRequest) = service.create(request)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @PutMapping("{id}")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody request: ClientUpdateRequest
    ) = service.update(id, request)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @GetMapping("{id}")
    fun getOne(@PathVariable id: Long) = service.getOne(id)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @GetMapping
    fun get(@RequestParam search: String?, pageable: Pageable) = service.getByOrganization(search, pageable)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @DeleteMapping("{id}")
    fun delete(@PathVariable id: Long) = service.delete(id)
}