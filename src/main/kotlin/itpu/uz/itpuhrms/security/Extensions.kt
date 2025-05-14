package itpu.uz.itpuhrms.security

import itpu.uz.itpuhrms.Role
import org.springframework.security.core.context.SecurityContextHolder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

fun getUserName(): String? {
    val authentication = SecurityContextHolder.getContext().authentication
    if (!(authentication == null || !authentication.isAuthenticated || "anonymousUser" == authentication.principal)) {
        logger.info { authentication.details }
        return authentication.principal as String
    }
    return null
}

fun getUserId(): Long? {
    val authentication = SecurityContextHolder.getContext().authentication
    if (!(authentication == null || !authentication.isAuthenticated || "anonymousUser" == authentication.principal)) {
        val userDetails = authentication.details as UserAuthenticationDetails
        return userDetails.userId
    }
    return null
}

fun getRole(): Role? {
    val authentication = SecurityContextHolder.getContext().authentication
    if (!(authentication == null || !authentication.isAuthenticated || "anonymousUser" == authentication.principal)) {
        val userDetails = authentication.details as UserAuthenticationDetails
        return userDetails.role
    }
    return null
}


fun String.hashPinfl(): String {
    val md = MessageDigest.getInstance("MD5")
    val hash = md.digest(this.toByteArray(StandardCharsets.UTF_8))
    val hexString = StringBuilder()
    for (b in hash) {
        val hex = Integer.toHexString(0xff and b.toInt())
        if (hex.length == 1) hexString.append('0')
        hexString.append(hex)
    }
    return hexString.toString()
}