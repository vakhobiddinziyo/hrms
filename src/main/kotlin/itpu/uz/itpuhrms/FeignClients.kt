package itpu.uz.itpuhrms//package zeroone.hrms
//
//import feign.Headers
//import org.springframework.cloud.openfeign.FeignClient
//import org.springframework.http.MediaType
//import org.springframework.web.bind.annotation.*
//import org.springframework.web.multipart.MultipartFile
//
//
//@FeignClient(
//    name = "hik-person-vision-client",
//    url = "\${hik-vision.client-url}person",
//    configuration = [FeignConfiguration::class]
//)
//interface HikVisionPersonClient {
//
//    @PostMapping("add/", consumes = ["application/json"])
//    fun addPerson(@RequestBody request: FeignRequest<PayloadAdd>): FeignResponse
//
//    @PutMapping("edit/", consumes = ["application/json"])
//    fun editPerson(@RequestBody request: FeignRequest<PayloadEdit>)
//
//    @DeleteMapping("delete/")
//    fun deletePerson(@RequestBody request: FeignRequest<MutableList<String>>): FeignResponse
//
//    @PostMapping("count")
//    fun countPersons(@RequestBody request: FeignRequest<PayloadSearch>): UserInfoCount
//
//    @PostMapping("/", consumes = ["application/json"])
//    fun searchOrAll(@RequestBody request: FeignRequest<PayloadSearch>): Root
//
//    @PostMapping
//    fun addFaceImage()
//}
//
//@FeignClient(
//    name = "hik-vision-face-image-client",
//    url = "\${hik-vision.client-url}face",
//    configuration = [FeignConfiguration::class]
//)
//@Headers("Content-Type: multipart/form-data")
//interface HikVisionFaceImageClient {
//
//    @PostMapping("add/", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
//    fun addFaceImage(
//        @RequestPart img: MultipartFile,
//        @RequestPart url: String,
//        @RequestPart username: String,
//        @RequestPart password: String,
//        @RequestPart("person_id") personId: String
//    ): FeignResponse
//
//    @PostMapping("check/")
//    fun checkFaceImage(@RequestBody request: FeignRequest<String>): FaceResponse
//
//    @PostMapping("delete/")
//    fun deleteFaceImage(@RequestBody request: FeignRequest<String>)
//}