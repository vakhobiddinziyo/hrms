package itpu.uz.itpuhrms.services.userOrgSession


import itpu.uz.itpuhrms.utils.BASE_API
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("$BASE_API/session")
class UserOrgSessionController(
    private val service: UserOrgSessionService
) {

    @GetMapping("{orgId}")
    fun getSession(@PathVariable orgId: Long) = service.getSession(orgId)

    @GetMapping("info")
    fun getOSessionInfo(@RequestHeader(name = "O-Session") id: String) = service.getSessionInfo(id)
}