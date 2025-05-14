package itpu.uz.itpuhrms.services.tourniquetEmployee

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import itpu.uz.itpuhrms.*
import itpu.uz.itpuhrms.services.ExtraService
import itpu.uz.itpuhrms.services.employee.EmployeeRepository
import itpu.uz.itpuhrms.services.tourniquet.TourniquetRepository
import itpu.uz.itpuhrms.services.tourniquetClient.TourniquetClientRepository
import itpu.uz.itpuhrms.services.validation.ValidationService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.File
import java.util.*

interface TourniquetEmployeeService {

    fun changeEmployeeTourniquetData(request: EmployeeDataRequest): List<TourniquetEmployeeDto>

    fun synchronizeEmployeeTourniquetDataFromUpdater(
        request: SynchronizationData,
        authorization: String
    ): MutableList<Long>

    fun getOneById(id: Long): List<TourniquetEmployeeDto>
    fun synchronizeEmployeeTourniquetData()
    fun getTourniquetEmployees(
        employeeId: Long?,
        tourniquetId: Long?,
        status: EmployeeStatus?,
        pageable: Pageable
    ): Page<TourniquetEmployeeDto>

    fun getTourniquetEmployeesForUpdater(tourniquetId: Long, authorization: String): List<HikVisionResponse>
    fun updateEmployeeDataFromUpdater(request: List<TourniquetEmployeeUpdateRequest>, authorization: String)

    fun deleteEmployeeData(id: Long)
}


@Service
class TourniquetEmployeeServiceImpl(
    private val employeeRepository: EmployeeRepository,
    private val validationService: ValidationService,
    private val dataRepository: EmployeeTourniquetDataRepository,
    private val tourniquetRepository: TourniquetRepository,
    private val clientRepository: TourniquetClientRepository,
    private val passwordEncoder: PasswordEncoder,
    private val extraService: ExtraService,
    private val jdbcTemplate: JdbcTemplate,
    private val objectMapper: ObjectMapper,
) : TourniquetEmployeeService {


    @Transactional
    override fun synchronizeEmployeeTourniquetData() {
        val organization = extraService.getOrgFromCurrentUser()
        val tourniquets = tourniquetRepository.findAllByOrganizationIdAndDeletedFalse(organization.id!!)
        val employees = employeeRepository.findAllByOrganizationIdAndPhStatusAndDeletedFalse(
            organization.id!!,
            PositionHolderStatus.BUSY
        )

        if (tourniquets.isEmpty()) throw TourniquetNotFoundException()
        if (employees.isEmpty()) throw EmployeeNotFoundException()

        val dataList = mutableListOf<EmployeeTourniquetData>()
        employees.forEach { employee ->
            tourniquets.forEach { tourniquet ->
                val exists = dataRepository.existsByEmployeeIdAndTourniquetIdAndDeletedFalse(
                    employee.id!!,
                    tourniquet.id!!
                )
                if (!exists) {
                    dataList.add(
                        EmployeeTourniquetData(
                            employee,
                            tourniquet,
                            EmployeeStatus.NOT_EXIST,
                            Date()
                        )
                    )
                }
            }
        }
        dataRepository.saveAll(dataList)
    }

    @Transactional
    override fun synchronizeEmployeeTourniquetDataFromUpdater(
        request: SynchronizationData,
        authorization: String
    ): MutableList<Long> {
        return request.run {
            val tourniquet = tourniquetRepository.findByIdAndDeletedFalse(tourniquetId)
            val credentials = authorization.extractPasswordAndUsername()
            val username = credentials[0]
            val password = credentials[1]
            val deletingIds = mutableListOf<Long>()
            clientRepository.findByUsernameAndDeletedFalse(username)?.let { client ->
                tourniquet?.let {
                    if (!passwordEncoder.matches(password, client.password))
                        throw UsernameOrPasswordIncorrectException()
                    if (tourniquet.organization.id != client.organization.id)
                        throw DifferentOrganizationsException()

                    val updatingDataList = mutableListOf<EmployeeTourniquetData>()
                    val dataList = dataRepository.findAllByTourniquetIdAndDeletedFalse(it.id!!)
                    val employeeMap = dataList.associateBy { data -> data.employee.id!! }

                    employees.forEach { employeeData ->
                        employeeMap[employeeData.id]?.let { existing ->
                            val updateStatus = updateStatus(existing, employeeData)
                            existing.status = updateStatus
                            updatingDataList.add(existing)
                        } ?: run {
                            deletingIds.add(
                                employeeData.id
                            )
                        }
                    }

                    val updatedEmployees = dataRepository.saveAll(updatingDataList).map { map -> map.employee }
                    val employees = employeeRepository.findAllByOrganizationIdAndPhStatusAndDeletedFalse(
                        tourniquet.organization.id!!,
                        PositionHolderStatus.BUSY
                    )
                    employees.removeAll(updatedEmployees)
                    val missingDataList = employees.map { missingEmployee ->
                        dataRepository.findByEmployeeIdAndTourniquetIdAndDeletedFalse(
                            missingEmployee.id!!,
                            tourniquet.id!!
                        )?.let { existing ->
                            existing.apply {
                                this.status = EmployeeStatus.REQUESTED
                            }
                        } ?: run {
                            EmployeeTourniquetData(
                                missingEmployee,
                                tourniquet,
                                EmployeeStatus.NOT_EXIST,
                                Date()
                            )
                        }
                    }
                    dataRepository.saveAll(missingDataList)
                }
            } ?: throw UsernameOrPasswordIncorrectException()
            deletingIds
        }
    }

    @Transactional
    override fun changeEmployeeTourniquetData(request: EmployeeDataRequest): List<TourniquetEmployeeDto> {
        val dataList = request.dataList.map { dataRequest ->
            dataRequest.run {
                val data = dataRepository.findByIdAndDeletedFalse(dataId)
                    ?: throw EmployeeDataNotFoundException()

                validationService.validateDifferentOrganizations(
                    data.tourniquet.organization,
                    extraService.getOrgFromCurrentUser()
                )
                dataRepository.save(
                    data.apply {
                        this.status = dataRequest.status
                    }
                )
            }
        }
        val dataMap = dataList.groupBy { it.employee }
        return TourniquetEmployeeDto.toResponse(dataMap)
    }

    override fun getOneById(id: Long): List<TourniquetEmployeeDto> {
        employeeRepository.findByIdAndDeletedFalse(id)?.let { employee ->

            validationService.validateDifferentOrganizations(
                employee.organization,
                extraService.getOrgFromCurrentUser()
            )
            val dataList = dataRepository.findAllByEmployeeIdAndDeletedFalse(employee.id!!)
            return TourniquetEmployeeDto.toResponse(dataList.groupBy { it.employee })
        } ?: throw EmployeeDataNotFoundException()
    }

    override fun getTourniquetEmployees(
        employeeId: Long?,
        tourniquetId: Long?,
        status: EmployeeStatus?,
        pageable: Pageable
    ): Page<TourniquetEmployeeDto> {
        val organization = extraService.getOrgFromCurrentUser()
        val queryStatus = status?.let { "'${status.name}'" }

        employeeId?.let { employeeRepository.findByIdAndDeletedFalse(it) ?: throw EmployeeNotFoundException() }
        tourniquetId?.let { tourniquetRepository.findByIdAndDeletedFalse(it) ?: throw TourniquetNotFoundException() }
        val countQuery = """
            select count(distinct e.id)
            from employee e
                     join employee_tourniquet_data etd on e.id = etd.employee_id
                     join tourniquet t on etd.tourniquet_id = t.id
            where t.deleted = false
              and etd.deleted = false
              and e.deleted = false
              and e.organization_id = ${organization.id!!}
              and (${employeeId} is null or e.id = ${employeeId})
              and (${tourniquetId} is null or t.id = ${tourniquetId})
              and (${queryStatus} is null or etd.status = ${queryStatus})
        """.trimIndent()

        val count = jdbcTemplate.queryForObject(countQuery, Long::class.java)!!
        val query = """
            select e.id                                                         as id,
                   coalesce(u.full_name, concat(p.name, '(', e.work_rate, ')')) as full_name,
                   fa.hash_id                                                   as image_hash_id,
                   e.ph_status                                                  as status,
                   jsonb_agg(
                           jsonb_build_object(
                                   'tourniquetEmployeeId', etd.id,
                                   'status', etd.status,
                                   'tourniquetId', t.id,
                                   'ip', t.ip,
                                   'name', t.name,
                                   'tourniquetType', t.type,
                                   'date', etd.date
                           )
                   )                                                            as tourniquet_data
            from employee e
                     join employee_tourniquet_data etd on e.id = etd.employee_id
                     left join file_asset fa on e.image_asset_id = fa.id
                     left join users u on e.user_id = u.id
                     join tourniquet t on etd.tourniquet_id = t.id
                     join position p on e.position_id = p.id
            where t.deleted = false
              and etd.deleted = false
              and e.deleted = false
              and e.organization_id = ${organization.id!!}
              and (${employeeId} is null or e.id = ${employeeId})
              and (${tourniquetId} is null or t.id = ${tourniquetId})
              and (${queryStatus} is null or etd.status = ${queryStatus})
            group by e.id, u.id, fa.id, p.id
            order by u.full_name
            limit ${pageable.pageSize} offset ${pageable.offset}  
        """.trimIndent()

        val content = jdbcTemplate.query(query) { rs, _ ->
            val tourniquetDataJson = rs.getString("tourniquet_data")
            TourniquetEmployeeDto(
                rs.getLong("id"),
                rs.getString("full_name"),
                PositionHolderStatus.valueOf(rs.getString("status")),
                rs.getString("image_hash_id"),
                objectMapper.readValue<List<TourniquetEmployeeDto.DataResponse>>(
                    tourniquetDataJson
                )
            )
        }
        return PageImpl(content, pageable, count)
    }


    override fun getTourniquetEmployeesForUpdater(tourniquetId: Long, authorization: String): List<HikVisionResponse> {
        val credentials = authorization.extractPasswordAndUsername()
        val username = credentials[0]
        val password = credentials[1]
        clientRepository.findByUsernameAndDeletedFalse(username)?.let { client ->
            val tourniquet = tourniquetRepository.findByIdAndDeletedFalse(tourniquetId)
                ?: throw TourniquetNotFoundException()
            if (!passwordEncoder.matches(password, client.password)) throw UsernameOrPasswordIncorrectException()
            if (tourniquet.organization.id != client.organization.id) throw DifferentOrganizationsException()

            val events = mutableListOf<HikVisionResponse>()
            events.addAll(addingEmployees(tourniquet.id!!))
            events.addAll(updatingEmployees(tourniquet.id!!))
            events.addAll(deletingEmployees(tourniquet.id!!))
            return events
        } ?: throw UsernameOrPasswordIncorrectException()
    }

    @Transactional
    override fun updateEmployeeDataFromUpdater(request: List<TourniquetEmployeeUpdateRequest>, authorization: String) {
        val credentials = authorization.extractPasswordAndUsername()
        val username = credentials[0]
        val password = credentials[1]

        request.forEach { update ->
            clientRepository.findByUsernameAndDeletedFalse(username)?.let { client ->
                if (!passwordEncoder.matches(password, client.password)) throw UsernameOrPasswordIncorrectException()
                val employeeData = dataRepository.findByIdAndDeletedFalse(update.eventId)
                    ?: throw EmployeeDataNotFoundException()
                if (employeeData.employee.organization.id != client.organization.id) throw DifferentOrganizationsException()

                when (employeeData.status) {
                    EmployeeStatus.REQUESTED,
                    EmployeeStatus.REQUEST_FAILED -> {
                        val status = if (update.success) EmployeeStatus.ACTIVE else EmployeeStatus.REQUEST_FAILED
                        updateEmployeeDataStatus(status, employeeData, update.error)
                    }

                    EmployeeStatus.UPDATE_REQUESTED,
                    EmployeeStatus.UPDATE_FAILED -> {
                        val status = if (update.success) EmployeeStatus.UPDATED else EmployeeStatus.UPDATE_FAILED
                        updateEmployeeDataStatus(status, employeeData, update.error)
                    }

                    EmployeeStatus.DELETE_REQUESTED,
                    EmployeeStatus.DELETE_FAILED -> {
                        val status = if (update.success) EmployeeStatus.DELETED else EmployeeStatus.DELETE_FAILED
                        updateEmployeeDataStatus(status, employeeData, update.error)
                    }

                    else -> {}
                }
            } ?: throw UsernameOrPasswordIncorrectException()
        }
    }

    override fun deleteEmployeeData(id: Long) {
        dataRepository.findByIdAndDeletedFalse(id)?.let { data ->
            validationService.validateDifferentOrganizations(
                data.tourniquet.organization,
                extraService.getOrgFromCurrentUser()
            )
            if (data.employee.phStatus != PositionHolderStatus.VACANT)
                throw EmployeeIsBusyException()
            dataRepository.trash(id)
        } ?: throw EmployeeDataNotFoundException()
    }

    private fun updateEmployeeDataStatus(status: EmployeeStatus, data: EmployeeTourniquetData, error: String?) {
        dataRepository.save(
            data.apply {
                this.status = status
                this.message = error
            }
        )
    }

    private fun addingEmployees(tourniquetId: Long): List<HikVisionResponse> {
        val employeesData = dataRepository.findAllByTourniquetIdAndStatusInAndDeletedFalseOrderById(
            tourniquetId,
            mutableListOf(EmployeeStatus.REQUESTED, EmployeeStatus.REQUEST_FAILED)
        )
        return employeeData(employeesData, HikVisionEventType.ADD)
    }

    private fun updatingEmployees(tourniquetId: Long): List<HikVisionResponse> {
        val employeesData = dataRepository.findAllByTourniquetIdAndStatusInAndDeletedFalseOrderById(
            tourniquetId,
            mutableListOf(EmployeeStatus.UPDATE_REQUESTED, EmployeeStatus.UPDATE_FAILED)
        )
        return employeeData(employeesData, HikVisionEventType.UPDATE)
    }

    private fun deletingEmployees(tourniquetId: Long): List<HikVisionResponse> {
        val employeesData = dataRepository.findAllByTourniquetIdAndStatusInAndDeletedFalseOrderById(
            tourniquetId,
            mutableListOf(EmployeeStatus.DELETE_REQUESTED, EmployeeStatus.DELETE_FAILED)
        )
        return employeesData.map {
            HikVisionResponse(
                it.id!!,
                HikVisionEventType.DELETE,
                it.tourniquet.id!!,
                UpdaterDataResponse(
                    it.employee.id!!
                )
            )
        }
    }


    private fun employeeData(
        employeesData: MutableList<EmployeeTourniquetData>,
        type: HikVisionEventType
    ): List<HikVisionResponse> {
        return employeesData.map { data ->
            val employee = data.employee
            val image = employee.imageAsset?.let {
                Base64.getEncoder().encodeToString(
                    File("${it.uploadFolder}/${it.uploadFileName}").readBytes()
                )
            }
            HikVisionResponse(
                data.id!!,
                type,
                data.tourniquet.id!!,
                UpdaterDataResponse(employee.id!!, employee.user?.fullName, image)
            )
        }
    }

    private fun updateStatus(existing: EmployeeTourniquetData, employee: EmployeeSyncRequest): EmployeeStatus {
        if (existing.status != EmployeeStatus.ACTIVE) return existing.status

        val status =
            if (!isFullNameEqual(existing, employee) && existing.employee.phStatus == PositionHolderStatus.BUSY)
                EmployeeStatus.UPDATE_REQUESTED else EmployeeStatus.ACTIVE
        return status
    }

    private fun isFullNameEqual(data: EmployeeTourniquetData, employee: EmployeeSyncRequest) =
        data.employee.user?.fullName == employee.name
}


//interface VisitorService {
//    fun addEmployeeToTourniquets(request: VisitorRequest)
//
//    //    fun editTourniquetEmployee(request: TourniquetEmployeeUpdateRequest)
//    fun deleteTourniquetEmployees(request: VisitorDeleteRequest)
//    fun getTourniquetEmployees(tourniquetId: Long, pageable: Pageable): Page<TourniquetEmployeeResponse>
//}
//
//@Service
//class VisitorServiceImpl(
//    private val personClient: HikVisionPersonClient,
//    private val employeeRepository: EmployeeRepository,
//    private val tourniquetRepository: TourniquetRepository,
//    private val credentialsRepository: UserCredentialsRepository,
//    private val validationService: ValidationService,
//    private val organizationRepository: OrganizationRepository,
//    private val visitorRepository: VisitorRepository,
//    private val fileAssetRepository: FileAssetRepository,
//    private val objectMapper: ObjectMapper,
//    private val faceImageClient: HikVisionFaceImageClient,
//    private val aesEncryptionService: AESEncryptionService,
//) : VisitorService {
//
//    @Transactional
//    override fun addEmployeeToTourniquets(request: VisitorRequest) {
//        request.run {
//            val credentials = credentials(pinfl)
//            val organization = organization(organizationId)
//
////            validationService.validateOrganizationUser(userId(), organization.id!!)
//
//            val tourniquets = organizationTourniquets(organizationId)
//            val hashPinfl = credentials.pinfl.hashPinfl()
//            val multipartFile = employeeImage(imageHashId)
//            val userId = credentials.user.id!!
//
//            if (tourniquets.isEmpty()) throw TourniquetIsEmptyException()
//
//            val existsEmployee = existsOrganizationEmployee(userId, organizationId)
//            if (existsEmployee) throw EmployeeExistsWithinOrganizationException()
//            val exists = existsOrganizationVisitor(hashPinfl, organizationId)
//            if (exists) throw VisitorAlreadyExists()
//
//            val userInfo = UserInfoAddRequest.userInfo(hashPinfl, credentials, TourniquetEmployeeType.VISITOR)
//            visitorRepository.save(Visitor(hashPinfl, organizationId, userId, userInfo, imageHashId))
//            val payload = PayloadAdd(userInfo)
//
//            tourniquets.forEach { tourniquet ->
//                val url = url(tourniquet.ip)
//                val password = aesEncryptionService.decode(tourniquet.password)
//                val username = tourniquet.username
//                val employeeNo = payload.addInfo.employeeNo
//
//                personClient.addPerson(FeignRequest(url, password, username, payload))
//                deleteIfHasImage(url, password, username, employeeNo)
//                faceImageClient.addFaceImage(multipartFile, url, username, password, employeeNo)
//            }
//        }
//    }
////
////    @Transactional
////    override fun editTourniquetEmployee(request: TourniquetEmployeeUpdateRequest) {
////        request.run {
////            val organization = organization(organizationId)
////            val credentials = credentials(userPinfl)
////            if (valid.startTime >= valid.endTime) throw InvalidStartTimeException()
////
//////            validationService.validateOrganizationUser(userId(), organizationId)
////            val tourniquets = organizationTourniquets(organizationId)
////            if (tourniquets.isEmpty()) throw TourniquetIsEmptyException()
////
////            val hashPinfl = credentials.pinfl.hashPinfl()
////            val visitor = visitorRepository.findByHashIdAndOrganizationId(hashPinfl, organization.id!!)
////                ?: throw VisitorNotFoundException()
////            val faceImage = faceImage(imageHashId, visitor.imageHashId)
////
////            updateVisitor(request, visitor, faceImage)
////
////            val payload = PayloadEdit.payload(visitor.hashId, name, type, valid)
////
////            tourniquets.forEach { tourniquet ->
////                val url = url(tourniquet.ip)
////                val password = aesEncryptionService.decode(tourniquet.password)
////                val username = tourniquet.username
////                val employeeNo = payload.editInfo.employeeNo
////
////                val personRequest = FeignRequest(url, password, username, payload)
////
////                faceImage?.let { image ->
////                    faceImageClient.deleteFaceImage(FeignRequest(url, password, username, employeeNo))
////                    val multipart = image.multipartFile()
////                    faceImageClient.addFaceImage(multipart, url, username, password, employeeNo)
////                }
////                personClient.editPerson(personRequest)
////            }
////        }
////    }
//
//    @Transactional
//    override fun deleteTourniquetEmployees(request: VisitorDeleteRequest) {
//        request.run {
////            validationService.validateOrganizationUser(userId(), organizationId)
//
//            val visitorList = visitors.map { visitorId ->
//                visitorRepository.findByHashIdAndOrganizationId(visitorId, organizationId)
//                    ?: throw VisitorNotFoundException()
//                visitorRepository.deleteByHashId(visitorId)
//                visitorId
//            }.toMutableList()
//
//            val tourniquets = organizationTourniquets(organizationId)
//            if (tourniquets.isEmpty()) throw TourniquetIsEmptyException()
//
//            tourniquets.forEach { tourniquet ->
//                val url = url(tourniquet.ip)
//                val password = aesEncryptionService.decode(tourniquet.password)
//                val username = tourniquet.username
//                val personRequest = FeignRequest(url, password, username, visitorList)
//                personClient.deletePerson(personRequest)
//            }
//        }
//    }
//
//    override fun getTourniquetEmployees(tourniquetId: Long, pageable: Pageable): Page<TourniquetEmployeeResponse> {
//        val tourniquet = tourniquetRepository.findByIdAndDeletedFalse(tourniquetId)
//            ?: throw TourniquetNotFoundException()
//
////        validationService.validateOrganizationUser(userId(), tourniquet.organization.id!!)
//
//        val personRequest = FeignRequest(
//            "http://${tourniquet.ip}",
//            aesEncryptionService.decode(tourniquet.password),
//            tourniquet.username,
//            PayloadSearch(
//                UserInfoSearchCond(
//                    generateRandomSearchId(),
//                    pageable.offset,
//                    pageable.pageSize
//                )
//            )
//        )
//
//        val response = personClient.searchOrAll(personRequest)
//        val searchResult = response.userInfoSearch
//        val infoList = searchResult.userInfo
//
//        val result = infoList.map { userInfo ->
//            TourniquetEmployeeResponse.toDto(userInfo, getEmployeeFromFeign(userInfo, tourniquet))
//        }
//        return PageImpl(result, pageable, searchResult.totalMatches)
//    }
//
//    private fun getEmployeeFromFeign(userInfo: UserInfoResponse, tourniquet: Tourniquet): Employee? {
//        return if (userInfo.employeeNo.isNumeric())
//            employeeRepository.findByUserIdAndOrganizationIdAndDeletedFalse(
//                userInfo.employeeNo.toLong(),
//                tourniquet.organization.id!!
//            ) else null
//    }
//
//    private fun organizationTourniquets(organizationId: Long): MutableList<Tourniquet> {
//        return tourniquetRepository.findAllByOrganizationIdAndDeletedFalse(
//            organizationId
//        )
//    }
//
//    private fun existsOrganizationVisitor(hashPinfl: String, organizationId: Long): Boolean {
//        return visitorRepository.existsByHashIdAndOrganizationId(hashPinfl, organizationId)
//    }
//
//    private fun existsOrganizationEmployee(userId: Long, organizationId: Long): Boolean {
//        return employeeRepository.existsByUserIdAndOrganizationIdAndDeletedFalse(
//            userId,
//            organizationId
//        )
//    }
//
//    private fun faceImage(newHashId: String, oldHashId: String): FileAsset? {
//        return if (newHashId != oldHashId) {
//            fileAssetRepository.findByHashIdAndDeletedFalse(
//                newHashId
//            ) ?: throw FileNotFoundException()
//        } else null
//    }
//
//    private fun employeeImage(imageHashId: String): CustomMultipartFile {
//        val file = fileAssetRepository.findByHashIdAndDeletedFalse(imageHashId)
//            ?: throw FileNotFoundException()
//        return file.multipartFile()
//    }
//
//    private fun hasFaceImage(url: String, password: String, username: String, employeeNo: String): Boolean {
//        val faceRequest = FeignRequest(url, password, username, employeeNo)
//        val checkResponse = faceImageClient.checkFaceImage(faceRequest)
//        return checkResponse.totalMatches == 0L
//    }
//
//    private fun deleteIfHasImage(url: String, password: String, username: String, employeeNo: String) {
//        val faceRequest = FeignRequest(url, password, username, employeeNo)
//        val hasFaceImage = hasFaceImage(url, password, username, employeeNo)
//        if (hasFaceImage) faceImageClient.deleteFaceImage(faceRequest)
//    }
//
////    private fun updateVisitor(request: TourniquetEmployeeUpdateRequest, visitor: Visitor, faceImage: FileAsset?) {
////        request.run {
////            visitor.person.name = name
////            visitor.person.userType = type.toLower()
////            visitor.person.valid = valid.validTimeRequest()
////            faceImage?.let { visitor.imageHashId = it.hashId }
////            visitorRepository.save(visitor)
////        }
////    }
//
//    private fun organization(organizationId: Long): Organization {
//        return organizationRepository.findByIdAndDeletedFalse(organizationId)
//            ?: throw OrganizationNotFoundException()
//    }
//
//    private fun credentials(userPinfl: String): UserCredentials {
//        return credentialsRepository.findByPinfl(userPinfl)
//            ?: throw UserCredentialsNotFoundException()
//    }
//
//}

fun url(ip: String): String {
    return "http://${ip}"
}
