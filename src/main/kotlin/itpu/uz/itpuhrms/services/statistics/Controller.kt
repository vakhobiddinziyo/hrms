package itpu.uz.itpuhrms.services.statistics

import itpu.uz.itpuhrms.utils.BASE_API
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("$BASE_API/statistics")
class StatisticsController(
    private val service: StatisticsService
) {
    @GetMapping("by-employees")
    fun getEmployees(@RequestParam id: Long, @RequestParam startDate: Long, @RequestParam endDate: Long) =
        service.getEmployeeStatistics(id, startDate, endDate)
}
