package itpu.uz.itpuhrms.services.hikvision

import java.util.*

data class HikvisionUserTourniquetRequest(
    val name: String,
    val employeeId: String,
    val dateTime: Date,
    val userType: String,
)

