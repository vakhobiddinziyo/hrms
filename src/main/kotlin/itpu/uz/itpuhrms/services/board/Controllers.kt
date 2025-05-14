package itpu.uz.itpuhrms.services.board


import itpu.uz.itpuhrms.BoardStatus
import itpu.uz.itpuhrms.utils.BASE_API
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("$BASE_API/board")
class BoardController(
    private val service: BoardService
) {

    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @PostMapping
    fun create(@Valid @RequestBody request: BoardRequest) = service.create(request)

    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @GetMapping("{id}")
    fun getOneById(@PathVariable id: Long) = service.getOneById(id)

    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @GetMapping
    fun getAll(
        @RequestParam projectId: Long,
        @RequestParam search: String?,
        @RequestParam status: BoardStatus?,
        pageable: Pageable
    ) = service.getAll(projectId, search, status, pageable)

    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @PutMapping("{id}")
    fun edit(@PathVariable id: Long, @Valid @RequestBody request: BoardUpdateRequest) = service.edit(id, request)

    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @DeleteMapping("{id}")
    fun delete(@PathVariable id: Long) = service.delete(id)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN')")
    @GetMapping("get-all")
    fun getAllForOrgAdmin(
        @RequestParam projectId: Long,
        @RequestParam search: String?,
        @RequestParam status: BoardStatus?,
        pageable: Pageable
    ) = service.getAllForOrgAdmin(projectId, search, status, pageable)
}