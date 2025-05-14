package itpu.uz.itpuhrms.services.employmentHistory

import itpu.uz.itpuhrms.config.CheckPinfl


data class EmploymentHistoryRequest(
    @CheckPinfl val userPinfl: String,
    val imageHashId: String,
    val employeeId: Long
)


