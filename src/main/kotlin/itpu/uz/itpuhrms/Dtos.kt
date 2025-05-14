package itpu.uz.itpuhrms


import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.*
import org.springframework.data.domain.PageImpl
import itpu.uz.itpuhrms.config.ValidWorkRate
import itpu.uz.itpuhrms.services.board.BoardAdminResponse
import itpu.uz.itpuhrms.services.board.BoardResponse
import itpu.uz.itpuhrms.services.employee.EmployeeResponse
import itpu.uz.itpuhrms.services.projectEmployee.ProjectEmployeeResponse
import itpu.uz.itpuhrms.services.state.StateResponse
import itpu.uz.itpuhrms.services.taskActionHistory.PriorityResponse
import java.util.*



//data class BoardStateResponse(
//    val pageable: PageImpl<StateResponse>
//) {
//    companion object {
//        fun toResponse(board: Board, pageable: PageImpl<StateResponse>) = board.run {
//            BoardStateResponse(
//                pageable
//            )
//        }
//    }
//}
//
//
//
//data class ProjectBoardResponse(
//    val id: Long,
//    val name: String,
//    val description: String?,
//    val department: String,
//    val pageable: PageImpl<BoardAdminResponse>
//) {
//    companion object {
//        fun toResponse(project: Project, pageable: PageImpl<BoardAdminResponse>) = project.run {
//            ProjectBoardResponse(
//                id!!,
//                name,
//                description,
//                project.department.name,
//                pageable
//            )
//        }
//    }
//}
//
//
//
//
//
//
//data class HeadDepartmentRequest(
//    val name: String,
//    val description: String?,
//    val departmentType: DepartmentType
//)
//
//
//
//
//
//
//data class EmployeeImageUpdateRequest(
//    val id: Long,
//    val imageHashId: String,
//)
//
//data class EmployeeHireRequest(
//    val status: Status,
//    val positionId: Long,
//    val departmentId: Long,
//    @field:Positive(message = "workRate must be positive")
//    @field:ValidWorkRate
//    val workRate: Double,
//    @field:Positive(message = "laborRate must be positive")
//    val laborRate: Short
//)
//
//
//
//data class TaskAdminResponse(
//    val id: Long,
//    val title: String,
//    val employees: List<ProjectEmployeeResponse> = mutableListOf(),
//    val state: StateResponse,
//    val board: BoardResponse,
//    var priority: PriorityResponse,
//    var files: List<String> = mutableListOf(),
//    var order: Short,
//    var startDate: Date? = null,
//    var endDate: Date? = null,
//    var description: String? = null,
//    var parentTaskId: Long? = null,
//    val timeEstimateAmount: Int? = null,
//    val subTasks: List<TaskAdminResponse>? = null
//) {
//    companion object {
//        fun toDto(task: Task, subtasks: List<Task>? = null): TaskAdminResponse {
//            return TaskAdminResponse(
//                task.id!!,
//                task.title,
//                task.employees.map { ProjectEmployeeResponse.toDto(it) }.sortedBy { it.id },
//                StateResponse.toDto(task.state),
//                BoardResponse.toDto(task.board),
//                PriorityResponse.toResponse(task.priority),
//                task.files.map { it.hashId },
//                task.order,
//                task.startDate,
//                task.endDate,
//                task.description,
//                task.parentTask?.id,
//                task.timeEstimateAmount,
//                subtasks?.map { toDto(it) }
//            )
//        }
//    }
//}




















data class VisitorDeleteRequest(
    val organizationId: Long,
    val visitors: List<String>
)




@JsonInclude(Include.NON_NULL)
data class FeignRequest<Payload>(
    var url: String,
    var password: String,
    var username: String,
    var payload: Payload,
)

data class FeignResponse(
    val statusCode: String?,
    val statusString: String?,
    val subStatusCode: String?,
    @JsonProperty("error_code")
    val errorCode: Int?,
    val status_code: Int?,
    val data: Data?
)

data class Data(
    val errorCode: Int?,
    val errorMsg: String?,
    val statusCode: Int?,
    val statusString: String?,
    val subStatusCode: String?,
    val status_code: Int?,
    val message: String?,
)

data class PayloadAdd(
    @JsonProperty("UserInfo")
    val addInfo: UserInfoAddRequest
)

data class PayloadEdit(
    @JsonProperty("UserInfo")
    val editInfo: UserInfoEditRequest
)

data class PayloadSearch(
    @JsonProperty("UserInfoSearchCond")
    val userInfoSearchCond: UserInfoSearchCond? = null
)

data class UserInfoSearchCond(
    val searchID: String,
    val searchResultPosition: Long,
    val maxResults: Int
)

data class UserInfoCount(
    val bindCardUserNumber: Long,
    val bindFaceUserNumber: Long,
    val userNumber: Long,
)

data class TourniquetEmployeeResponse(
    var name: String,
    var userType: String,
    var gender: String,
    var employeeResponse: EmployeeResponse? = null,
    val employeeNo: String,
    @JsonProperty("Valid")
    var valid: ValidResponse,
    val password: String? = null
) {
    companion object {
        fun toDto(userInfo: UserInfoResponse, employee: Employee? = null) = userInfo.run {
            TourniquetEmployeeResponse(
                name,
                userType,
                gender,
                employee?.let {
                    EmployeeResponse.toDto(it)
                },
                employeeNo,
                valid,
                password
            )
        }
    }
}

data class Root(
    @JsonProperty("UserInfoSearch")
    val userInfoSearch: UserInfoSearch,
)

data class UserInfoSearch(
    @JsonProperty("UserInfo")
    val userInfo: MutableList<UserInfoResponse> = mutableListOf(),
    val numOfMatches: Long,
    val responseStatusStrg: String,
    @JsonProperty("searchID")
    val searchId: String,
    val totalMatches: Long,
)



data class UserInfoEditRequest(
    val employeeNo: String,
    val name: String,
    @JsonProperty("Valid")
    val valid: ValidRequest,
    val userType: String
)

data class UserInfoResponse(
    val employeeNo: String,
    val name: String,
    @JsonProperty("Valid")
    val valid: ValidResponse,
    val userType: String,
    val gender: String,
    val password: String,
)


data class ValidResponse(
    val beginTime: String,
    val enable: Boolean,
    val endTime: String,
    val timeType: String,
)


data class FaceResponse(
    @JsonProperty("MatchList")
    val matchList: List<MatchList>? = null,
    val numOfMatches: Long?,
    val responseStatusStrg: String?,
    val statusCode: Long?,
    val statusString: String?,
    val subStatusCode: String?,
    val totalMatches: Long?,
    val data: Data?,
    @JsonProperty("error_code")
    val errorCode: Int?,
    val status_code: Int?,
)

data class MatchList(
    @JsonProperty("FPID")
    val employeeId: String,
    val faceURL: String,
    val modelData: String
)
