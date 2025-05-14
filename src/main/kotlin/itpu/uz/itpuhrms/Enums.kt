package itpu.uz.itpuhrms


enum class TokenType(val typeName: String, val grantType: String, val ttl: Long) {
    ACCESS_TOKEN("access_token", "password", 36000),
    REFRESH_TOKEN("refresh_token", "refresh_token", 3 * ACCESS_TOKEN.ttl)
}

enum class Status {
    ACTIVE,
    PENDING,
    DEACTIVATED
}


enum class BoardStatus {
    ACTIVE,
    ARCHIVED
}

enum class PositionHolderStatus {
    BUSY,
    VACANT
}


enum class BirthdayType {
    TODAY,
    TOMORROW,
    THIS_MONTH
}

enum class EmployeeStatus {
    NOT_EXIST,
    REQUESTED,
    UPDATE_REQUESTED,
    DELETE_REQUESTED,
    UPDATED,
    DELETED,
    ACTIVE,
    REQUEST_FAILED,
    UPDATE_FAILED,
    DELETE_FAILED,
}

enum class HikVisionEventType {
    ADD,
    DELETE,
    UPDATE
}






enum class StructureType {
    BOARD, PROJECT, DEPARTMENT, POSITION
}



enum class TourniquetEmployeeType {
    NORMAL,
    VISITOR,
}

enum class PermissionData {
    ADDING_EMPLOYEE
}

enum class DepartmentType {
    IT,
    FINANCE,
    HEAD
}

enum class TaskPriority(val localizedName: LocalizedName) {
    IMPORTANT(LocalizedName("Muhim", "Важный", "Important")),
    URGENT(LocalizedName("Shoshilinch", "Срочный", "Urgent")),
    HIGH(LocalizedName("Yuqori", "Высокий", "High")),
    NORMAL(LocalizedName("Odatiy", "Нормальный", "Normal")),
    LOW(LocalizedName("Past", "Низкий", "Low")),
    NONE(LocalizedName("Mavjud emas", "Никто", "None")),
}

enum class TableDateType {
    WORK_DAY,
    HOLIDAY,
    REST_DAY
}

enum class DayOfWeek(val day: Short) {
    MONDAY(1),
    TUESDAY(2),
    WEDNESDAY(3),
    THURSDAY(4),
    FRIDAY(5),
    SATURDAY(6),
    SUNDAY(7);

    companion object {
        fun of(day: Short): DayOfWeek {
            val weekDays = values()
            if (day !in 1..7) throw InvalidDayOfWeekException(day)
            return weekDays[day - 1]
        }

        fun getDayOrder(day: DayOfWeek): Int {
            return when (day) {
                MONDAY -> 1
                TUESDAY -> 2
                WEDNESDAY -> 3
                THURSDAY -> 4
                FRIDAY -> 5
                SATURDAY -> 6
                SUNDAY -> 7
            }
        }
    }

}

enum class Language {
    UZ, ENG, RU
}



enum class EmploymentHistoryType {
    HIRED,
    DISMISSED
}

enum class TaskAction {
    CREATE_TASK,
    CREATE_SUB_TASK,
    REMOVE_SUB_TASK,
    ASSIGN_USER,
    REMOVE_USER,
    SET_DUE_DATE,
    CHANGE_DUE_DATE,
    REMOVE_DUE_DATE,
    SET_START_DATE,
    CHANGE_START_DATE,
    REMOVE_START_DATE,
    CHANGE_TITLE,
    SET_TASK_PRIORITY,
    CHANGE_TASK_PRIORITY,
    REMOVE_TASK_PRIORITY,
    CHANGE_STATE,
    FILE_UPLOAD,
    REMOVE_FILE,
    TIME_TRACKED,
    ADD_COMMENT,
    TIME_AMOUNT_ESTIMATED,
    ESTIMATED_TIME_AMOUNT_REMOVED,
}

enum class EventType {
    VACATION,
    BILL,
    BUSINESS_TRIP,
    ILL
}

enum class UserTourniquetType {
    IN,
    OUT
}



enum class ResultType {
    COMPLETED,
    FAILED,
}

enum class DefaultTaskState {
    OPEN,
    CLOSED
}




enum class TaskActionMessage {
    ORGANIZATION_NAME,
    PROJECT_NAME,
    ACTION_OWNER,
    TITLE,
    SUB_TASKS,
    STATE,
    EMPLOYEE,
    START_DATE,
    DUE_DATE,
    PRIORITY,
    TASK_FILE,
    TASK_COMMENT,
    TIME_TRACKED,
    ESTIMATED_TIME
}

enum class AttendanceStatus {
    ON_TIME,
    LATE,
    ABSENT
}
enum class AttendanceDataMessage{
    USER_FULL_NAME,
    POSITION_NAME,
    DATE_TIME,
    STATUS,
    EMPLOYEE_ID,
    ATTENDANCE,
    SELECT_DATE
}

enum class ArrivalStatus {
    ON_TIME,
    LATE,
    EXCUSED_LATE
}

enum class Level {
    LEAD,
    SUB_LEAD
}


enum class ProjectStatus {
    ACTIVE,
    ARCHIVED
}


enum class Gender {
    MALE,
    FEMALE
}


enum class Role {
    DEVELOPER,
    ADMIN,
    USER,
    ORG_ADMIN
}

enum class TourniquetType {
    IN,
    OUT,
    DEFAULT
}

enum class ProjectEmployeeRole {
    OWNER,
    MANAGER,
    DEVELOPER,
    GUEST
}


enum class ErrorCode(val code: Int) {
    USER_SESSION_NOT_FOUND(10),
    USER_NOT_FOUND(100),
    USER_ALREADY_EXISTS_WITH_USERNAME(101),
    UNSUPPORTED_GRANT_TYPE(102),
    BAD_CREDENTIALS(103),
    UNKNOWN_ERROR(104),
    GENERAL_API_EXCEPTION(105),
    VALIDATION_ERROR(106),
    OBJECT_ALREADY_EXISTS(107),
    OBJECT_NOT_FOUND(108),
    EMPLOYEE_NOT_FOUND(109),
    DEPARTMENT_NOT_FOUND(110),
    ORGANIZATION_ALREADY_EXIST(111),
    ORGANIZATION_NOT_FOUND(112),
    POSITION_NOT_FOUND(113),
    NOT_ALLOWED(114),
    USER_ALREADY_EXISTS_WITH_PINFL(115),
    USER_CREDENTIAL_NOT_FOUND(116),
    FILE_NOT_FOUND(117),
    PERMISSION_NOT_FOUND(118),
    USER_ORG_STORE_NOT_FOUND(119),
    TOURNIQUET_NOT_FOUND(120),
    INVALID_DAY_OF_WEEK(121),
    DAY_OF_WEEK_ALREADY_EXISTS(122),
    WORKING_DATE_CONFIG_NOT_FOUND(123),
    INVALID_START_TIME(124),
    EMPLOYEE_IS_ALREADY_BUSY(125),
    EMPLOYEE_IS_ALREADY_VACANT(126),
    EMPLOYEE_ALREADY_EXISTS_IN_ORGANIZATION(127),
    EMPLOYMENT_HISTORY_NOT_FOUND(128),
    PROJECT_NOT_FOUND(129),
    BOARD_NOT_FOUND(130),
    PROJECT_EMPLOYEE_NOT_FOUND(131),
    STATE_NOT_FOUND(132),
    ORDER_NUMBER_ALREADY_EXIST(133),
    COMMENT_NOT_FOUND(134),
    TASK_NOT_FOUND(135),
    FOREIGN_EMPLOYEE_ERROR(136),
    DIFFERENT_ORGANIZATIONS_ERROR(137),
    TOURNIQUET_NAME_ALREADY_EXISTS(138),
    DIFFERENT_USERS_ERROR(139),
    TOURNIQUET_IS_EMPTY_ERROR(140),
    FEIGN_ERROR(141),
    VISITOR_NOT_FOUND(142),
    VISITOR_ALREADY_EXISTS(143),
    NUMBER_FORMAT_ERROR(144),
    TASK_ACTION_HISTORY_NOT_FOUND(145),
    TABLE_DATE_NOT_FOUND(146),
    TIME_TRACKING_NOT_FOUND(147),
    STATE_TEMPLATE_NOT_FOUND(148),
    TABLE_DATE_ALREADY_EXCEPTION(149),
    GENERAL_API_ERROR(150),
    INVALID_ROLE_EXCEPTION(151),
    EMPLOYEE_IMAGE_ALREADY_USED(151),
    NOT_VALID_IMAGE_TYPE(152),
    IMAGE_SIZE_LIMIT(153),
    INVALID_PHONE_NUMBER(154),
    SERIAL_NUMBER_ALREADY_EXISTS(155),
    NOT_VALID_EMPLOYEE_TYPE(156),
    EMPLOYEE_DATA_NOT_FOUND(157),
    USER_ORG_STORE_ALREADY_EXIST(158),
    TOURNIQUET_CLIENT_ALREADY_EXISTS_WITH_USERNAME(159),
    TOURNIQUET_CLIENT_NOT_FOUND(160),
    INVALID_AUTHORIZATION_TYPE(161),
    USERNAME_OR_PASSWORD_INCORRECT(162),
    PAST_TABLE_DATE(163),
    DIFFERENT_BOARD_OWNER(164),
    DIFFERENT_PROJECT_OWNER(165),
    DIFFERENT_BOARD_TASK(166),
    INCORRECT_STATE_ORDER(167),
    PROJECT_EMPLOYEE_ALREADY_EXIST(168),
    INVALID_DATE_RANGE(169),
    INVALID_SUB_TASK_ERROR(170),
    INCORRECT_TASK_ORDER(171),
    TIME_NOT_COMPATIBLE(172),
    ACCESS_DENIED_ERROR(173),
    PERMISSION_DENIED_ERROR(174),
    INVALID_REQUIRED_HOUR(175),
    USER_DEACTIVATED(176),
    TIME_TRACKING_ALREADY_EXISTS(178),
    TIME_TRACKING_LIMIT(179),
    NON_WORKING_DAY(180),
    EMPLOYEE_CONNECTED_TO_TASK(181),
    DELETING_PROJECT_OWNER_ERROR(182),
    OTP_MESSAGE_LIMIT_ERROR(183),
    OTP_IS_PROHIBITED_ERROR(184),
    SUBSCRIBER_NOT_FOUND(185),
    TASK_SUBSCRIBER_NOT_FOUND(186),
    TASK_SUBSCRIBER_ALREADY_EXIST(187),
    DIFFERENT_PARENT_TASK_ERROR(188),
    INVALID_TASK_ERROR(189),
    BOARD_SETTINGS_ALREADY_EXIST(190),
    BOARD_SETTINGS_NOT_FOUND(191),
    ASSIGN_DEACTIVATED_EMPLOYEE(192),
    DIFFERENT_BOARD_PROJECT(193),
    TABLE_DATE_NOT_WORK_DAY(194),
    USER_ABSENCE_TRACKER_NOT_FOUND(195),
    USER_ABSENCE_TRACKER_ALREADY_EXISTS(196),
    NOT_SAME_MONTH_OF_YEAR(197)
}