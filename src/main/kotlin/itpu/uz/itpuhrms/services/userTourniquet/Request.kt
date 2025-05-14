package itpu.uz.itpuhrms.services.userTourniquet

import itpu.uz.itpuhrms.UserTourniquetType
import java.util.*

data class UserTourniquetRequest(
    val name: String,
    val employeeId: String,
    val dateTime: Date,
    val userType: String,
)


data class EventData(
    val type: UserTourniquetType,
    val eventTime: Date,
)
