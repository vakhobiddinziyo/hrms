package itpu.uz.itpuhrms.services

import org.apache.commons.logging.LogFactory
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import itpu.uz.itpuhrms.EventResponse
import itpu.uz.itpuhrms.UserTourniquetRequest

interface HikVisionService {
    fun eventListener(response: EventResponse, image: MultipartFile?): String
}

@Service
class HikVisionServiceImpl(
    private val service: UserTourniquetService,
) : HikVisionService {

    private val logger = LogFactory.getLog(javaClass)

    override fun eventListener(response: EventResponse, image: MultipartFile?): String {
        val event = response.accessControllerEvent
        if (event != null && event.currentVerifyMode != "invalid" && event.employeeNoString != null && event.userType != null) {
            event.let {
                println("${response.dateTime}" + " " + event.employeeNoString + " " + event.name)
                service.create(
                    UserTourniquetRequest(
                        event.deviceName,
                        event.employeeNoString,
                        response.dateTime,
                        event.userType
                    ),
                    image
                )
            }
            logger.info(event.toString())
        } else {
            println("${response.dateTime} --- UNKNOWN PERSON")
//           service.saveUnknownPerson(response,image)
        }
        return "ok"
    }
}