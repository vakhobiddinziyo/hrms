package itpu.uz.itpuhrms.services.state

import itpu.uz.itpuhrms.State
import itpu.uz.itpuhrms.services.task.TaskResponse

data class StateResponse(
    val id: Long,
    val name: String,
    val order: Short,
    val boardId: Long,
    val tasks: List<TaskResponse>?
) {
    companion object {
        fun toDto(state: State, tasks: List<TaskResponse>? = mutableListOf()) = StateResponse(
            state.id!!,
            state.name,
            state.order,
            state.board.id!!,
            tasks
        )
    }
}