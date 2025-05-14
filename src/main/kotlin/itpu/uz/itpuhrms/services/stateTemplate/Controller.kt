package itpu.uz.itpuhrms.services.stateTemplate


import itpu.uz.itpuhrms.utils.BASE_API
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("$BASE_API/state-template")
class StateTemplateController(
    private val service: StateTemplateService
) {
    @PreAuthorize("hasAnyAuthority('DEVELOPER', 'ADMIN')")
    @PostMapping
    fun create(@Valid @RequestBody request: StateTemplateRequest) = service.create(request)


    @GetMapping("{id}")
    fun getOneById(@PathVariable id: Long) = service.getOneById(id)


    @GetMapping
    fun getAll(pageable: Pageable) = service.getAll(pageable)

    @PreAuthorize("hasAnyAuthority('DEVELOPER', 'ADMIN')")
    @PutMapping("{id}")
    fun edit(@PathVariable id: Long, @RequestBody request: StateTemplateRequest) = service.edit(id, request)

    @PreAuthorize("hasAnyAuthority('DEVELOPER', 'ADMIN')")
    @DeleteMapping("{id}")
    fun delete(@PathVariable id: Long) = service.delete(id)
}