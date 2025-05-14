package itpu.uz.itpuhrms.services.workingDate

import itpu.uz.itpuhrms.*
import itpu.uz.itpuhrms.services.ExtraService
import itpu.uz.itpuhrms.services.validation.ValidationService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.time.Duration

interface WorkingDateConfigService {
    fun create(request: WorkingDateConfigRequest): WorkingDateConfigResponse
    fun update(id: Long, request: WorkingDateUpdateRequest): WorkingDateConfigResponse
    fun get(pageable: Pageable): Page<WorkingDateConfigResponse>
    fun get(): List<WorkingDateConfigResponse>
    fun getOne(id: Long): WorkingDateConfigResponse
    fun delete(id: Long)
}

@Service
class WorkingDateConfigServiceImpl(
    private val repository: WorkingDateConfigRepository,
    private val extraService: ExtraService,
    private val validationService: ValidationService,
) : WorkingDateConfigService {
    override fun create(request: WorkingDateConfigRequest): WorkingDateConfigResponse {
        return request.run {
            val dayOfWeek = DayOfWeek.of(day)
            val organization = extraService.getOrgFromCurrentUser()
            val duration = Duration.between(startHour, endHour).seconds

            if (startHour >= endHour)
                throw InvalidStartTimeException()
            if (repository.existsByOrganizationIdAndDayAndDeletedFalse(organization.id!!, dayOfWeek))
                throw DayOfWeekAlreadyExistsException(day)
            if (duration.toInt() < (requiredMinutes * 60) || requiredMinutes <= 0)
                throw InvalidRequiredHourException()


            val savedConfig = repository.save(
                WorkingDateConfig(
                    organization,
                    startHour,
                    endHour,
                    requiredMinutes,
                    dayOfWeek
                )
            )
            WorkingDateConfigResponse.toResponse(savedConfig)
        }
    }

    override fun update(id: Long, request: WorkingDateUpdateRequest): WorkingDateConfigResponse {
        return repository.findByIdAndDeletedFalse(id)?.let { workingDateConfig ->
            request.run {
                if (startHour >= endHour) throw InvalidStartTimeException()
                val duration = Duration.between(startHour, endHour).seconds
                if (duration.toInt() < (requiredMinutes * 60) || requiredMinutes <= 0)
                    throw InvalidRequiredHourException()
            }
            validationService.validateDifferentOrganizations(
                workingDateConfig.organization,
                extraService.getOrgFromCurrentUser()
            )

            workingDateConfig.apply {
                this.startHour = request.startHour
                this.endHour = request.endHour
                this.requiredMinutes = request.requiredMinutes
            }
            WorkingDateConfigResponse.toResponse(repository.save(workingDateConfig))
        } ?: throw WorkingDateConfigNotFoundException()
    }

    override fun get(pageable: Pageable) =
        repository.findAllNotDeleted(pageable).map { WorkingDateConfigResponse.toResponse(it) }

    override fun get(): List<WorkingDateConfigResponse> {
        val organization = extraService.getOrgFromCurrentUser()
        return repository.findAllByOrganizationIdAndDeletedFalse(organization.id!!)
            .map { WorkingDateConfigResponse.toResponse(it) }
            .sortedBy { DayOfWeek.getDayOrder(it.day) }
    }

    override fun getOne(id: Long): WorkingDateConfigResponse {
        return repository.findByIdAndDeletedFalse(id)?.let {
            WorkingDateConfigResponse.toResponse(it)
        } ?: throw WorkingDateConfigNotFoundException()
    }

    override fun delete(id: Long) {
        repository.findByIdAndDeletedFalse(id)?.let {
            validationService.validateDifferentOrganizations(
                it.organization,
                extraService.getOrgFromCurrentUser()
            )
            repository.trash(id)
        } ?: throw WorkingDateConfigNotFoundException()
    }
}