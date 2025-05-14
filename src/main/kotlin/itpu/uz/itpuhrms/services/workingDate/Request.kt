package itpu.uz.itpuhrms.services.workingDate

import java.time.LocalTime

data class WorkingDateConfigRequest(
    var startHour: LocalTime,
    var endHour: LocalTime,
    val requiredMinutes: Int,
    var day: Short,
)


data class WorkingDateUpdateRequest(
    var startHour: LocalTime,
    var endHour: LocalTime,
    var requiredMinutes: Int
)

