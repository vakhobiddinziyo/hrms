package itpu.uz.itpuhrms.services.tourniquetClient

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class ClientRequest(
    @field:Size(min = 1, max = 255, message = "username length should be between 1 and 255")
    @field:NotBlank
    val username: String,
    @field:Size(min = 1, max = 255, message = "password length should be between 1 and 255")
    @field:NotBlank
    val password: String,
)

data class ClientUpdateRequest(
    @field:Size(min = 1, max = 255, message = "username length should be between 1 and 255")
    @field:NotBlank
    val username: String,
    @field:Size(min = 1, max = 255, message = "password length should be between 1 and 255")
    val password: String? = null,
)
