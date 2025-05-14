package itpu.uz.itpuhrms.services.userAbsenceTracker


import itpu.uz.itpuhrms.utils.BASE_API
import org.springframework.data.domain.Pageable
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("$BASE_API/user-absence-tracker")
class UserAbsenceTrackerController(
    private val service: UserAbsenceTrackerService
) {
    @PostMapping
    fun create(@RequestBody request: UserAbsenceTrackerRequest) = service.create(request)

    @PutMapping
    fun update(@RequestParam("trackerId") id: Long, @RequestBody request: UserAbsenceTrackerRequest) =
        service.update(id, request)

    @GetMapping
    fun getOne(@RequestParam("trackerId") id: Long) = service.getOne(id)

    @GetMapping("get-all")
    fun getAll(
        @RequestParam("userId") userId: Long?,
        @RequestParam("startDate") startDate: Long,
        @RequestParam("endDate") endDate: Long,
        @RequestParam("eventType") eventType: String?,
        pageable: Pageable
    ) = service.getAll(userId, startDate, endDate, eventType, pageable)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @GetMapping("by-table-date")
    fun getAllByTableDate(
        @RequestParam tableDateId: Long,
        pageable: Pageable
    ) = service.getAllByTableDate(tableDateId, pageable)

    @DeleteMapping
    fun delete(@RequestParam("trackerId") id: Long) = service.delete(id)
}