package itpu.uz.itpuhrms.services.tourniquet

import itpu.uz.itpuhrms.*
import itpu.uz.itpuhrms.services.ExtraService
import itpu.uz.itpuhrms.services.tourniquetClient.TourniquetClientRepository
import itpu.uz.itpuhrms.services.validation.ValidationService
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

interface TourniquetService {
    fun create(request: TourniquetRequest): TourniquetResponse
    fun update(id: Long, request: TourniquetUpdateRequest): TourniquetResponse
    fun get(orgId: Long?, pageable: Pageable): Page<TourniquetResponse>
    fun get(authorization: String): List<TourniquetDto>
    fun getByOrganization(pageable: Pageable): Page<TourniquetResponse>
    fun getOneById(id: Long): TourniquetResponse
    fun delete(id: Long)
}


@Service
class TourniquetServiceImpl(
    private val repository: TourniquetRepository,
    private val aesEncryptionService: AESEncryptionService,
    private val validationService: ValidationService,
    private val clientRepository: TourniquetClientRepository,
    private val passwordEncoder: PasswordEncoder,
    private val extraService: ExtraService,
) : TourniquetService {
    override fun create(request: TourniquetRequest): TourniquetResponse {
        return request.run {
            val organization = extraService.getOrgFromCurrentUser()
            if (repository.existsByName(name)) throw TourniquetNameAlreadyExistsException()

            val savedTourniquet = repository.save(
                Tourniquet(
                    organization,
                    ip,
                    name,
                    username,
                    aesEncryptionService.encode(password),
                    type,
                    description
                )
            )
            TourniquetResponse.toResponse(savedTourniquet)
        }
    }

    override fun update(id: Long, request: TourniquetUpdateRequest): TourniquetResponse {
        return repository.findByIdAndDeletedFalse(id)?.let { tourniquet ->
            validationService.validateDifferentOrganizations(
                tourniquet.organization,
                extraService.getOrgFromCurrentUser()
            )
            if (repository.existsByName(request.name) && request.name != tourniquet.name)
                throw TourniquetNameAlreadyExistsException()

            tourniquet.apply {
                this.ip = request.ip
                this.name = request.name
                this.username = request.username
                request.password?.let { this.password = aesEncryptionService.encode(it) }
                this.description = request.description
            }

            TourniquetResponse.toResponse(repository.save(tourniquet))
        } ?: throw TourniquetNotFoundException()
    }

    override fun get(orgId: Long?, pageable: Pageable) =
        repository.findTourniquetsByDeletedFalseAndFilter(orgId, pageable)
            .map {
                TourniquetResponse.toResponse(it)
            }

    override fun get(authorization: String): List<TourniquetDto> {
        val credentials = authorization.extractPasswordAndUsername()
        val username = credentials[0]
        val password = credentials[1]
        clientRepository.findByUsernameAndDeletedFalse(username)?.let { client ->
            if (!passwordEncoder.matches(password, client.password)) throw UsernameOrPasswordIncorrectException()

            return repository.findAllByOrganizationIdAndDeletedFalse(client.organization.id!!)
                .map {
                    TourniquetDto.toDto(it, aesEncryptionService.decode(it.password))
                }
        } ?: throw UsernameOrPasswordIncorrectException()
    }

    override fun getByOrganization(pageable: Pageable): Page<TourniquetResponse> {
        val organization = extraService.getOrgFromCurrentUser()
        return repository.findAllByOrganizationIdAndDeletedFalse(organization.id!!, pageable)
            .map { TourniquetResponse.toResponse(it) }
    }

    override fun getOneById(id: Long): TourniquetResponse {
        return repository.findByIdAndDeletedFalse(id)?.let {
            TourniquetResponse.toResponse(it)
        } ?: throw TourniquetNotFoundException()
    }

    override fun delete(id: Long) {
        repository.findByIdAndDeletedFalse(id)?.let {
            validationService.validateDifferentOrganizations(
                it.organization,
                extraService.getOrgFromCurrentUser()
            )
            repository.trash(id)
        } ?: throw TourniquetNotFoundException()
    }

}

interface AESEncryptionService {
    fun encode(tourniquetPassword: String): String
    fun decode(tourniquetPassword: String): String
}

@Service
class AESEncryptionServiceImpl(
    @Value("\${encrypt.aes.secret}")
    private val secret: String
) : AESEncryptionService {

    private lateinit var secretKey: SecretKeySpec
    private lateinit var key: ByteArray
    private val algorithm = "AES"

    private fun prepareSecreteKey(myKey: String) {
        key = myKey.toByteArray(StandardCharsets.UTF_8)
        val sha: MessageDigest = MessageDigest.getInstance("SHA-1")
        key = sha.digest(key)
        key = key.copyOf(16)
        secretKey = SecretKeySpec(key, algorithm)
    }

    override fun encode(tourniquetPassword: String): String {
        prepareSecreteKey(secret)
        val cipher = Cipher.getInstance(algorithm)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        return Base64.getEncoder()
            .encodeToString(cipher.doFinal(tourniquetPassword.toByteArray(StandardCharsets.UTF_8)))
    }

    override fun decode(tourniquetPassword: String): String {
        prepareSecreteKey(secret)
        val cipher = Cipher.getInstance(algorithm)
        cipher.init(Cipher.DECRYPT_MODE, secretKey)
        return String(cipher.doFinal(Base64.getDecoder().decode(tourniquetPassword)))
    }
}