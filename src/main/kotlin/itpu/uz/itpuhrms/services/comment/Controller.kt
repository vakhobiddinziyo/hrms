package itpu.uz.itpuhrms.services.comment


import itpu.uz.itpuhrms.utils.BASE_API
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("$BASE_API/comment")
class CommentController(
    private val service: CommentService
) {
    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @PostMapping
    fun create(@RequestBody @Valid request: CommentRequest) = service.create(request)

    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @GetMapping("{id}")
    fun getOneById(@PathVariable id: Long) = service.getOneById(id)

    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @GetMapping
    fun getAll(@RequestParam taskId: Long, pageable: Pageable) = service.getAll(taskId, pageable)

    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @PutMapping("{id}")
    fun edit(@PathVariable id: Long, @RequestBody @Valid request: CommentRequest) = service.edit(id, request)

    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @DeleteMapping("{id}")
    fun delete(@PathVariable id: Long) = service.delete(id)
}