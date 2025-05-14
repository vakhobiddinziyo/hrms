package itpu.uz.itpuhrms.services.tableDate

import itpu.uz.itpuhrms.TableDate
import itpu.uz.itpuhrms.TableDateType

data class TableDateResponse(
    val id: Long,
    val date: Long,
    val type: TableDateType,
    val organizationId: Long,
    val countAbsenceTracker: Int? = null
) {
    companion object {
        fun toDto(tableDate: TableDate, countAbsenceTracker: Int? = null) = TableDateResponse(
            tableDate.id!!,
            tableDate.date.time,
            tableDate.type,
            tableDate.organization.id!!,
            countAbsenceTracker
        )
    }
}