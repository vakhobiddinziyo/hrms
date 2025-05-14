package itpu.uz.itpuhrms

import com.fasterxml.jackson.annotation.JsonProperty
import itpu.uz.itpuhrms.ValidRequest.Companion.validTimeRequest
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*


@Document
@CompoundIndex(def = "{'hashId': 1, 'organizationId': 1}", unique = true)
class Visitor(
    val hashId: String,
    val organizationId: Long,
    val userId: Long,
    val person: UserInfoAddRequest,
    // this is for faceImage
    var imageHashId: String,
    @Indexed(expireAfterSeconds = 86400 * 30) val createdAt: Date = Date(),
    @org.springframework.data.annotation.Id var id: String? = null
)

@Document
class UnknownPerson(
    val organizationId: Long?,
    val tourniquetId: Long?,
    val tourniquetName: String?,
    // this is for faceImage
    var image: String?,
    @Indexed(expireAfterSeconds = 86400 * 30) val createdAt: Date = Date(),
    @org.springframework.data.annotation.Id var id: String? = null
)

@Document
@CompoundIndex(def = "{'userId': 1, 'organizationId': 1}")
class UserOrgSession(
    val userId: Long,
    val organizationId: Long,
    @Enumerated(EnumType.STRING) var role: Role,
    @Indexed(expireAfter = "5m") val createdDate: Date = Date(),
    @org.springframework.data.annotation.Id var id: String? = null
)

data class UserInfoAddRequest(
    val employeeNo: String,
    var name: String,
    @JsonProperty("Valid")
    var valid: ValidRequest,
    var userType: String,
    val gender: String,
) {
    companion object {
        fun userInfo(
            hashPinfl: String,
            credentials: UserCredentials,
            type: TourniquetEmployeeType
        ) = UserInfoAddRequest(
            hashPinfl,
            credentials.user.fullName,
            validTimeRequest(),
            type.toLower(),
            credentials.gender.toLower(),
        )
    }
}

data class ValidRequest(
    val beginTime: String,
    val enable: Boolean,
    val endTime: String,
) {
    companion object {
        fun validTimeRequest(): ValidRequest {
            val now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)
            val tenYearsLater = now.plusYears(10)
            return ValidRequest(now.toString(), true, tenYearsLater.toString())
        }
    }
}
