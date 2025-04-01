package itpu.uz.itpuhrms.security

import itpu.uz.itpuhrms.Role

data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val expireAt: Long
)

data class LoginRequest(
    val username: String,
    val password: String
)

data class RefreshRequest(
    val refreshToken: String
)

data class JwtClaim(
    val username: String,
    val userId: Long
)

data class UserAuthenticationDetails(
    val role: Role,
    val userId: Long?
)