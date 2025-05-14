package itpu.uz.itpuhrms.services.projectEmployee


import itpu.uz.itpuhrms.utils.BASE_API
import org.springframework.data.domain.Pageable
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("$BASE_API/project-employee")
class ProjectEmployeeController(
    private val service: ProjectEmployeeService
) {
    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @PostMapping
    fun create(@RequestBody request: ProjectEmployeeRequest) = service.create(request)

    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @GetMapping("{id}")
    fun getOneById(@PathVariable id: Long) = service.getOneById(id)

    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @GetMapping("me")
    fun getMe(@RequestParam projectId: Long) = service.getMe(projectId)

    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @GetMapping
    fun getAllByProjectId(
        @RequestParam projectId: Long,
        @RequestParam search: String?,
        @RequestParam status: String?,
        pageable: Pageable
    ) = service.getAll(projectId, search, status, pageable)

    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @PutMapping("{id}")
    fun edit(@PathVariable id: Long, @RequestBody request: ProjectEmployeeEditRequest) = service.edit(id, request)

    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @DeleteMapping("{id}")
    fun delete(@PathVariable id: Long) = service.delete(id)
}