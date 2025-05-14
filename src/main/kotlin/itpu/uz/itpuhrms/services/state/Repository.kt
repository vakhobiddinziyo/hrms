package itpu.uz.itpuhrms.services.state

import itpu.uz.itpuhrms.Board
import itpu.uz.itpuhrms.State
import itpu.uz.itpuhrms.StateValidation
import itpu.uz.itpuhrms.base.BaseRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.web.bind.annotation.RequestParam

@Repository
interface StateRepository : BaseRepository<State> {
    @Query(
        """
        select s from State s
        where (:boardId is null or s.board.id = :boardId)
        and s.deleted = false
        order by s.order asc
    """
    )
    fun findAllByBoardId(@RequestParam boardId: Long?, pageable: Pageable): Page<State>
    fun findAllByBoardIdAndDeletedFalseOrderByOrder(boardId: Long): MutableList<State>
    fun findTopByBoardIdAndDeletedFalseOrderByOrderDesc(boardId: Long): State?
    fun existsByBoardIdAndDeletedFalse(boardId: Long): Boolean
    fun findByIdAndBoardIdAndDeletedFalse(id: Long, boardId: Long): State?
    fun findByBoardAndOrder(board: Board, order: Short): State?
}


@Repository
interface StateValidationRepository : BaseRepository<StateValidation> {

}
