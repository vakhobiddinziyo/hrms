package itpu.uz.itpuhrms

import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import itpu.uz.itpuhrms.security.AuthService
import itpu.uz.itpuhrms.security.LoginRequest
import itpu.uz.itpuhrms.security.RefreshRequest
import itpu.uz.itpuhrms.services.*


const val BASE_API = "api/v1"

@RestController
@RequestMapping("$BASE_API/auth")
class AuthController(
    private val authService: AuthService
) {
    /**
     * Authenticate user.
     *
     * Authenticates user with the provided credentials.
     *
     * @param request The login request object containing username and password.
     * @return The authentication response object containing authentication token and user details.
     */
    @PostMapping("login")
    fun login(@RequestBody request: LoginRequest) =
        authService.authenticate(request)

    /**
     * Refresh authentication token.
     *
     * Refreshes the authentication token with the provided refresh token.
     *
     * @param request The refresh request object containing refresh token.
     * @return The authentication response object containing new authentication token.
     */

    @PostMapping("refresh")
    fun refresh(@RequestBody request: RefreshRequest) = authService.refresh(request)
}

@RestController
@RequestMapping("$BASE_API/user")
class UserController(
    private val service: UserService
) {
    @GetMapping("roles")
    fun getRoles() = service.getAllRoles()

    @PreAuthorize("hasAnyAuthority('DEVELOPER', 'ADMIN')")
    @PostMapping("admin")
    fun createUser(@Valid @RequestBody request: UserAdminRequest) = service.createUser(request)

    @PreAuthorize("hasAnyAuthority('DEVELOPER', 'ADMIN')")
    @PutMapping("admin/{id}")
    fun updateUser(@PathVariable id: Long, @RequestBody @Valid request: UserAdminUpdateRequest) =
        service.updateUser(id, request)

    @PutMapping("client/{id}")
    fun update(@PathVariable id: Long, @Valid @RequestBody request: UserUpdateRequest) = service.update(id, request)

    @PreAuthorize("hasAnyAuthority('DEVELOPER', 'ADMIN')")
    @GetMapping("admin")
    fun getAllUserList(
        @RequestParam role: Role?,
        @RequestParam search: String?,
        pageable: Pageable
    ) = service.getAll(role, search, pageable)

    @PreAuthorize("hasAnyAuthority('DEVELOPER', 'ADMIN')")
    @GetMapping("admin/{id}")
    fun getOneAdmin(@PathVariable id: Long) = service.getOneByIdAdmin(id)

    @GetMapping("client")
    fun getAllClients(
        @RequestParam search: String?,
        @RequestParam gender: Gender?,
        pageable: Pageable
    ) = service.getAllClients(search, gender, pageable)

    @PostMapping("client")
    fun createClient(@Valid @RequestBody request: UserRequest) = service.createClient(request)

    @GetMapping("me")
    fun userMe() = service.userMe()

    @PostMapping("search")
    fun getByPinfl(@RequestBody request: PinflRequest) = service.getByPinfl(request)

    @GetMapping("{id}")
    fun getOneById(@PathVariable id: Long) = service.getOneById(id)

    @PostMapping("credentials")
    fun saveOrgAdminCredentials(@RequestBody request: UserCredentialsRequest) = service.saveOrgAdminCredentials(request)

    @PreAuthorize("hasAnyAuthority('DEVELOPER', 'ADMIN')")
    @DeleteMapping("{id}")
    fun delete(@PathVariable id: Long) = service.delete(id)
}

@RestController
@RequestMapping("$BASE_API/organization")
class OrganizationController(
    private val service: OrganizationService
) {
    @PreAuthorize("hasAnyAuthority('DEVELOPER', 'ADMIN')")
    @PostMapping
    fun createOrganization(@Valid @RequestBody request: OrgRequest) = service.create(request)

    @PreAuthorize("hasAnyAuthority('DEVELOPER', 'ADMIN')")
    @PutMapping("{id}")
    fun update(@PathVariable id: Long, @Valid @RequestBody request: OrgRequest) = service.update(id, request)

    @PreAuthorize("hasAnyAuthority('DEVELOPER', 'ADMIN')")
    @DeleteMapping("{id}")
    fun delete(@PathVariable id: Long) = service.delete(id)

    @GetMapping("{id}")
    fun getById(@PathVariable id: Long) = service.getById(id)

    @PreAuthorize("hasAnyAuthority('DEVELOPER', 'ADMIN')")
    @GetMapping
    fun getList(
        @RequestParam search: String?,
        @RequestParam status: Status?,
        @RequestParam active: Boolean?,
        pageable: Pageable
    ) = service.getList(search, status, active, pageable)

    @PreAuthorize("hasAnyAuthority('DEVELOPER', 'ADMIN','ORG_ADMIN')")
    @GetMapping("statistics")
    fun getStatistics() = service.getOrganizationStatistics()

    @PreAuthorize("hasAnyAuthority('DEVELOPER', 'ADMIN')")
    @PutMapping("active/{id}")
    fun changeOrgActive(@PathVariable id: Long) = service.changeOrgActive(id)
}

@RestController
@RequestMapping("$BASE_API/permissions")
class PermissionController(
    private val permissionService: PermissionService
) {
    @PreAuthorize("hasAnyAuthority('DEVELOPER', 'ADMIN')")
    @PostMapping
    fun create(@Valid @RequestBody request: PermissionRequest) = permissionService.create(request)

    //    @PreAuthorize("hasAnyAuthority('DEVELOPER', 'ADMIN')")
    @GetMapping("{id}")
    fun getById(@PathVariable id: Long) = permissionService.getById(id)

    //    @PreAuthorize("hasAnyAuthority('DEVELOPER', 'ADMIN')")
    @GetMapping
    fun getAll() = permissionService.getAll()

    @PreAuthorize("hasAnyAuthority('DEVELOPER', 'ADMIN')")
    @PutMapping("{id}")
    fun update(@PathVariable id: Long, @Valid @RequestBody updateRequest: PermissionRequest) =
        permissionService.update(id, updateRequest)

    @PreAuthorize("hasAnyAuthority('DEVELOPER', 'ADMIN')")
    @DeleteMapping("{id}")
    fun delete(@PathVariable id: Long) = permissionService.delete(id)
}

@RestController
@RequestMapping("$BASE_API/department")
class DepartmentController(
    private val service: DepartmentService
) {
    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @PostMapping
    fun create(@Valid @RequestBody request: DepartmentRequest) = service.create(request)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @PutMapping("{id}")
    fun update(@PathVariable id: Long, @Valid @RequestBody request: DepartmentRequest) =
        service.update(id, request)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @DeleteMapping("{id}")
    fun delete(@PathVariable id: Long) = service.delete(id)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @GetMapping("{id}")
    fun getById(@PathVariable id: Long) = service.getById(id)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @GetMapping
    fun getDepartmentPage(pageable: Pageable) = service.getPage(pageable)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @GetMapping("list")
    fun getDepartmentList(@RequestParam parentId: Long?) = service.getListForStructure(parentId)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @GetMapping("list-parent")
    fun getDepartmentListByParent(@RequestParam parentId: Long?) = service.getListForStructureByParent(parentId)

    @PreAuthorize("hasAnyAuthority('DEVELOPER', 'ADMIN')")
    @GetMapping("admin")
    fun departmentList(
        @RequestParam type: DepartmentType,
        @RequestParam orgId: Long
    ) = service.getListByType(type, orgId)
}

@RestController
@RequestMapping("$BASE_API/project")
class ProjectController(
    private val service: ProjectService
) {

    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @PostMapping
    fun createProject(@RequestBody @Valid request: ProjectCreateRequest) = service.createProject(request)

    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @PutMapping("{id}")
    fun updateProject(@PathVariable id: Long, @RequestBody @Valid request: ProjectUpdateRequest) =
        service.update(id, request)

    @GetMapping("{id}")
    fun getOneById(@PathVariable id: Long) = service.getProjectById(id)


    @PreAuthorize("hasAnyAuthority('ORG_ADMIN')")
    @GetMapping
    fun getProjects(@RequestParam departmentId: Long?, @RequestParam status: ProjectStatus?, pageable: Pageable) =
        service.getAllProjectForOrgAdmin(departmentId, status, pageable)

    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @GetMapping("me")
    fun getProjectsPageForCurrentUser(
        @RequestParam search: String?,
        @RequestParam status: ProjectStatus?,
        pageable: Pageable
    ) = service.getProjectsPageForCurrentUser(search, status, pageable)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @GetMapping("list")
    fun getProjectsList(@RequestParam departmentId: Long?) = service.getAllProject(departmentId)

    @DeleteMapping("{id}")
    fun deleteProject(@PathVariable id: Long) = service.delete(id)

}

@RestController
@RequestMapping("$BASE_API/position")
class PositionController(
    private val service: PositionService
) {

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @PostMapping
    fun create(@Valid @RequestBody request: PositionRequest) = service.create(request)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @PutMapping("{id}")
    fun update(@PathVariable id: Long, @Valid @RequestBody request: PositionRequest) = service.update(id, request)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @DeleteMapping("{id}")
    fun delete(@PathVariable id: Long) = service.delete(id)

    @PreAuthorize("hasAnyAuthority('ADMIN')")
    @PostMapping("admin")
    fun createAdmin(@Valid @RequestBody request: PositionAdminRequest) = service.adminCreate(request)

    @PreAuthorize("hasAnyAuthority('ADMIN')")
    @PutMapping("admin/{id}")
    fun updateAdmin(@PathVariable id: Long, @Valid @RequestBody request: PositionAdminRequest) =
        service.adminUpdate(request, id)

    @PreAuthorize("hasAnyAuthority('ADMIN')")
    @DeleteMapping("admin/{id}")
    fun deleteAdmin(@PathVariable id: Long) = service.adminDelete(id)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @GetMapping("{id}")
    fun getOneById(@PathVariable id: Long) = service.getOneById(id)

    @PreAuthorize("hasAnyAuthority('ADMIN','DEVELOPER')")
    @GetMapping("console/{id}")
    fun getOneByIdForConsole(@PathVariable id: Long) = service.getOneById(id)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @GetMapping
    fun getAll(pageable: Pageable) = service.getAll(pageable)

    @PreAuthorize("hasAnyAuthority('DEVELOPER', 'ADMIN')")
    @GetMapping("console")
    fun getAllForConsole(@RequestParam orgId: Long?, pageable: Pageable) = service.getAllForConsole(orgId, pageable)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @GetMapping("list")
    fun getPositionList(@RequestParam departmentId: Long) = service.getPositionList(departmentId)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @GetMapping("employee")
    fun getPositionEmployees(pageable: Pageable) = service.getPositionEmployees(pageable)
}

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

@RestController
@RequestMapping("$BASE_API/tourniquet")
class TourniquetController(
    private val service: TourniquetService
) {
    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @PostMapping
    fun create(@Valid @RequestBody request: TourniquetRequest) = service.create(request)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @PutMapping("{id}")
    fun update(@PathVariable id: Long, @Valid @RequestBody request: TourniquetUpdateRequest) =
        service.update(id, request)

    @PreAuthorize("hasAnyAuthority('DEVELOPER', 'ADMIN')")
    @GetMapping
    fun getByOrganization(pageable: Pageable, @RequestParam orgId: Long?) =
        service.get(orgId, pageable)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @GetMapping("organization")
    fun getByOrganization(pageable: Pageable) = service.getByOrganization(pageable)

    @PreAuthorize("hasAnyAuthority('DEVELOPER', 'ADMIN') or (hasAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin)")
    @GetMapping("{id}")
    fun getById(@PathVariable id: Long) = service.getOneById(id)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @DeleteMapping("{id}")
    fun delete(@PathVariable id: Long) = service.delete(id)
}

@RestController
@RequestMapping("$BASE_API/work-date")
class WorkDateController(
    private val workingDateConfigService: WorkingDateConfigService
) {
    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @PostMapping
    fun create(@Valid @RequestBody request: WorkingDateConfigRequest) = workingDateConfigService.create(request)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @PutMapping("{id}")
    fun update(@PathVariable id: Long, @Valid @RequestBody request: WorkingDateUpdateRequest) =
        workingDateConfigService.update(id, request)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @GetMapping
    fun get(pageable: Pageable) = workingDateConfigService.get(pageable)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @GetMapping("org")
    fun get() = workingDateConfigService.get()

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @GetMapping("{id}")
    fun getById(@PathVariable id: Long) = workingDateConfigService.getOne(id)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @DeleteMapping("{id}")
    fun delete(@PathVariable id: Long) = workingDateConfigService.delete(id)
}

@RestController
@RequestMapping("$BASE_API/employment")
class EmploymentHistoryController(
    private val service: EmploymentHistoryService
) {
    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @PostMapping
    fun hireEmployee(@RequestBody request: EmploymentHistoryRequest) = service.hire(request)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @PutMapping("{employeeId}")
    fun dismissEmployee(@PathVariable employeeId: Long) = service.dismiss(employeeId)

    @GetMapping
    fun getEmploymentHistory(@RequestParam userId: Long, pageable: Pageable) = service.getUserHistory(userId, pageable)

    @GetMapping("{id}")
    fun getOne(@PathVariable id: Long) = service.getOne(id)
}

@RestController
@RequestMapping("$BASE_API/board")
class BoardController(
    private val service: BoardService
) {

    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @PostMapping
    fun create(@Valid @RequestBody request: BoardRequest) = service.create(request)

    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @GetMapping("{id}")
    fun getOneById(@PathVariable id: Long) = service.getOneById(id)

    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @GetMapping
    fun getAll(
        @RequestParam projectId: Long,
        @RequestParam search: String?,
        @RequestParam status: BoardStatus?,
        pageable: Pageable
    ) = service.getAll(projectId, search, status, pageable)

    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @PutMapping("{id}")
    fun edit(@PathVariable id: Long, @Valid @RequestBody request: BoardUpdateRequest) = service.edit(id, request)

    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @DeleteMapping("{id}")
    fun delete(@PathVariable id: Long) = service.delete(id)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN')")
    @GetMapping("get-all")
    fun getAllForOrgAdmin(
        @RequestParam projectId: Long,
        @RequestParam search: String?,
        @RequestParam status: BoardStatus?,
        pageable: Pageable
    ) = service.getAllForOrgAdmin(projectId, search, status, pageable)
}

@RestController
@RequestMapping("$BASE_API/project-employee")
class ProjectEmployeeController(
    private val service: ProjectEmployeeService
) {
    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @PostMapping
    fun create(@RequestBody request: ProjectEmployeeRequest) = service.create(request)

    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @GetMapping("{id}")
    fun getOneById(@PathVariable id: Long) = service.getOneById(id)

    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @GetMapping("me")
    fun getMe(@RequestParam projectId: Long) = service.getMe(projectId)

    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @GetMapping
    fun getAllByProjectId(
        @RequestParam projectId: Long,
        @RequestParam search: String?,
        @RequestParam status: String?,
        pageable: Pageable
    ) = service.getAll(projectId, search, status, pageable)

    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @PutMapping("{id}")
    fun edit(@PathVariable id: Long, @RequestBody request: ProjectEmployeeEditRequest) = service.edit(id, request)

    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @DeleteMapping("{id}")
    fun delete(@PathVariable id: Long) = service.delete(id)
}

@RestController
@RequestMapping("$BASE_API/state")
class StateController(
    private val service: StateService
) {
    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @PostMapping
    fun create(@Valid @RequestBody request: StateRequest) = service.create(request)

    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @GetMapping("{id}")
    fun getOneById(@PathVariable id: Long) = service.getOneById(id)

    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @PostMapping("page")
    fun getAll(
        @RequestBody request: StateSearch,
        pageable: Pageable
    ) = service.getAll(request, pageable)

    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @PutMapping("{id}")
    fun edit(@PathVariable id: Long, @Valid @RequestBody request: StateUpdateRequest) = service.edit(id, request)

    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @DeleteMapping("{id}")
    fun delete(@PathVariable id: Long) = service.delete(id)
}

@RestController
@RequestMapping("$BASE_API/comment")
class CommentController(
    private val service: CommentService
) {
    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @PostMapping
    fun create(@RequestBody @Valid request: CommentRequest) = service.create(request)

    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @GetMapping("{id}")
    fun getOneById(@PathVariable id: Long) = service.getOneById(id)

    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @GetMapping
    fun getAll(@RequestParam taskId: Long, pageable: Pageable) = service.getAll(taskId, pageable)

    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @PutMapping("{id}")
    fun edit(@PathVariable id: Long, @RequestBody @Valid request: CommentRequest) = service.edit(id, request)

    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @DeleteMapping("{id}")
    fun delete(@PathVariable id: Long) = service.delete(id)
}

@RestController
@RequestMapping("$BASE_API/user-organization")
class UserOrgStoreController(
    private val service: UserOrgStoreService,
) {
    @PreAuthorize("hasAnyAuthority('DEVELOPER')")
    @PostMapping
    fun create(@RequestBody @Valid request: UserOrgStoreRequest) = service.createUserOrgStore(request)


    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @PutMapping("role")
    fun updateUserOrgStoreRole(@RequestParam orgId: Long) = service.updateUserOrgStoreRole(orgId)

    @GetMapping
    fun getUserOrganizations(pageable: Pageable) =
        service.getUserOrganizations(pageable)
}

@RestController
@RequestMapping("$BASE_API/task")
class TaskController(
    private val service: TaskService
) {

    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @PostMapping
    fun create(@Valid @RequestBody request: TaskCreateRequest) = service.create(request)

    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @GetMapping("{id}")
    fun getOneById(@PathVariable id: Long) = service.getOneById(id)

    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @GetMapping
    fun getAll(
        @RequestParam parentId: Long?,
        @RequestParam boardId: Long,
        pageable: Pageable
    ) = service.getAll(parentId, boardId, pageable)

    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @GetMapping("/type")
    fun getAllTaskByType() = service.getTasksByType()

//    @GetMapping("/employee")
//    fun getAllByEmployeeId(
//        @RequestParam("search") search: String?,
//        pageable: Pageable) = service.getAll(search, pageable )
//


    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @PutMapping("{id}")
    fun edit(@PathVariable id: Long, @Valid @RequestBody request: TaskUpdateRequest) = service.edit(id, request)

    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @PutMapping("change-state/{id}")
    fun changeState(
        @PathVariable id: Long,
        @RequestParam stateId: Long,
        @RequestParam order: Short
    ) = service.changeState(id, stateId, order)

    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @PutMapping("move/{taskId}")
    fun moveTaskToBoard(
        @PathVariable taskId: Long,
        @RequestParam newBoardId: Long
    ) = service.moveTaskToBoard(taskId, newBoardId)

    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @DeleteMapping
    fun delete(@RequestBody request: TaskDeleteRequest) = service.delete(request)

    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @GetMapping("search-by-id")
    fun searchById(
        @RequestParam taskId: Long?,
        @RequestParam boardId: Long,
        @RequestParam startDate: Long?,
        @RequestParam endDate: Long?,
        @RequestParam priority: TaskPriority?,
        pageable: Pageable)
        = service.searchById(taskId, boardId, startDate, endDate,priority, pageable)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN')")
    @GetMapping("get-all")
    fun getAllForOrgAdmin(
        @RequestParam boardId: Long,
        @RequestParam stateName: String?,
        @RequestParam priority: TaskPriority?,
        pageable: Pageable) = service.getAllForOrgAdmin(boardId, stateName, priority, pageable)

}

@RestController
@RequestMapping("$BASE_API/task-action-history")
class TaskActionHistoryController(
    private val service: TaskActionHistoryService
) {
//    @PostMapping
//    fun create(@RequestBody request: TaskActionHistoryRequest) = service.create(request)

    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @GetMapping("{id}")
    fun getOneById(@PathVariable id: Long) = service.getOneById(id)

    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @GetMapping
    fun getAll(@RequestParam taskId: Long, pageable: Pageable) = service.getAll(taskId, pageable)

//    @DeleteMapping("{id}")
//    fun delete(@PathVariable id: Long) = service.delete(id)
}

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

@RestController
@RequestMapping("$BASE_API/time-tracking")
class TimeTrackingController(
    private val service: TimeTrackingService
) {

    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @PostMapping
    fun create(@RequestBody request: TimeTrackingCreateRequest) = service.create(request)

    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @GetMapping("{id}")
    fun getOneById(@PathVariable id: Long) = service.getOneById(id)

    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @GetMapping
    fun getAll(@RequestParam taskId: Long) = service.getAll(taskId)


    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @PutMapping("{id}")
    fun edit(@PathVariable id: Long, @RequestBody request: TimeTrackingUpdateRequest) = service.edit(id, request)

    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @DeleteMapping("{id}")
    fun delete(@PathVariable id: Long) = service.delete(id)
}

@RestController
@RequestMapping("$BASE_API/state-template")
class StateTemplateController(
    private val service: StateTemplateService
) {
    @PreAuthorize("hasAnyAuthority('DEVELOPER', 'ADMIN')")
    @PostMapping
    fun create(@Valid @RequestBody request: StateTemplateRequest) = service.create(request)


    @GetMapping("{id}")
    fun getOneById(@PathVariable id: Long) = service.getOneById(id)


    @GetMapping
    fun getAll(pageable: Pageable) = service.getAll(pageable)

    @PreAuthorize("hasAnyAuthority('DEVELOPER', 'ADMIN')")
    @PutMapping("{id}")
    fun edit(@PathVariable id: Long, @RequestBody request: StateTemplateRequest) = service.edit(id, request)

    @PreAuthorize("hasAnyAuthority('DEVELOPER', 'ADMIN')")
    @DeleteMapping("{id}")
    fun delete(@PathVariable id: Long) = service.delete(id)
}

@RestController
@RequestMapping("$BASE_API/table-date")
class TableDateController(
    private val service: TableDateService
) {
//    @PostMapping
//    fun create(@RequestBody request: TableDateRequest) = service.create(request)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @GetMapping("{id}")
    fun getOneById(@PathVariable id: Long) = service.getOneById(id)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @GetMapping
    fun getAll(
        @RequestParam startDate: Long,
        @RequestParam endDate: Long,
        pageable: Pageable
    ) = service.getAll(startDate, endDate, pageable)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @PutMapping("{id}")
    fun edit(@PathVariable id: Long, @RequestBody request: TableDateUpdateRequest) = service.edit(id, request)

    @PreAuthorize("hasAnyAuthority('DEVELOPER', 'ADMIN')")
    @GetMapping("refresh")
    fun refreshCalendar() = service.refresh()

//    @DeleteMapping("{id}")
//    fun delete(@PathVariable id: Long) = service.delete(id)
}

@RestController
@RequestMapping("$BASE_API/tourniquet-employee")
class TourniquetEmployeeController(
    private val service: TourniquetEmployeeService,
) {

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @PutMapping
    fun changeEmployeeTourniquetData(@Valid @RequestBody request: EmployeeDataRequest) =
        service.changeEmployeeTourniquetData(request)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @PostMapping("synchronize")
    fun synchronizeEmployeeTourniquetData() = service.synchronizeEmployeeTourniquetData()

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @GetMapping("{id}")
    fun getOne(@PathVariable id: Long) = service.getOneById(id)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @GetMapping
    fun getTourniquetEmployees(
        @RequestParam employeeId: Long?,
        @RequestParam tourniquetId: Long?,
        @RequestParam status: EmployeeStatus?,
        pageable: Pageable
    ) = service.getTourniquetEmployees(employeeId, tourniquetId, status, pageable)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @DeleteMapping("{id}")
    fun deleteTourniquetEmployeeData(@PathVariable id: Long) = service.deleteEmployeeData(id)

}


@RestController
@RequestMapping("$BASE_API/user-tourniquet")
class UserTourniquetController(
    private val service: UserTourniquetService
) {

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @GetMapping
    fun get(
        @RequestParam startDate: Long,
        @RequestParam endDate: Long,
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false) userId: Long?,
        pageable: Pageable
    ) = service.getOrganizationUserTourniquets(userId, startDate, endDate, search, pageable)

    @GetMapping("{userId}")
    fun getUserEvents(
        @RequestParam startDate: Long,
        @RequestParam endDate: Long,
        @PathVariable userId: Long
    ) = service.getUserEvents(userId, startDate, endDate)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @GetMapping("working-minutes")
    fun getWorkingMinutes(
        @RequestParam startDate: Long,
        @RequestParam endDate: Long,
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false) departmentId: Long?,
        @RequestParam active: Boolean = true,
        pageable: Pageable
    ) = service.getWorkingMinutes(startDate, endDate, search, departmentId, active, pageable)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @GetMapping("working-hours-excel")
    fun getWorkingMinutesExcel(
        @RequestParam startDate: Long,
        @RequestParam endDate: Long,
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false) departmentId: Long?
    ) = service.getWorkingMinutesExcel(startDate, endDate, search, departmentId)

    @GetMapping("user-working-minutes")
    fun getUserWorkingMinutes(
        @RequestParam userId: Long,
        @RequestParam startDate: Long,
        @RequestParam endDate: Long
    ) = service.getUserWorkingMinutes(userId, startDate, endDate)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @GetMapping("last-absent-events")
    fun getLastAbsentEvents(
        @RequestParam startDate: Long,
        @RequestParam endDate: Long,
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false) departmentId: Long?,
        pageable: Pageable
    ) = service.getLastAbsentEvents(startDate, endDate, search, departmentId, pageable)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @PostMapping("synchronize-events")
    fun synchronizeEvents(@RequestParam tableDateId: Long) = service.synchronizeEvents(tableDateId)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @GetMapping("attendance-excel")
    fun dailyAttendance(@RequestParam date: Long) = service.dailyAttendance(date)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @GetMapping("unknown-people")
    fun getUnknownPeople(
        @RequestParam orgId: Long,
        pageable: Pageable
    ) = service.getUnknownPeople(orgId, pageable)
}

@RestController
@RequestMapping("$BASE_API/user-tourniquet-result")
class UserTourniquetResultController(
    private val service: UserTourniquetResultService,
) {
    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @GetMapping
    fun getOrganizationResult(
        @RequestParam startDate: Long,
        @RequestParam endDate: Long
    ) = service.getOrganizationResult(startDate, endDate)
}

@RestController
@RequestMapping("$BASE_API/hook")
class TourniquetEmployeeUpdatingController(
    private val service: TourniquetEmployeeService,
    private val tourniquetService: TourniquetService,
) {
    @GetMapping("{tourniquetId}")
    fun getOrganizationResult(
        @PathVariable tourniquetId: Long,
        @RequestHeader(name = "Authorization") authorization: String
    ) = service.getTourniquetEmployeesForUpdater(tourniquetId, authorization)

    @PostMapping("synchronize")
    fun synchronizeEmployeeTourniquetData(
        @RequestBody request: SynchronizationData,
        @RequestHeader("Authorization") authorization: String
    ) = service.synchronizeEmployeeTourniquetDataFromUpdater(request, authorization)


    @PutMapping
    fun updateEmployeeData(
        @RequestHeader("Authorization") authorization: String,
        @RequestBody request: List<TourniquetEmployeeUpdateRequest>
    ) = service.updateEmployeeDataFromUpdater(request, authorization)

    @GetMapping("/tourniquets")
    fun get(@RequestHeader("Authorization") authorization: String) = tourniquetService.get(authorization)
}

@RestController
@RequestMapping("$BASE_API/tourniquet-client")
class TourniquetClientController(
    private val service: TourniquetClientService,
) {
    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @PostMapping
    fun create(@Valid @RequestBody request: ClientRequest) = service.create(request)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @PutMapping("{id}")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody request: ClientUpdateRequest
    ) = service.update(id, request)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @GetMapping("{id}")
    fun getOne(@PathVariable id: Long) = service.getOne(id)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @GetMapping
    fun get(@RequestParam search: String?, pageable: Pageable) = service.getByOrganization(search, pageable)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @DeleteMapping("{id}")
    fun delete(@PathVariable id: Long) = service.delete(id)
}


@RestController
@RequestMapping("$BASE_API/session")
class UserOrgSessionController(
    private val service: UserOrgSessionService
) {

    @GetMapping("{orgId}")
    fun getSession(@PathVariable orgId: Long) = service.getSession(orgId)

    @GetMapping("info")
    fun getOSessionInfo(@RequestHeader(name = "O-Session") id: String) = service.getSessionInfo(id)
}


@RestController
@RequestMapping("$BASE_API/telegram-bot")
class TelegramBotController(
    private val notificationService: TelegramNotificationService
) {

    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @PostMapping("hash")
    fun generateHash() = notificationService.getHash()
}


@RestController
@RequestMapping("$BASE_API/subscriber")
class SubscriberController(
    private val service: SubscriberService
) {
    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @GetMapping("me")
    fun getMe() = service.getMe()

    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @PutMapping
    fun update(@RequestBody request: SubscriberRequest) = service.update(request)
}

@RestController
@RequestMapping("$BASE_API/task-subscriber")
class TaskSubscriberController(
    private val service: TaskSubscriberService
) {
//    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
//    @PostMapping
//    fun create(@RequestBody request: TaskSubscriberRequest) = service.create(request)
//
//    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
//    @PutMapping
//    fun update(@RequestBody request: TaskSubscriberUpdateRequest) = service.update(request)
//
//    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
//    @GetMapping
//    fun get(@RequestParam boardId: Long) = service.get(boardId)
//
//    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
//    @GetMapping("{id}")
//    fun getOne(@PathVariable id: Long) = service.getOne(id)
//
//    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
//    @DeleteMapping("{id}")
//    fun delete(@PathVariable id: Long) = service.delete(id)
}

@RestController
@RequestMapping("$BASE_API/board-settings")
class BoardSettingsController(
    private val service: NotificationSettingsService
) {
    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @PostMapping
    fun create(@RequestBody request: BoardSettingsRequest) = service.create(request)

    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @PutMapping("{id}")
    fun update(@PathVariable id: Long, @RequestBody request: BoardSettingsUpdateRequest) = service.update(id, request)

    @PreAuthorize("hasAnyAuthority('USER','ORG_ADMIN') and @authorizationService.employee")
    @GetMapping("{boardId}")
    fun get(@PathVariable boardId: Long) = service.getOne(boardId)
}

@RestController
@RequestMapping("$BASE_API/user-absence-tracker")
class UserAbsenceTrackerController(
    private val service: UserAbsenceTrackerService
) {
    @PostMapping
    fun create(@RequestBody request: UserAbsenceTrackerRequest) = service.create(request)

    @PutMapping
    fun update(@RequestParam("trackerId") id: Long, @RequestBody request: UserAbsenceTrackerRequest) =
        service.update(id, request)

    @GetMapping
    fun getOne(@RequestParam("trackerId") id: Long) = service.getOne(id)

    @GetMapping("get-all")
    fun getAll(
        @RequestParam("userId") userId: Long?,
        @RequestParam("startDate") startDate: Long,
        @RequestParam("endDate") endDate: Long,
        @RequestParam("eventType") eventType: String?,
        pageable: Pageable
    ) = service.getAll(userId, startDate, endDate, eventType, pageable)

    @PreAuthorize("hasAnyAuthority('ORG_ADMIN') and @authorizationService.grantedOrgAdmin")
    @GetMapping("by-table-date")
    fun getAllByTableDate(
        @RequestParam tableDateId: Long,
        pageable: Pageable
    ) = service.getAllByTableDate(tableDateId, pageable)

    @DeleteMapping
    fun delete(@RequestParam("trackerId") id: Long) = service.delete(id)
}


@RestController
@RequestMapping("$BASE_API/statistics")
class StatisticsController(
    private val service: StatisticsService
) {
    @GetMapping("by-employees")
    fun getEmployees(@RequestParam id: Long, @RequestParam startDate: Long, @RequestParam endDate: Long) =
        service.getEmployeeStatistics(id, startDate, endDate)
}

@RestController
@RequestMapping("$BASE_API/statistic-bot")
class StatisticBotController(
    private val service: StatisticBotService) {

    @PostMapping("hash-id")
    fun getHashId() = service.getHash()
}
