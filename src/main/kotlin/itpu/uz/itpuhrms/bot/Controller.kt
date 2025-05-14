package itpu.uz.itpuhrms.bot


import itpu.uz.itpuhrms.utils.BASE_API
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("$BASE_API/statistic-bot")
class StatisticBotController(
    private val service: StatisticBotService
) {

    @PostMapping("hash-id")
    fun getHashId() = service.getHash()
}
