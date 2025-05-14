package itpu.uz.itpuhrms.services.task

import itpu.uz.itpuhrms.*
import itpu.uz.itpuhrms.utils.BASE_API
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("$BASE_API/task")
class TaskController(
    private val service: TaskService
) {

    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @PostMapping
    fun create(@Valid @RequestBody request: TaskCreateRequest) = service.create(request)

    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @GetMapping("{id}")
    fun getOneById(@PathVariable id: Long) = service.getOneById(id)

    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @GetMapping
    fun getAll(
        @RequestParam parentId: Long?,
        @RequestParam boardId: Long,
        pageable: Pageable
    ) = service.getAll(parentId, boardId, pageable)

    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @GetMapping("/type")
    fun getAllTaskByType() = service.getTasksByType()



    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @PutMapping("{id}")
    fun edit(@PathVariable id: Long, @Valid @RequestBody request: TaskUpdateRequest) = service.edit(id, request)

    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @PutMapping("change-state/{id}")
    fun changeState(
        @PathVariable id: Long,
        @RequestParam stateId: Long,
        @RequestParam order: Short
    ) = service.changeState(id, stateId, order)

    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @PutMapping("move/{taskId}")
    fun moveTaskToBoard(
        @PathVariable taskId: Long,
        @RequestParam newBoardId: Long
    ) = service.moveTaskToBoard(taskId, newBoardId)

    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @DeleteMapping
    fun delete(@RequestBody request: TaskDeleteRequest) = service.delete(request)

    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @GetMapping("search-by-id")
    fun searchById(
        @RequestParam taskId: Long?,
        @RequestParam boardId: Long,
        @RequestParam startDate: Long?,
        @RequestParam endDate: Long?,
        @RequestParam priority: TaskPriority?,
        pageable: Pageable
    )
            = service.searchById(taskId, boardId, startDate, endDate,priority, pageable)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN')")
    @GetMapping("get-all")
    fun getAllForOrgAdmin(
        @RequestParam boardId: Long,
        @RequestParam stateName: String?,
        @RequestParam priority: TaskPriority?,
        pageable: Pageable
    ) = service.getAllForOrgAdmin(boardId, stateName, priority, pageable)

}