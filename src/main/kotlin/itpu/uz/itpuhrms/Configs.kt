package itpu.uz.itpuhrms

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration


@Configuration
@ConfigurationProperties(prefix = "sms.valid")
class PhoneNumberConfiguration {
    var length: Int = 9
    lateinit var prefix: String
    lateinit var codes: Set<String>

    fun validatePhoneNumber(phoneNumber: String): String {
        var validPhoneNumber = phoneNumber.replace(Regex("[+\\-_,]"), "")
        if (!validPhoneNumber.matches(Regex("^[0-9]+$"))) throw InvalidPhoneNumberException()
        if (validPhoneNumber.length == length + prefix.length && validPhoneNumber.startsWith(prefix)) {
            validPhoneNumber = validPhoneNumber.substring(3)
        }
        if (validPhoneNumber.length != length) throw InvalidPhoneNumberException()
        val code = validPhoneNumber.substring(0, 2)
        if (!codes.contains(code)) throw InvalidPhoneNumberException()
        return validPhoneNumber
    }
}