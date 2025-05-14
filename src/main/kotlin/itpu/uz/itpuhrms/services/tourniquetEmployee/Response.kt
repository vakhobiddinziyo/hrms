package itpu.uz.itpuhrms.services.tourniquetEmployee

import itpu.uz.itpuhrms.*
import java.util.*

data class TourniquetEmployeeDto(
    val id: Long,
    val fullName: String,
    val phStatus: PositionHolderStatus,
    val imageHashId: String?,
    val tourniquetData: List<DataResponse>
) {
    data class DataResponse(
        val tourniquetEmployeeId: Long,
        val status: EmployeeStatus,
        val tourniquetId: Long,
        val ip: String,
        val name: String,
        val tourniquetType: TourniquetType,
        val date: Date,
        val message: String? = null
    )

    companion object {
        fun toResponse(dataMap: Map<Employee, List<EmployeeTourniquetData>>): List<TourniquetEmployeeDto> {
            return dataMap.map { mapElement ->
                mapElement.key.run {
                    TourniquetEmployeeDto(
                        id!!,
                        user?.fullName ?: (position.name + "(${workRate})"),
                        phStatus,
                        imageAsset?.hashId,
                        mapElement.value.map { data ->
                            DataResponse(
                                tourniquetEmployeeId = data.id!!,
                                status = data.status,
                                tourniquetId = data.tourniquet.id!!,
                                ip = data.tourniquet.ip,
                                name = data.tourniquet.name,
                                tourniquetType = data.tourniquet.type,
                                date = data.date,
                                message = data.message
                            )
                        }
                    )
                }
            }
        }

    }
}


data class UpdaterDataResponse(
    val employeeId: Long,
    val employeeName: String? = null,
    val image: String? = null,
)

data class HikVisionResponse(
    val eventId: Long,
    val type: HikVisionEventType,
    val tourniquetId: Long,
    val data: UpdaterDataResponse
)
