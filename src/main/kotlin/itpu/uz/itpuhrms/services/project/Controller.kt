package itpu.uz.itpuhrms.services.project


import itpu.uz.itpuhrms.ProjectStatus
import itpu.uz.itpuhrms.utils.BASE_API
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("$BASE_API/project")
class ProjectController(
    private val service: ProjectService
) {

    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @PostMapping
    fun createProject(@RequestBody @Valid request: ProjectCreateRequest) = service.createProject(request)

    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @PutMapping("{id}")
    fun updateProject(@PathVariable id: Long, @RequestBody @Valid request: ProjectUpdateRequest) =
        service.update(id, request)

    @GetMapping("{id}")
    fun getOneById(@PathVariable id: Long) = service.getProjectById(id)


    @PreAuthorize("hasAnyAuthority('ORG_ADMIN')")
    @GetMapping
    fun getProjects(@RequestParam departmentId: Long?, @RequestParam status: ProjectStatus?, pageable: Pageable) =
        service.getAllProjectForOrgAdmin(departmentId, status, pageable)

    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @GetMapping("me")
    fun getProjectsPageForCurrentUser(
        @RequestParam search: String?,
        @RequestParam status: ProjectStatus?,
        pageable: Pageable
    ) = service.getProjectsPageForCurrentUser(search, status, pageable)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @GetMapping("list")
    fun getProjectsList(@RequestParam departmentId: Long?) = service.getAllProject(departmentId)

    @DeleteMapping("{id}")
    fun deleteProject(@PathVariable id: Long) = service.delete(id)

}