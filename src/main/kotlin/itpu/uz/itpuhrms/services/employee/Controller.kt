package itpu.uz.itpuhrms.services.employee

import itpu.uz.itpuhrms.AttendanceStatus
import itpu.uz.itpuhrms.BirthdayType
import itpu.uz.itpuhrms.PositionHolderStatus
import itpu.uz.itpuhrms.Status
import itpu.uz.itpuhrms.security.userId
import itpu.uz.itpuhrms.utils.BASE_API
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("$BASE_API/employee")
class EmployeeController(
    private val service: EmployeeService
) {
    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @PostMapping
    fun hire(@Valid @RequestBody request: EmployeeRequest) = service.create(request)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @PostMapping("vacant")
    fun createVacantEmployee(@Valid @RequestBody request: EmployeeVacantRequest) = service.createVacantEmployee(request)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @PutMapping("{id}")
    fun update(@PathVariable id: Long, @Valid @RequestBody request: EmployeeUpdateRequest) = service.update(id, request)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @PutMapping("change-status/{id}")
    fun changeStatus(@PathVariable id: Long, @RequestParam status: Status) = service.changeStatus(id, status)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @DeleteMapping("{id}")
    fun delete(@PathVariable id: Long) = service.delete(id)

    @PreAuthorize("hasAnyAuthority('DEVELOPER', 'ADMIN')")
    @GetMapping("console")
    fun getAllEmployee(@RequestParam organizationId: Long, pageable: Pageable) =
        service.getAllEmployee(organizationId, pageable)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @GetMapping("office-emp")
    fun getAmountEmployees() = service.getEmployeesAmount()

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @GetMapping("in-office")
    fun getAmountInOfficeEmployees(pageable: Pageable) =
        service.getInOfficeEmployees(pageable)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @GetMapping("out-office")
    fun getAmountOutOfficeEmployees(pageable: Pageable) =
        service.getOutOfficeEmployees(pageable)

    @GetMapping("statistics")
    fun getEmployeesStatistics(
        @RequestParam startDate: Long,
        @RequestParam endDate: Long,
        pageable: Pageable
    ) = service.getEmployeesStatistics(startDate, endDate, pageable)


    @GetMapping("working-days")
    fun getUserWorkingDaysStatistics(
        @RequestParam startDate: Long,
        @RequestParam endDate: Long,
    ) = service.getUserWorkingDaysStatistics(userId(), startDate, endDate)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @GetMapping
    fun getAll(
        @RequestParam status: PositionHolderStatus?,
        @RequestParam search: String?,
        @RequestParam departmentId: Long?,
        @RequestParam atOffice: Boolean?,
        @RequestParam structureTree: Boolean?,
        @RequestParam positionId: Long?,
        pageable: Pageable
    ) = service.getAll(status, search, departmentId, positionId, atOffice, structureTree, pageable)


    @GetMapping("not-in-project")
    fun getAllForProject(
        @RequestParam search: String?,
        @RequestParam projectId: Long,
        pageable: Pageable
    ) = service.getAllForProject(projectId, search, pageable)


    @GetMapping("{id}")
    fun getOneById(@PathVariable id: Long) = service.getOneById(id)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @GetMapping("birthday")
    fun getEmployeesByBirthday(
        @RequestParam type: BirthdayType,
        pageable: Pageable
    ) = service.getEmployeesByBirthday(type, pageable)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @GetMapping("late")
    fun getOrganizationLateEmployees(
        @RequestParam date: Long,
        pageable: Pageable
    ) = service.getOrganizationLateEmployees(date, pageable)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @GetMapping("absents")
    fun getOrganizationAbsentEmployees(
        @RequestParam date: Long,
        pageable: Pageable
    ) = service.getOrganizationAbsentEmployees(date, pageable)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @GetMapping("age-statistics")
    fun getEmployeeAgeStatistics() = service.getEmployeeAgeStatistics()


    @GetMapping("labor-activity")
    fun getLaborActivity() = service.getLaborActivity()

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @GetMapping("kpi")
    fun getEmployeesKPI(
        @RequestParam startDate: Long,
        @RequestParam endDate: Long,
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false) departmentId: Long?,
        @RequestParam active: Boolean = true,
        pageable: Pageable
    ) = service.getEmployeesKPI(startDate, endDate, search, departmentId, active, pageable)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @GetMapping("attendance")
    fun getEmployeesAttendance(
        @RequestParam date: Long,
        @RequestParam status: AttendanceStatus?,
        pageable: Pageable
    ) = service.getEmployeesAttendance(date, pageable, status)

    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN')")
    @GetMapping("one-kpi")
    fun getEmployeeKPI(
        @RequestParam startDate: Long,
        @RequestParam endDate: Long,
        @RequestParam employeeId: Long
    ) = service.getEmployeeKPI(employeeId, startDate, endDate)
}