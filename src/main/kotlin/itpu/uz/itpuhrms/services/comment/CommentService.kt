package itpu.uz.itpuhrms.services.comment

import itpu.uz.itpuhrms.*
import itpu.uz.itpuhrms.base.BaseMessage
import itpu.uz.itpuhrms.CommentNotFoundException
import itpu.uz.itpuhrms.FileNotFoundException
import itpu.uz.itpuhrms.TaskActionHistoryNotFoundException
import itpu.uz.itpuhrms.TaskNotFoundException
import itpu.uz.itpuhrms.services.ExtraService
import itpu.uz.itpuhrms.services.file.FileAssetRepository
import itpu.uz.itpuhrms.services.task.TaskRepository
import itpu.uz.itpuhrms.services.taskActionHistory.TaskActionHistoryRepository
import itpu.uz.itpuhrms.services.taskActionHistory.TaskActionHistoryRequest
import itpu.uz.itpuhrms.services.taskActionHistory.TaskActionHistoryResponse
import itpu.uz.itpuhrms.services.taskActionHistory.TaskActionHistoryService
import itpu.uz.itpuhrms.services.validation.ValidationService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


interface CommentService {
    fun create(request: CommentRequest): TaskActionHistoryResponse
    fun getOneById(id: Long): CommentResponse
    fun getAll(taskId: Long, pageable: Pageable): Page<TaskActionHistoryResponse>
    fun edit(id: Long, request: CommentRequest): CommentResponse
    fun delete(id: Long): BaseMessage
}

@Service
class CommentServiceImpl(
    private val repository: CommentRepository,
    private val fileAssetRepository: FileAssetRepository,
    private val taskRepository: TaskRepository,
    private val extraService: ExtraService,
    private val validationService: ValidationService,
    private val actionHistoryService: TaskActionHistoryService,
    private val taskActionHistoryRepository: TaskActionHistoryRepository,
) : CommentService {

    @Transactional
    override fun create(request: CommentRequest): TaskActionHistoryResponse {
        val employee = extraService.getEmployeeFromCurrentUser()
        val task = taskRepository.findByIdAndDeletedFalse(request.taskId) ?: throw TaskNotFoundException()
        validationService.validateProjectEmployee(
            task.board.project,
            employee
        )
        val files = files(request)
        val comment = repository.save(Comment(task, request.text, employee.user!!, files))
        actionHistoryService.create(
            TaskActionHistoryRequest(
                task.id!!,
                employee.user!!.id!!,
                TaskAction.ADD_COMMENT,
                commentId = comment.id!!
            )
        )
        val action = taskActionHistoryRepository.findByCommentIdAndDeletedFalse(comment.id!!)
            ?: throw TaskActionHistoryNotFoundException()
        return TaskActionHistoryResponse.toDto(action)
    }

    override fun getOneById(id: Long): CommentResponse {
        return repository.findByIdAndDeletedFalse(id)?.let {
            validationService.validateProjectEmployee(
                it.task.board.project,
                extraService.getEmployeeFromCurrentUser()
            )
            CommentResponse.toDto(it)
        } ?: throw CommentNotFoundException()
    }

    override fun getAll(taskId: Long, pageable: Pageable): Page<TaskActionHistoryResponse> {
        val task = taskRepository.findByIdAndDeletedFalse(taskId) ?: throw TaskNotFoundException()
        validationService.validateProjectEmployee(
            task.board.project,
            extraService.getEmployeeFromCurrentUser()
        )
        return taskActionHistoryRepository.findAllCommentsActionHistoryByTaskId(taskId, pageable)
            .map { TaskActionHistoryResponse.toDto(it) }
    }

    @Transactional
    override fun edit(id: Long, request: CommentRequest): CommentResponse {
        val employee = extraService.getEmployeeFromCurrentUser()
        val comment = repository.findByIdAndOwnerIdAndDeletedFalse(id, employee.user!!.id!!)
            ?: throw CommentNotFoundException()
        val files = files(request)
        return CommentResponse.toDto(
            repository.save(
                comment.apply {
                    this.text = request.text
                    request.files?.let { this.files = files }
                }
            )
        )
    }

    @Transactional
    override fun delete(id: Long): BaseMessage {
        val employee = extraService.getEmployeeFromCurrentUser()
        repository.findByIdAndOwnerIdAndDeletedFalse(id, employee.user!!.id!!)
            ?.let {
                repository.trash(id)
            } ?: throw CommentNotFoundException()
        return BaseMessage.OK
    }

    private fun files(request: CommentRequest): MutableList<FileAsset>? {
        return request.files?.map {
            fileAssetRepository.findByHashIdAndDeletedFalse(it) ?: throw FileNotFoundException()
        }?.toMutableList()
    }
}