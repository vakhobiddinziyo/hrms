package itpu.uz.itpuhrms.services.subscriber


import itpu.uz.itpuhrms.utils.BASE_API
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("$BASE_API/subscriber")
class SubscriberController(
    private val service: SubscriberService
) {
    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @GetMapping("me")
    fun getMe() = service.getMe()

    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @PutMapping
    fun update(@RequestBody request: SubscriberRequest) = service.update(request)
}
