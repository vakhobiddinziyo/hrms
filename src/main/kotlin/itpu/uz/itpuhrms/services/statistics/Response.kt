package itpu.uz.itpuhrms.services.statistics

import java.math.BigDecimal


data class EmployeeTotalStatistics(
    var imageHashId: String? = null,
    var fullName: String,
    var department: String,
    var workMinutes: BigDecimal? = null,
    var workDay: Int? = null,
    var totalWithoutEstimateAmount: Int? = null,
    var totalTaskMinutesAlreadyDone: BigDecimal? = null,
    var totalTaskMinutesNeedToDone: BigDecimal? = null,
    var lateCount: Int? = null,
    var totalLateMinutes: BigDecimal? = null,
)

data class WorkLatencyResponse(
    var lateCount: Int? = null,
    var totalLateMinutes: BigDecimal? = null,
)

data class TotalTaskInfoResponse(
    var totalWithoutEstimateAmount: Int? = null,
    var totalTaskMinutesAlreadyDone: BigDecimal? = null,
    var totalTaskMinutesNeedToDone: BigDecimal? = null
)


data class TotalWorkMinutesAndDayResponse(
    var totalWorkingMinutes: BigDecimal? = null,
    var totalWorkingDays: Int? = null,
)

