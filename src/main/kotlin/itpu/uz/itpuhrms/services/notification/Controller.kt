package itpu.uz.itpuhrms.services.notification

import itpu.uz.itpuhrms.utils.BASE_API
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("$BASE_API/board-settings")
class BoardSettingsController(
    private val service: NotificationSettingsService
) {
    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @PostMapping
    fun create(@RequestBody request: BoardSettingsRequest) = service.create(request)

    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @PutMapping("{id}")
    fun update(@PathVariable id: Long, @RequestBody request: BoardSettingsUpdateRequest) = service.update(id, request)

    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @GetMapping("{boardId}")
    fun get(@PathVariable boardId: Long) = service.getOne(boardId)
}

@RestController
@RequestMapping("$BASE_API/telegram-bot")
class TelegramBotController(
    private val notificationService: TelegramNotificationService
) {

    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @PostMapping("hash")
    fun generateHash() = notificationService.getHash()
}

