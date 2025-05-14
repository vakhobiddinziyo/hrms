package itpu.uz.itpuhrms.services.tourniquetEmployee


import itpu.uz.itpuhrms.EmployeeStatus
import itpu.uz.itpuhrms.services.tourniquet.TourniquetService
import itpu.uz.itpuhrms.utils.BASE_API
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("$BASE_API/tourniquet-employee")
class TourniquetEmployeeController(
    private val service: TourniquetEmployeeService,
) {

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @PutMapping
    fun changeEmployeeTourniquetData(@Valid @RequestBody request: EmployeeDataRequest) =
        service.changeEmployeeTourniquetData(request)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @PostMapping("synchronize")
    fun synchronizeEmployeeTourniquetData() = service.synchronizeEmployeeTourniquetData()

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @GetMapping("{id}")
    fun getOne(@PathVariable id: Long) = service.getOneById(id)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @GetMapping
    fun getTourniquetEmployees(
        @RequestParam employeeId: Long?,
        @RequestParam tourniquetId: Long?,
        @RequestParam status: EmployeeStatus?,
        pageable: Pageable
    ) = service.getTourniquetEmployees(employeeId, tourniquetId, status, pageable)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @DeleteMapping("{id}")
    fun deleteTourniquetEmployeeData(@PathVariable id: Long) = service.deleteEmployeeData(id)

}


@RestController
@RequestMapping("$BASE_API/hook")
class TourniquetEmployeeUpdatingController (
    private val service: TourniquetEmployeeService,
    private val tourniquetService: TourniquetService,
) {
    @GetMapping("{tourniquetId}")
    fun getOrganizationResult(
        @PathVariable tourniquetId: Long,
        @RequestHeader(name = "Authorization") authorization: String
    ) = service.getTourniquetEmployeesForUpdater(tourniquetId, authorization)

    @PostMapping("synchronize")
    fun synchronizeEmployeeTourniquetData(
        @RequestBody request: SynchronizationData,
        @RequestHeader("Authorization") authorization: String
    ) = service.synchronizeEmployeeTourniquetDataFromUpdater(request, authorization)


    @PutMapping
    fun updateEmployeeData(
        @RequestHeader("Authorization") authorization: String,
        @RequestBody request: List<TourniquetEmployeeUpdateRequest>
    ) = service.updateEmployeeDataFromUpdater(request, authorization)

    @GetMapping("/tourniquets")
    fun get(@RequestHeader("Authorization") authorization: String) = tourniquetService.get(authorization)
}


