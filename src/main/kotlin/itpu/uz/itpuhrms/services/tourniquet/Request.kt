package itpu.uz.itpuhrms.services.tourniquet

import itpu.uz.itpuhrms.TourniquetType
import itpu.uz.itpuhrms.config.ValidIP
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size


data class TourniquetRequest(
    @field:Size(min = 1, max = 15, message = "ip length should be between 1 and 15")
    @field:NotBlank
    @field:ValidIP("Not valid ip address")
    val ip: String,
    @field:Size(min = 1, max = 255, message = "name length should be between 1 and 255")
    @field:NotBlank
    val name: String,
    @field:Size(min = 1, max = 255, message = "username length should be between 1 and 255")
    @field:NotBlank
    val username: String,
    @field:Size(min = 1, max = 255, message = "password length should be between 1 and 255")
    @field:NotBlank
    val password: String,
    val type: TourniquetType,
    @field:Size(min = 1, max = 255, message = "description length should be between 1 and 255")
    val description: String?
)



data class TourniquetUpdateRequest(
    @field:Size(min = 1, max = 15, message = "ip length should be between 1 and 15")
    @field:NotBlank
    @field:ValidIP("Not valid ip address")
    val ip: String,
    @field:Size(min = 1, max = 255, message = "name length should be between 1 and 255")
    @field:NotBlank
    val name: String,
    @field:Size(min = 1, max = 255, message = "username length should be between 1 and 255")
    @field:NotBlank
    val username: String,
    @field:Size(min = 1, max = 255, message = "password length should be between 1 and 255")
    @field:NotBlank
    val password: String? = null,
    @field:Size(min = 1, max = 255, message = "description length should be between 1 and 255")
    val description: String?
)
