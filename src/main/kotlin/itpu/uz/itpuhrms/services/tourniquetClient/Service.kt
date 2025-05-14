package itpu.uz.itpuhrms.services.tourniquetClient

import itpu.uz.itpuhrms.*
import itpu.uz.itpuhrms.services.ExtraService
import itpu.uz.itpuhrms.services.validation.ValidationService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service


interface TourniquetClientService {
    fun create(request: ClientRequest): ClientResponse
    fun update(id: Long, request: ClientUpdateRequest): ClientResponse
    fun getOne(id: Long): ClientResponse
    fun getByOrganization(search: String?, pageable: Pageable): Page<ClientResponse>
    fun delete(id: Long)
}

@Service
class TourniquetClientServiceImpl(
    private val repository: TourniquetClientRepository,
    private val validationService: ValidationService,
    private val passwordEncoder: PasswordEncoder,
    private val extraService: ExtraService
) : TourniquetClientService {

    override fun create(request: ClientRequest): ClientResponse {
        return request.run {
            val organization = extraService.getOrgFromCurrentUser()
            if (repository.existsByUsername(username)) throw TourniquetClientUsernameAlreadyExistException()
            val client = repository.save(
                TourniquetClient(
                    organization, username, passwordEncoder.encode(password)
                )
            )
            ClientResponse.toResponse(client)
        }
    }

    override fun update(id: Long, request: ClientUpdateRequest): ClientResponse {
        return repository.findByIdAndDeletedFalse(id)?.let { client ->
            validationService.validateDifferentOrganizations(
                extraService.getOrgFromCurrentUser(),
                client.organization
            )
            val exist = repository.existsByUsername(request.username)
            if (exist && request.username != client.username)
                throw TourniquetClientUsernameAlreadyExistException()
            client.username = request.username
            request.password?.let {
                client.password = passwordEncoder.encode(request.password)
            }
            ClientResponse.toResponse(repository.save(client))
        } ?: throw TourniquetClientNotFoundException()
    }

    override fun getOne(id: Long) =
        repository.findByIdAndDeletedFalse(id)?.let {
            ClientResponse.toResponse(it)
        } ?: throw TourniquetClientNotFoundException()

    override fun getByOrganization(search: String?, pageable: Pageable): Page<ClientResponse> {
        val organization = extraService.getOrgFromCurrentUser()
        return repository.findOrganizationTourniquetClients(
            search, organization.id!!, pageable
        ).map { ClientResponse.toResponse(it) }
    }

    override fun delete(id: Long) {
        val organization = extraService.getOrgFromCurrentUser()
        repository.findByIdAndDeletedFalse(id)?.let {
            validationService.validateDifferentOrganizations(
                organization,
                it.organization
            )
            repository.trash(id)
        } ?: throw TourniquetClientNotFoundException()
    }
}