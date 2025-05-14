package itpu.uz.itpuhrms.services.tableDate


import itpu.uz.itpuhrms.utils.BASE_API
import org.springframework.data.domain.Pageable
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("$BASE_API/table-date")
class TableDateController(
    private val service: TableDateService
) {
    @PostMapping
    fun create(@RequestBody request: TableDateRequest) = service.create(request)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @GetMapping("{id}")
    fun getOneById(@PathVariable id: Long) = service.getOneById(id)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @GetMapping
    fun getAll(
        @RequestParam startDate: Long,
        @RequestParam endDate: Long,
        pageable: Pageable
    ) = service.getAll(startDate, endDate, pageable)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @PutMapping("{id}")
    fun edit(@PathVariable id: Long, @RequestBody request: TableDateUpdateRequest) = service.edit(id, request)

    @PreAuthorize("hasAnyAuthority('DEVELOPER', 'ADMIN')")
    @GetMapping("refresh")
    fun refreshCalendar() = service.refresh()

}
