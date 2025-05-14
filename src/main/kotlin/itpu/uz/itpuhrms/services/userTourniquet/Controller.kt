package itpu.uz.itpuhrms.services.userTourniquet


import itpu.uz.itpuhrms.utils.BASE_API
import org.springframework.data.domain.Pageable
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("$BASE_API/user-tourniquet")
class UserTourniquetController(
    private val service: UserTourniquetService
) {

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @GetMapping
    fun get(
        @RequestParam startDate: Long,
        @RequestParam endDate: Long,
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false) userId: Long?,
        pageable: Pageable
    ) = service.getOrganizationUserTourniquets(userId, startDate, endDate, search, pageable)

    @GetMapping("{userId}")
    fun getUserEvents(
        @RequestParam startDate: Long,
        @RequestParam endDate: Long,
        @PathVariable userId: Long
    ) = service.getUserEvents(userId, startDate, endDate)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @GetMapping("working-minutes")
    fun getWorkingMinutes(
        @RequestParam startDate: Long,
        @RequestParam endDate: Long,
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false) departmentId: Long?,
        @RequestParam active: Boolean = true,
        pageable: Pageable
    ) = service.getWorkingMinutes(startDate, endDate, search, departmentId, active, pageable)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @GetMapping("working-hours-excel")
    fun getWorkingMinutesExcel(
        @RequestParam startDate: Long,
        @RequestParam endDate: Long,
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false) departmentId: Long?
    ) = service.getWorkingMinutesExcel(startDate, endDate, search, departmentId)

    @GetMapping("user-working-minutes")
    fun getUserWorkingMinutes(
        @RequestParam userId: Long,
        @RequestParam startDate: Long,
        @RequestParam endDate: Long
    ) = service.getUserWorkingMinutes(userId, startDate, endDate)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @GetMapping("last-absent-events")
    fun getLastAbsentEvents(
        @RequestParam startDate: Long,
        @RequestParam endDate: Long,
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false) departmentId: Long?,
        pageable: Pageable
    ) = service.getLastAbsentEvents(startDate, endDate, search, departmentId, pageable)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @PostMapping("synchronize-events")
    fun synchronizeEvents(@RequestParam tableDateId: Long) = service.synchronizeEvents(tableDateId)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @GetMapping("attendance-excel")
    fun dailyAttendance(@RequestParam date: Long) = service.dailyAttendance(date)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @GetMapping("unknown-people")
    fun getUnknownPeople(
        @RequestParam orgId: Long,
        pageable: Pageable
    ) = service.getUnknownPeople(orgId, pageable)
}