package itpu.uz.itpuhrms.services.userTourniquet


import itpu.uz.itpuhrms.*
import java.util.*


data class UserTourniquetResponse(
    val userId: Long,
    val fullName: String,
    val userPinfl: String,
    val position: String,
    val events: List<EventData>
)


data class EventPairData(
    val eventIn: Date?,
    val eventInHashId: String?,
    val eventOut: Date?,
    val eventOutHashId: String?,
)

data class WorkingMinutesResponse(
    val userId: Long,
    val fullName: String,
    val dataList: List<DataResponse>
)

data class DataResponse(
    val workingDate: Long,
    val tableDateId: Long,
    val workingMinutes: Int,
    val eventAmount: Long,
    val dayType: TableDateType,
    val userAbsenceTrackerId: Long? = null
)


data class AbsentEvents(
    val userId: Long,
    val fullName: String,
    val userPinfl: String,
    val position: String,
    val department: String,
    val events: List<EventData>
)

data class UnknownPersonResponse(
    val tourniquetName: String?,
    var image: String?,
    val createdAt: Date = Date()
) {
    companion object {
        fun toDto(unknownPerson: UnknownPerson) = UnknownPersonResponse(
            unknownPerson.tourniquetName,
            unknownPerson.image,
            unknownPerson.createdAt
        )
    }
}

data class DepartmentDataResponse(
    val departmentName: String,
    val userSummary: List<ExcelWorkingMinutesResponse>
)


data class ExcelWorkingMinutesResponse(
    val userId: Long,
    val fullName: String,
    val requiredMinutes: Int,
    val positionName: String,
    val workSummary: List<ExcelDataResponse>
)



data class ExcelDataResponse(
    val workingDate: Date,
    val tableDateId: Long? = null,
    var workingMinutes: Int? = null,
    val dayType: TableDateType? = null,
    val tourniquetData: List<TourniquetData> = mutableListOf()
) {
    data class TourniquetData(
        val id: Long,
        val type: UserTourniquetType,
        val time: Date
    )
}
