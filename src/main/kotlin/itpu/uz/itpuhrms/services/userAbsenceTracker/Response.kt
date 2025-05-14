package itpu.uz.itpuhrms.services.userAbsenceTracker

import itpu.uz.itpuhrms.*
import itpu.uz.itpuhrms.services.tableDate.TableDateResponse

data class UserAbsenceTrackerResponse(
    val id: Long,
    val userId: Long,
    val tableDateResponse: TableDateResponse,
    val eventType: EventType,
    val fileDataResponse: FileDataResponse? = null,
    val description: String?
) {
    companion object {
        fun toDto(
            entity: UserAbsenceTracker,
            tableDateResponse: TableDateResponse,
            fileDataResponse: FileDataResponse? = null
        ): UserAbsenceTrackerResponse =
            UserAbsenceTrackerResponse(
                id = entity.id!!,
                userId = entity.user.id!!,
                tableDateResponse = tableDateResponse,
                eventType = entity.eventType,
                fileDataResponse = fileDataResponse,
                description = entity.description
            )
    }
}


data class UserAbsenceTrackerAdminResponse(
    val id: Long,
    val userId: Long,
    val employeeId: Long,
    val fullName: String,
    val positionName: String,
    val avatarHashId: String?,
    val eventType: EventType,
    val fileDataResponse: FileDataResponse? = null,
    val description: String?
) {
    companion object {
        fun toDto(
            entity: UserAbsenceTracker,
            employee: Employee,
            fileDateResponse: FileDataResponse?
        ): UserAbsenceTrackerAdminResponse {
            return UserAbsenceTrackerAdminResponse(
                id = entity.id!!,
                userId = entity.user.id!!,
                employeeId = employee.id!!,
                fullName = entity.user.fullName,
                positionName = employee.position.name,
                avatarHashId = employee.imageAsset?.hashId,
                eventType = entity.eventType,
                fileDataResponse = fileDateResponse,
                description = entity.description
            )
        }
    }
}


data class FileDataResponse(
    val hashId: String,
    val fileSize: Long,
    val fileContentType: String,
    val fileName: String
) {
    companion object {
        fun toResponse(file: FileAsset) = FileDataResponse(
            hashId = file.hashId,
            fileSize = file.fileSize,
            fileContentType = file.fileContentType,
            fileName = file.fileName
        )
    }
}