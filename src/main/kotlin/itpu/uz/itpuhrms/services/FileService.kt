package itpu.uz.itpuhrms.services

import itpu.uz.itpuhrms.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.FileUrlResource
import org.springframework.http.CacheControl
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import itpu.uz.itpuhrms.security.HashIdUtil
import java.io.File
import java.net.URLEncoder
import java.util.*
import java.util.concurrent.TimeUnit

interface FileService {
    fun upload(file: MultipartFile): FileResponse
    fun getByHashId(hashId: String): ResponseEntity<FileUrlResource>
}

@Service
class FileServiceImpl(
    private val repository: FileAssetRepository,
    @Value("\${file-asset.upload.folder}")
    private val uploadFolder: String,
    private val hashIdUtil: HashIdUtil
) : FileService {

    override fun upload(file: MultipartFile): FileResponse {
        val contentType = file.contentType ?: MediaType.MULTIPART_FORM_DATA_VALUE

        val now = Calendar.getInstance()
        val uploadFile = "${now.timeInMillis}.${file.fileExtension()}"
        val uploadFolder =
            "${uploadFolder}/${now.get(Calendar.YEAR)}/${now.get(Calendar.MONTH) + 1}/${now.get(Calendar.DAY_OF_MONTH)}"

        val fileAsset = FileAsset(
            fileName = file.fileName(),
            fileExtension = file.fileExtension(),
            fileSize = file.size,
            fileContentType = contentType,
            hashId = hashIdUtil.encode(now.timeInMillis),
            uploadFolder = uploadFolder,
            uploadFileName = uploadFile,
            active = true
        )

        val savedAsset = repository.save(fileAsset)

        val uploadLocation = File(uploadFolder).also { it.mkdirs() }
        File(uploadLocation, uploadFile).writeBytes(file.bytes)

        return FileResponse(savedAsset.hashId)
    }

    override fun getByHashId(hashId: String): ResponseEntity<FileUrlResource> {
        repository.findByHashIdAndDeletedFalse(hashId)?.let {
            return ResponseEntity.ok().header(
                HttpHeaders.CONTENT_DISPOSITION,
                "inline;filename=\"${URLEncoder.encode("${it.fileName}.${it.fileExtension}", Charsets.UTF_8)}\""
            ).contentType(MediaType.parseMediaType(it.fileContentType)).contentLength(it.fileSize)
                .cacheControl(CacheControl.maxAge(3, TimeUnit.DAYS).cachePublic())
                .body(FileUrlResource("${it.uploadFolder}/${it.uploadFileName}"))
        } ?: throw FileNotFoundException()
    }
}
