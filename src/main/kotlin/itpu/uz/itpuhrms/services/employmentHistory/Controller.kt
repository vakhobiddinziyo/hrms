package itpu.uz.itpuhrms.services.employmentHistory


import itpu.uz.itpuhrms.utils.BASE_API
import org.springframework.data.domain.Pageable
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("$BASE_API/employment")
class EmploymentHistoryController(
    private val service: EmploymentHistoryService
) {
    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @PostMapping
    fun hireEmployee(@RequestBody request: EmploymentHistoryRequest) = service.hire(request)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @PutMapping("{employeeId}")
    fun dismissEmployee(@PathVariable employeeId: Long) = service.dismiss(employeeId)

    @GetMapping
    fun getEmploymentHistory(@RequestParam userId: Long, pageable: Pageable) = service.getUserHistory(userId, pageable)

    @GetMapping("{id}")
    fun getOne(@PathVariable id: Long) = service.getOne(id)
}