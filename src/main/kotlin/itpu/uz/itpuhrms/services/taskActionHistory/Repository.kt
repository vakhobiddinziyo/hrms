package itpu.uz.itpuhrms.services.taskActionHistory

import itpu.uz.itpuhrms.TaskActionHistory
import itpu.uz.itpuhrms.base.BaseRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface TaskActionHistoryRepository : BaseRepository<TaskActionHistory> {
    fun findAllByTaskIdAndDeletedFalseOrderByIdDesc(taskId: Long, pageable: Pageable): Page<TaskActionHistory>

    @Query(
        """
        select h
        from TaskActionHistory h
        where h.action = 'ADD_COMMENT'
          and h.comment.deleted = false
          and h.task.deleted = false
          and h.task.id = :taskId
        order by h.id desc 
    """
    )
    fun findAllCommentsActionHistoryByTaskId(taskId: Long, pageable: Pageable): Page<TaskActionHistory>
    fun findByCommentIdAndDeletedFalse(commentId: Long): TaskActionHistory?
    fun findTopByTaskIdAndDeletedFalseOrderById(taskId: Long): TaskActionHistory?
}
