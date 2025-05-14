package itpu.uz.itpuhrms.services.timeTracking

import itpu.uz.itpuhrms.*
import itpu.uz.itpuhrms.services.tableDate.TableDateResponse

data class TimeTrackingResponse(
    val id: Long,
    val taskId: Long,
    val duration: Long,
    val endTime: String,
    val startTime: String,
    val tableDate: TableDateResponse,
    val note: String?
) {
    companion object {
        fun toDto(entity: TimeTracking) = entity.run {
            TimeTrackingResponse(
                id!!,
                task.id!!,
                duration,
                endTime.prettyString(),
                startTime.prettyString(),
                TableDateResponse.toDto(tableDate),
                note
            )
        }
    }
}

data class TimeTrackingListResponse(
    val owner: OwnerShortResponse,
    val timeList: List<TimeTrackingResponse>
) {
    companion object {
        fun toDto(owner: User, list: List<TimeTracking>) = TimeTrackingListResponse(
            OwnerShortResponse.toDto(owner),
            list.map { TimeTrackingResponse.toDto(it) }
        )
    }
}



data class OwnerShortResponse(
    val id: Long,
    val name: String
) {
    companion object {
        fun toDto(entity: User) = entity.run { OwnerShortResponse(id!!, fullName) }
    }
}

