package itpu.uz.itpuhrms.services.userOrgStore


import itpu.uz.itpuhrms.utils.BASE_API
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("$BASE_API/user-organization")
class UserOrgStoreController(
    private val service: UserOrgStoreService,
) {
    @PreAuthorize("hasAnyAuthority('DEVELOPER')")
    @PostMapping
    fun create(@RequestBody @Valid request: UserOrgStoreRequest) = service.createUserOrgStore(request)


    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @PutMapping("role")
    fun updateUserOrgStoreRole(@RequestParam orgId: Long) = service.updateUserOrgStoreRole(orgId)

    @GetMapping
    fun getUserOrganizations(pageable: Pageable) =
        service.getUserOrganizations(pageable)
}
