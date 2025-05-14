package itpu.uz.itpuhrms.services.timeTracking

import java.time.LocalTime


data class TimeTrackingCreateRequest(
    val taskId: Long,
    val startDate: Long?,
    val endDate: Long?,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val note: String?
)



data class TimeTrackingUpdateRequest(
    val startTime: LocalTime,
    val endTime: LocalTime,
    val note: String?
)
