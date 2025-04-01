package itpu.uz.itpuhrms.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import itpu.uz.itpuhrms.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.authentication.InsufficientAuthenticationException
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import java.util.*
import javax.crypto.SecretKey


interface AuthService {
    fun authenticate(request: LoginRequest): TokenResponse
    fun refresh(request: RefreshRequest): TokenResponse
}

@Service
class AuthServiceImpl(
    private val userDetailsService: CustomUserDetailsService,
    private val jwtService: JwtService,
    private val passwordEncoder: BCryptPasswordEncoder
) : AuthService {
    override fun authenticate(request: LoginRequest): TokenResponse {
        val user = userDetailsService.loadUserByUsername(request.username)
        if (!passwordEncoder.matches(request.password, user.password)) {
            throw org.springframework.security.authentication.BadCredentialsException("username or password is incorrect")
        }
        return jwtService.generateToken(user)
    }

    override fun refresh(request: RefreshRequest): TokenResponse {
        val claims = jwtService.getClaims(request.refreshToken)
            ?: throw InsufficientAuthenticationException("Not Valid Refresh token")

        val user = userDetailsService.loadUserByUsername(claims.username)
        return jwtService.generateToken(user)
    }


}

@Component
class CustomUserDetailsService(
    private val userRepository: UserRepository
) : UserDetailsService {
    override fun loadUserByUsername(username: String): CustomUserDetails {
        val user = userRepository.findByUsernameAndDeletedFalse(username) ?: throw BadCredentialsException()
        if (user.status == Status.DEACTIVATED) throw DeactivatedUserException()
        return CustomUserDetails(user.password, user.username, user.role, user.id!!)
    }
}

data class CustomUserDetails(
    val myPassword: String,
    val myUserName: String,
    val role: Role,
    val id: Long
) : UserDetails {

    override fun getAuthorities(): MutableCollection<out GrantedAuthority> {
        return listOf(SimpleGrantedAuthority(role.name)).toMutableList()
    }

    override fun getPassword(): String = myPassword

    override fun getUsername(): String = myUserName

    override fun isAccountNonExpired(): Boolean = true

    override fun isAccountNonLocked(): Boolean = true

    override fun isCredentialsNonExpired(): Boolean = true

    override fun isEnabled(): Boolean = true

}

@Component
class JwtService(
    @Value("\${application.security.jwt.secret-key}")
    private val secret: String,
    @Value("\${application.security.jwt.expiration}")
    private val expirationAccessToken: Long,
    @Value("\${application.security.jwt.refresh-token.expiration}")
    private val expirationRefreshToken: Long
) {
    private val issuer = "hrms-pro"
    fun generateToken(userDetails: CustomUserDetails): TokenResponse {
        val accessClaims: MutableMap<String, Any> = HashMap()
        val now = Date()
        val expiresIn = Date(now.time + expirationAccessToken)

        accessClaims[Constants.USERNAME] = userDetails.username
        accessClaims[Constants.USER_ID] = userDetails.id
        accessClaims[Constants.TOKEN_TYPE] = Constants.ACCESS_TOKEN

        val access = Jwts.builder()
            .id(UUID.randomUUID().toString())
            .claims(accessClaims)
            .issuer(issuer)
            .issuedAt(Date(System.currentTimeMillis()))
            .expiration(expiresIn)
            .signWith(signKey()).compact()
        val refreshClaims: MutableMap<String, Any> = HashMap()

        refreshClaims[Constants.TOKEN_TYPE] = Constants.REFRESH_TOKEN
        refreshClaims[Constants.USERNAME] = userDetails.username
        refreshClaims[Constants.USER_ID] = userDetails.id

        val refreshToken = Jwts.builder()
            .issuer(issuer)
            .claims(refreshClaims)
            .id(UUID.randomUUID().toString())
            .expiration(Date(now.time + expirationRefreshToken))
            .signWith(signKey()).compact()
        return TokenResponse(access, refreshToken, expiresIn.time)
    }

    private fun signKey(): SecretKey {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret))
    }

    fun getClaims(token: String): JwtClaim? {
        return try {
            val claims = extractAllClaims(token)
            val username = claims[Constants.USERNAME]?.toString()
                ?: throw IllegalArgumentException("Username is missing in the token claims")

            val userId = claims[Constants.USER_ID]?.toString()?.toLongOrNull()
                ?: throw IllegalArgumentException("UserId is missing or invalid in the token claims")

            JwtClaim(username, userId)
        } catch (e: Exception) {
            null
        }
    }

    private fun extractAllClaims(token: String): Claims {
        return Jwts
            .parser()
            .verifyWith(signKey())
            .build()
            .parseClaimsJws(token)
            .payload
    }
}


interface AuthorizationService {
    fun isGrantedOrgAdmin(): Boolean
    fun isEmployee(): Boolean
}

@Service
class AuthorizationServiceImpl(
    private val repository: UserOrgSessionRepository,
    private val userOrgStoreRepository: UserOrgStoreRepository,
    private val employeeRepository: EmployeeRepository
) : AuthorizationService {
    override fun isGrantedOrgAdmin(): Boolean {
        val sessionId = getOSession()
        val userOrgSession = repository.findByIdOrNull(sessionId)
            ?: throw UserSessionNotFoundException()
        if (userOrgSession.userId != userId())
            throw UserSessionNotFoundException()
        val userOrgStore =
            userOrgStoreRepository.findByUserIdAndOrganizationIdAndDeletedFalse(userId(), userOrgSession.organizationId)
                ?: throw OrganizationNotFoundException()
        return userOrgStore.granted
    }

    override fun isEmployee(): Boolean {
        val sessionId = getOSession()
        val userOrgSession = repository.findByIdOrNull(sessionId)
            ?: throw UserSessionNotFoundException()
        if (userOrgSession.userId != userId())
            throw UserSessionNotFoundException()
        val employeeExists = employeeRepository.existsByUserIdAndOrganizationIdAndPhStatusAndDeletedFalse(
            userId(), userOrgSession.organizationId, PositionHolderStatus.BUSY
        )
        return employeeExists
    }
}