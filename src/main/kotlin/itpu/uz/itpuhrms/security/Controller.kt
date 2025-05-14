package itpu.uz.itpuhrms.security

import itpu.uz.itpuhrms.utils.BASE_API
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController



@RestController
@RequestMapping("$BASE_API/auth")
class AuthController(
    private val authService: AuthService
) {
    @PostMapping("login")
    fun login(@RequestBody request: LoginRequest) =
        authService.authenticate(request)

    @PostMapping("refresh")
    fun refresh(@RequestBody request: RefreshRequest) = authService.refresh(request)
}
