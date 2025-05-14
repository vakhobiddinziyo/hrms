package itpu.uz.itpuhrms.services.workingDate

import itpu.uz.itpuhrms.DayOfWeek
import itpu.uz.itpuhrms.WorkingDateConfig
import itpu.uz.itpuhrms.services.organization.OrgAdminResponse
import java.time.LocalDate
import java.time.LocalDateTime


data class WorkingDateConfigResponse(
    val id: Long,
    val startHour: String,
    val endHour: String,
    val day: DayOfWeek,
    val organization: OrgAdminResponse,
    val requiredMinutes: Int
) {
    companion object {
        fun toResponse(config: WorkingDateConfig) = config.run {
            WorkingDateConfigResponse(
                id!!,
                startHour.toString(),
                endHour.toString(),
                day,
                OrgAdminResponse.toDto(organization),
                requiredMinutes
            )
        }
    }
}



data class DailyAttendanceResponse(
    val pinfl: String,
    val fullName: String,
    val positionName: String,
    val startDate: LocalDateTime?,
    val endDate: LocalDateTime?,
    val givenDate: LocalDate
)

