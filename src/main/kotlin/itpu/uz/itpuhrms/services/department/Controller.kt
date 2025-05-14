package itpu.uz.itpuhrms.services.department


import itpu.uz.itpuhrms.DepartmentType
import itpu.uz.itpuhrms.utils.BASE_API
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("$BASE_API/department")
class DepartmentController(
    private val service: DepartmentService
) {
    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @PostMapping
    fun create(@Valid @RequestBody request: DepartmentRequest) = service.create(request)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @PutMapping("{id}")
    fun update(@PathVariable id: Long, @Valid @RequestBody request: DepartmentRequest) =
        service.update(id, request)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @DeleteMapping("{id}")
    fun delete(@PathVariable id: Long) = service.delete(id)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @GetMapping("{id}")
    fun getById(@PathVariable id: Long) = service.getById(id)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @GetMapping
    fun getDepartmentPage(pageable: Pageable) = service.getPage(pageable)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @GetMapping("list")
    fun getDepartmentList(@RequestParam parentId: Long?) = service.getListForStructure(parentId)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @GetMapping("list-parent")
    fun getDepartmentListByParent(@RequestParam parentId: Long?) = service.getListForStructureByParent(parentId)

    @PreAuthorize("hasAnyAuthority('DEVELOPER', 'ADMIN')")
    @GetMapping("admin")
    fun departmentList(
        @RequestParam type: DepartmentType,
        @RequestParam orgId: Long
    ) = service.getListByType(type, orgId)
}