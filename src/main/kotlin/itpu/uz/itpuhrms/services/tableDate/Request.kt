package itpu.uz.itpuhrms.services.tableDate

import itpu.uz.itpuhrms.TableDateType

data class TableDateRequest(
    val date: Long,
    val type: TableDateType,
    val organizationId: Long = 0
)


data class TableDateUpdateRequest(
    val type: TableDateType
)
