package itpu.uz.itpuhrms.services.file


import itpu.uz.itpuhrms.utils.BASE_API
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("$BASE_API/file")
class FileController(
    private val service: FileService
) {
    @PostMapping
    fun upload(@RequestParam("file") file: MultipartFile) = service.upload(file)

    @GetMapping("{hashId}")
    fun getOneByHashId(@PathVariable hashId: String) = service.getByHashId(hashId)
}