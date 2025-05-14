package itpu.uz.itpuhrms.services.hikvision


import itpu.uz.itpuhrms.utils.BASE_API
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("$BASE_API/hook/hikvision")
class HikVisionController(
    private val service: HikVisionService,
) {
    @PostMapping
    fun getHook(
        @RequestPart("Picture") image: MultipartFile?,
        @RequestPart("event_log") event: EventResponse
    ) = service.eventListener(event, image)
}
