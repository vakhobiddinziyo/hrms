package itpu.uz.itpuhrms.services.tourniquetEmployee

import itpu.uz.itpuhrms.EmployeeStatus

data class EmployeeDataRequest(
    val dataList: MutableList<DataRequest>
)

data class DataRequest(
    val dataId: Long,
    val status: EmployeeStatus
)

data class TourniquetEmployeeUpdateRequest(
    val eventId: Long,
    val success: Boolean,
    val error: String? = null
)



data class SynchronizationData(
    val tourniquetId: Long,
    val employees: MutableList<EmployeeSyncRequest>
)

data class EmployeeSyncRequest(
    val id: Long,
    val name: String,
)
