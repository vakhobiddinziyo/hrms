package itpu.uz.itpuhrms.services.userTourniquetResult

import itpu.uz.itpuhrms.ResultType
import itpu.uz.itpuhrms.UserTourniquetResult
import java.util.*

data class UserTourniquetResultResponse(
    val tourniquetName: String,
    val employeeId: String,
    val dateTime: Date,
    val type: ResultType,
    val message: String
) {
    companion object {
        fun toResponse(result: UserTourniquetResult) = result.run {
            UserTourniquetResultResponse(
                tourniquetName,
                employeeId,
                dateTime, type, message
            )
        }
    }
}