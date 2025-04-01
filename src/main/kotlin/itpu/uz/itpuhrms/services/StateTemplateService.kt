package itpu.uz.itpuhrms.services

import itpu.uz.itpuhrms.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


interface StateTemplateService {
    fun create(request: StateTemplateRequest): StateTemplateResponse
    fun getOneById(id: Long): StateTemplateResponse
    fun edit(id: Long, request: StateTemplateRequest): StateTemplateResponse
    fun getAll(pageable: Pageable): Page<StateTemplateResponse>
    fun delete(id: Long)
    fun saveDefaultStates(board: Board)
}

@Service
class StateTemplateServiceImpl(
    private val stateRepository: StateRepository,
    private val repository: StateTemplateRepository
) : StateTemplateService {

    @Transactional
    override fun create(request: StateTemplateRequest): StateTemplateResponse {
        return StateTemplateResponse.toDto(
            repository.save(
                StateTemplate(
                    request.name,
                    request.status
                )
            )
        )
    }

    override fun getOneById(id: Long) = repository.findByIdAndDeletedFalse(id)?.let {
        StateTemplateResponse.toDto(it)
    } ?: throw StateTemplateNotFoundException()

    @Transactional
    override fun edit(id: Long, request: StateTemplateRequest) =
        repository.findByIdAndDeletedFalse(id)?.let {
            it.apply {
                status = request.status
                name = request.name
            }
            StateTemplateResponse.toDto(repository.save(it))
        } ?: throw StateTemplateNotFoundException()

    override fun getAll(pageable: Pageable) = repository.findAllNotDeleted(pageable)
        .map { StateTemplateResponse.toDto(it) }

    @Transactional
    override fun delete(id: Long) {
        repository.findByIdAndDeletedFalse(id)?.let {
            repository.deleteById(it.id!!)
        } ?: throw StateTemplateNotFoundException()
    }

    @Transactional
    override fun saveDefaultStates(board: Board) {
        var templates = repository.findDefaultTemplatesByStatus(Status.ACTIVE).sortedBy { it.id }
//        if (templates.size < 2) {
//            val existingNames = templates.map { it.name }.toSet()
//            val neededTemplates = mutableListOf<StateTemplate>()
//            if ("OPEN" !in existingNames) {
//                neededTemplates.add(StateTemplate(DefaultTaskState.OPEN.name, Status.ACTIVE))
//            }
//            if ("CLOSED" !in existingNames) {
//                neededTemplates.add(StateTemplate(DefaultTaskState.CLOSED.name, Status.ACTIVE))
//            }
//            repository.saveAll(neededTemplates)
//            templates = repository.findDefaultTemplatesByStatus(Status.ACTIVE).sortedBy { it.id }
//        }
//        //opened va closed template larni olib 2 ta immutable true bugan default state yaratyapti
        val states = templates.mapIndexed { index, template ->
            State(
                template.name,
                (index + 1).toShort(),
                board,
                true
            )
        }
        stateRepository.saveAll(states)
    }
}