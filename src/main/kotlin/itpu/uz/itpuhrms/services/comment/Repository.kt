package itpu.uz.itpuhrms.services.comment


import itpu.uz.itpuhrms.Comment
import itpu.uz.itpuhrms.base.BaseRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository


@Repository
interface CommentRepository : BaseRepository<Comment> {
    fun findAllByTaskIdAndDeletedFalseOrderByIdDesc(taskId: Long, pageable: Pageable): Page<Comment>
    fun findByIdAndOwnerIdAndDeletedFalse(id: Long, ownerId: Long): Comment?
}