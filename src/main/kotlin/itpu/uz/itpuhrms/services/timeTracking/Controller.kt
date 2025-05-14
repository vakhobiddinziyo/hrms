package itpu.uz.itpuhrms.services.timeTracking


import itpu.uz.itpuhrms.utils.BASE_API
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("$BASE_API/time-tracking")
class TimeTrackingController(
    private val service: TimeTrackingService
) {

    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @PostMapping
    fun create(@RequestBody request: TimeTrackingCreateRequest) = service.create(request)

    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @GetMapping("{id}")
    fun getOneById(@PathVariable id: Long) = service.getOneById(id)

    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @GetMapping
    fun getAll(@RequestParam taskId: Long) = service.getAll(taskId)


    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @PutMapping("{id}")
    fun edit(@PathVariable id: Long, @RequestBody request: TimeTrackingUpdateRequest) = service.edit(id, request)

    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @DeleteMapping("{id}")
    fun delete(@PathVariable id: Long) = service.delete(id)
}