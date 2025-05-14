package itpu.uz.itpuhrms.services.userAbsenceTracker

import itpu.uz.itpuhrms.EventType

data class UserAbsenceTrackerRequest(
    val tableDateId: Long,
    val eventType: EventType,
    val fileHashId: String?,
    val description: String?
)