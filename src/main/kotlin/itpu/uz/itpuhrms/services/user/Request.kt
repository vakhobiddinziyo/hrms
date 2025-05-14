package itpu.uz.itpuhrms.services.user

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include
import itpu.uz.itpuhrms.*
import itpu.uz.itpuhrms.config.CheckPinfl
import itpu.uz.itpuhrms.config.ValidEmail
import itpu.uz.itpuhrms.services.organization.OrgAdminResponse
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.*


data class UserCredentialsRequest(
    @CheckPinfl
    val pinfl: String,
    @field:Size(min = 1, max = 255, message = "fio length should be between 1 and 255")
    @field:NotBlank
    val fio: String,
    val cardGivenDate: Long,
    val cardExpireDate: Long,
    val cardSerialNumber: String,
    val gender: Gender,
    val birthday: Long,
) {
    fun toEntity(user: User) = UserCredentials(
        pinfl,
        fio,
        Date(cardGivenDate),
        Date(cardExpireDate),
        cardSerialNumber,
        gender,
        Date(birthday),
        user
    )
}


data class PinflRequest(
    @CheckPinfl val pinfl: String
)

data class UserAdminRequest(
    @field:Size(min = 1, max = 255, message = "fullName length should be between 1 and 255")
    @field:NotBlank
    val fullName: String,
    @field:Size(min = 1, max = 12, message = "phone number length should be between 1 and 12")
    @field:NotBlank
    @field:Pattern(regexp = "^[0-9]+\$", message = "phoneNumber should be only numbers")
    val phoneNumber: String,
    @field:Size(min = 1, max = 255, message = "username length should be between 1 and 255")
    @field:NotBlank
    val username: String,
    @field:Size(min = 1, max = 255, message = "password length should be between 1 and 255")
    @field:NotBlank
    val password: String,
    val status: Status,
    @field:Size(min = 1, max = 255, message = "mail length should be between 1 and 255")
    @field:NotBlank
    @field:ValidEmail(message = "must send valid mail")
    val mail: String,
    val role: Role,
    val orgStores: Set<OrgStoreRequest>,
    val avatarPhoto: String? = null,
    @field:Valid val credentials: UserCredentialsRequest? = null
)

data class UserAdminResponse(
    val id: Long,
    val fullName: String,
    val username: String,
    val role: Role,
    val status: Status,
    val phoneNumber: String,
    val mail: String,
    val avatarPhoto: String? = null,
    val organization: List<OrgAdminResponse>,
    @JsonInclude(Include.NON_NULL)
    val credentials: UserCredentialsResponse? = null
) {
    companion object {
        fun toDto(user: User, organization: List<OrgAdminResponse>, credentials: UserCredentials?) = UserAdminResponse(
            user.id!!,
            user.fullName,
            user.username,
            user.role,
            user.status,
            user.phoneNumber,
            user.mail,
            user.avatarPhoto?.hashId,
            organization,
            credentials?.let { UserCredentialsResponse.toDto(it) }
        )
    }
}

data class UserUpdateRequest(
    @field:Size(min = 1, max = 255, message = "fullName length should be between 1 and 255")
    val fullName: String?,
    @field:Size(min = 1, max = 255, message = "username length should be between 1 and 255")
    val username: String?,
    @field:Size(min = 1, max = 12, message = "phone number length should be between 1 and 12")
    @field:Pattern(regexp = "^[0-9]+\$", message = "phoneNumber should be only numbers")
    val phoneNumber: String?,
    @field:Size(min = 1, max = 255, message = "mail length should be between 1 and 255")
    @field:ValidEmail(message = "must send valid mail")
    val mail: String?,
    @field:Size(min = 1, max = 255, message = "password length should be between 1 and 255")
    val password: String? = null,
    val avatarPhoto: String? = null,
    @field:Valid val credentials: UserCredentialsRequest? = null
)

data class UserAdminUpdateRequest(
    @field:Size(min = 1, max = 255, message = "fullName length should be between 1 and 255")
    @field:NotBlank
    val fullName: String,
    @field:Size(min = 1, max = 255, message = "phoneNumber length should be between 1 and 255")
    @field:NotBlank
    val phoneNumber: String,
    @field:Size(min = 1, max = 255, message = "username length should be between 1 and 255")
    @field:NotBlank
    val username: String,
    @field:Size(min = 1, max = 255, message = "mail length should be between 1 and 255")
    @field:NotBlank
    @field:ValidEmail(message = "must send valid mail")
    val mail: String,
    val status: Status,
    val password: String? = null,
    val avatarPhoto: String? = null,
    val orgStores: Set<OrgStoreRequest>,
    val role: Role? = null,
    @field:Valid val credentials: UserCredentialsRequest? = null
)

data class UserRequest(
    @field:Size(min = 1, max = 255, message = "fullName length should be between 1 and 255")
    @field:NotBlank
    val fullName: String,
    @field:Size(min = 1, max = 12, message = "phone number length should be between 1 and 12")
    @field:NotBlank
    @field:Pattern(regexp = "^[0-9]+\$", message = "phoneNumber should be only numbers")
    val phoneNumber: String,
    @field:Size(min = 1, max = 255, message = "username length should be between 1 and 255")
    @field:NotBlank
    val username: String,
    @field:Size(min = 1, max = 255, message = "password length should be between 1 and 255")
    @field:NotBlank
    val password: String,
    val status: Status,
    @field:Size(min = 1, max = 255, message = "mail length should be between 1 and 255")
    @field:NotBlank
    @field:ValidEmail(message = "must send valid mail")
    val mail: String,
    val avatarPhoto: String? = null,
    @field:Valid val credentials: UserCredentialsRequest
) {
    fun toEntity(phoneNumber: String, passwordEncoder: PasswordEncoder, avatarPhoto: FileAsset?) = User(
        fullName,
        phoneNumber,
        username,
        passwordEncoder.encode(password),
        status,
        mail,
        Role.USER,
        avatarPhoto
    )
}

data class OrgStoreRequest(
    val orgId: Long,
    val granted: Boolean
)
