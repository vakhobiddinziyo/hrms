package itpu.uz.itpuhrms.services.userTourniquetResult


import itpu.uz.itpuhrms.utils.BASE_API
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("$BASE_API/user-tourniquet-result")
class UserTourniquetResultController(
    private val service: UserTourniquetResultService,
) {
    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @GetMapping
    fun getOrganizationResult(
        @RequestParam startDate: Long,
        @RequestParam endDate: Long
    ) = service.getOrganizationResult(startDate, endDate)
}
