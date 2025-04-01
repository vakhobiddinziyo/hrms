package itpu.uz.itpuhrms

import jakarta.persistence.*
import org.hibernate.annotations.ColumnDefault
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalTime
import java.util.*


@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
class BaseEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,
    @CreatedDate @Temporal(TemporalType.TIMESTAMP) var createdDate: Date? = null,
    @LastModifiedDate @Temporal(TemporalType.TIMESTAMP) var modifiedDate: Date? = null,
    @CreatedBy var createdBy: String? = null,
    @LastModifiedBy var lastModifiedBy: String? = null,
    @Column(nullable = false) @ColumnDefault(value = "false") var deleted: Boolean = false
)

@Entity
class Organization(
    var name: String,
    var description: String?,
    @Column(length = 60) @Enumerated(EnumType.STRING) var status: Status,
    @Column(unique = true) var tin: String,
    var isActive: Boolean = true
) : BaseEntity()

@Entity(name = "users")
class User(
    var fullName: String,
    @Column(length = 12) var phoneNumber: String,
    @Column(unique = true) var username: String,
    var password: String,
    @Enumerated(EnumType.STRING) var status: Status,
    var mail: String,
    @Enumerated(EnumType.STRING) var role: Role,
    @ManyToOne var avatarPhoto: FileAsset? = null
) : BaseEntity()

/**
 * Agarda Employee 2-3 ta organizationda 3ishlayotgan bo'lsa,
 * N organizatsiyada oddiy xodim, X organizatsiyada admin bo'lishi mumkin.
 */
@Entity
class UserOrgStore(
    @ManyToOne var user: User,
    @ManyToOne var organization: Organization,
    @Enumerated(EnumType.STRING) var role: Role,
    @ColumnDefault("false") var granted: Boolean // Bu field org admin role dagi user qaysi organizatsiyada haqiqatda orgadminlik
                                                // huquqi berilganini bilish uchun qoshilgan edi. Misol uchun men birinchi organizatsiyada
                                                // orgadmin bosam boshqasida oddiy employee boʻlishim ham mumkin
) : BaseEntity()

@Entity
class UserCredentials(
    @Column(unique = true) var pinfl: String,
    var fio: String,
    var cardGivenDate: Date,
    var cardExpireDate: Date,
    @Column(unique = true) var cardSerialNumber: String,
    @Column(length = 8) @Enumerated(EnumType.STRING) var gender: Gender,
    var birthday: Date,
    @ManyToOne var user: User
) : BaseEntity()

@Entity
class Employee(
    @ManyToOne var user: User?,
    var code: String? = null,
    @Column(length = 50) @Enumerated(EnumType.STRING) var status: Status,
    var atOffice: Boolean, // current xodim officeda yoki yo'qligi uchun
    @ManyToOne var position: Position,
    @ManyToOne var department: Department,
    @ManyToOne var organization: Organization,
    @Column(length = 20) @Enumerated(EnumType.STRING) var phStatus: PositionHolderStatus,
    var workRate: Double,
    var laborRate: Short,
    @ManyToMany var permissions: MutableSet<Permission>,
    @OneToOne var imageAsset: FileAsset? = null
) : BaseEntity()

@Entity
class Position(
    var name: String,
    @Column(length = 60) @Enumerated(EnumType.STRING) var level: Level,
    @ManyToOne var organization: Organization,
    @ManyToMany var permission: MutableSet<Permission>
) : BaseEntity()

@Entity
class Permission(
    var permissionData: String,
    var description: String? = null
) : BaseEntity()

@Entity
class Department(
    var name: String,
    var description: String?,
    @Enumerated(EnumType.STRING) var departmentType: DepartmentType,
    @ManyToOne var organization: Organization,
    @ManyToOne var parentDepartment: Department? = null,
    @ManyToOne var headDepartment: Department? = null,
    @Column(length = 20) @Enumerated(EnumType.STRING) var status: Status = Status.ACTIVE
) : BaseEntity()

@Entity
class FileAsset(
    var fileName: String,
    @Column(length = 60) var fileExtension: String,
    var fileSize: Long,
    var fileContentType: String,
    @Column(unique = true) var hashId: String,
    @Column(length = 30) var uploadFolder: String,
    var uploadFileName: String,
    var active: Boolean = false
) : BaseEntity()

@Entity
class Project(
    @Column(length = 50, nullable = false) var name: String,
    @ManyToOne(optional = false) val owner: Employee,
    @ManyToOne(optional = false) val department: Department,
    @Column(length = 175) var description: String? = null,
    @Enumerated(EnumType.STRING)
    var status: ProjectStatus = ProjectStatus.ACTIVE
) : BaseEntity()

@Entity
class ProjectEmployee(
    @ManyToOne(fetch = FetchType.LAZY, optional = false) val project: Project,
    @ManyToOne(fetch = FetchType.LAZY, optional = false) val employee: Employee,
    @Enumerated(EnumType.STRING) var projectEmployeeRole: ProjectEmployeeRole,
) : BaseEntity()

@Entity
class Board(
    @Column(length = 75, nullable = false) var name: String,
    @ManyToOne(optional = false) val project: Project,
    @ManyToOne(optional = false) val owner: Employee,
    @Enumerated(EnumType.STRING)
    var status: BoardStatus = BoardStatus.ACTIVE
) : BaseEntity()

@Entity
class State(
    @Column(length = 25, nullable = false) var name: String,
    @Column(name = "ordered") var order: Short = 0,
    @ManyToOne(fetch = FetchType.LAZY, optional = false) var board: Board,
    val immutable: Boolean = false
) : BaseEntity()

@Entity
class StateTemplate(
    var name: String,
    @Enumerated(EnumType.STRING) var status: Status
) : BaseEntity()

@Entity
class StateValidation(
    @ManyToOne var state: State,
    var stateOrder: Short,
    @Enumerated(EnumType.STRING) var status: Status,
    @ManyToOne var board: Board
) : BaseEntity()

@Entity
class Task(
    @Column(length = 225, nullable = false) var title: String,
    @ManyToMany var employees: MutableSet<ProjectEmployee>,
    @ManyToOne var state: State,
    @ManyToOne(optional = false) var board: Board,
    @ManyToMany var files: MutableList<FileAsset>,
    @Column(name = "ordered") var order: Short,
    @Enumerated(value = EnumType.STRING) @Column(length = 16) var priority: TaskPriority,
    @ManyToOne var owner: User,
    @Column(columnDefinition = "text") var description: String? = null,
    var startDate: Date? = null,
    var endDate: Date? = null,
    var timeEstimateAmount: Int? = null,
    @ManyToOne var parentTask: Task? = null,
) : BaseEntity()

@Entity
class TaskActionHistory(
    @ManyToOne var task: Task,
    @ManyToOne var owner: User,
    @Enumerated(value = EnumType.STRING) var action: TaskAction,
    @ManyToMany val files: MutableList<FileAsset>? = null,
    @ManyToOne val fromState: State? = null,
    @ManyToOne val toState: State? = null,
    @ManyToOne val subjectEmployee: Employee? = null,
    @ManyToOne val subTask: Task? = null,
    @ManyToOne val comment: Comment? = null,
    @ManyToOne val timeTracking: TimeTracking? = null,
    val dueDate: Date? = null,
    val startDate: Date? = null,
    val title: String? = null,
    val priority: TaskPriority? = null,
    val timeEstimateAmount: Int? = null
) : BaseEntity()

@Entity
class Comment(
    @ManyToOne val task: Task,
    @Column(columnDefinition = "text") var text: String,
    @ManyToOne(optional = false) var owner: User,
    @OneToMany var files: MutableList<FileAsset>? = null
) : BaseEntity()


// Apparat registratsiya
@Entity
class Tourniquet(
    @ManyToOne var organization: Organization,
    var ip: String,
    @Column(unique = true) var name: String,
    var username: String,
    var password: String,
    @Enumerated(EnumType.STRING) val type: TourniquetType,
    var description: String? = null
) : BaseEntity()


// Ishchilarni kirish chiqish log lar
@Entity
class UserTourniquet(
    @ManyToOne var organization: Organization,
    @ManyToOne var user: User,
    @ManyToOne var tourniquet: Tourniquet,
    @ManyToOne var tableDate: TableDate,
    var time: Date,
    @Enumerated(EnumType.STRING) val userType: TourniquetEmployeeType,
    @Enumerated(EnumType.STRING) var type: UserTourniquetType,
    @ManyToOne var snapshot: FileAsset? = null,
    @Enumerated(EnumType.STRING) var status: ArrivalStatus? = null
) : BaseEntity()


// 1 oylik ish kun dam kunlarni jadvali
@Entity
class TourniquetTracker(
    val inTime: Date,
    val outTime: Date,
    val amount: Int,
    @ManyToOne val user: User,
    @ManyToOne val tourniquet: Tourniquet,
    @ManyToOne val tableDate: TableDate
) : BaseEntity()

@Entity
class TableDate(
    var date: Date,
    @Enumerated(EnumType.STRING) var type: TableDateType,
    @ManyToOne var organization: Organization
) : BaseEntity()


// Task bay ishlangan soatlar
@Entity
class TimeTracking(
    @ManyToOne val owner: User,
    @ManyToOne val task: Task,
    @ManyToOne val tableDate: TableDate,
    var duration: Long,
    var startTime: LocalTime,
    var endTime: LocalTime,
    @Column(columnDefinition = "text", length = 275) var note: String? = null
) : BaseEntity()


@Entity
class WorkingDateConfig(
    @ManyToOne var organization: Organization,
    var startHour: LocalTime,
    var endHour: LocalTime,
    @ColumnDefault("480") var requiredMinutes: Int,
    @Enumerated(EnumType.STRING) var day: DayOfWeek
) : BaseEntity()


@Entity
class UserAbsenceTracker(
    @ManyToOne var user: User,
    @ManyToOne var tableDate: TableDate,
    @Enumerated(EnumType.STRING) var eventType: EventType,
    @ManyToOne var reasonDoc: FileAsset?,
    var description: String?
) : BaseEntity()

@Entity
class UserEmploymentHistory(
    @ManyToOne val employee: Employee,
    @ManyToOne val user: User,
    @ManyToOne val position: Position,
    @ManyToOne val department: Department,
    val hiredDate: Date,
    var dismissedDate: Date? = null
) : BaseEntity()


@Embeddable
class LocalizedName(
    @Column(length = 50) var uz: String,
    @Column(length = 50) var ru: String,
    @Column(length = 50) var en: String
) {
    @Transient
    fun localized(): String {
        return when (LocaleContextHolder.getLocale().language) {
            "en" -> this.en
            "ru" -> this.ru
            else -> this.uz
        }
    }

    @Transient
    fun localizedNameForBot(subscriber: Subscriber): String {
        return when (subscriber.language) {
            Language.ENG -> this.en
            Language.RU -> this.ru
            Language.UZ -> this.uz
        }
    }
}

@Entity
class UserTourniquetResult(
    val tourniquetName: String,
    val employeeId: String,
    val dateTime: Date,
    @Enumerated(EnumType.STRING)
    val type: ResultType,
    val message: String
) : BaseEntity()

@Entity
class EmployeeTourniquetData(
    @ManyToOne var employee: Employee,
    @ManyToOne var tourniquet: Tourniquet,
    @Enumerated(EnumType.STRING)
    var status: EmployeeStatus,
    var date: Date,
    var message: String? = null,
) : BaseEntity()

@Entity
class TourniquetClient(
    @ManyToOne val organization: Organization,
    @Column(unique = true) var username: String,
    var password: String
) : BaseEntity()

@Document
@CompoundIndex(def = "{'hashId': 1, 'organizationId': 1}", unique = true)
class Visitor(
    val hashId: String,
    val organizationId: Long,
    val userId: Long,
    val person: UserInfoAddRequest,
    // this is for faceImage
    var imageHashId: String,
    @Indexed(expireAfterSeconds = 86400 * 30) val createdAt: Date = Date(),
    @org.springframework.data.annotation.Id var id: String? = null
)

@Document
class UnknownPerson(
    val organizationId: Long?,
    val tourniquetId: Long?,
    val tourniquetName: String?,
    // this is for faceImage
    var image: String?,
    @Indexed(expireAfterSeconds = 86400 * 30) val createdAt: Date = Date(),
    @org.springframework.data.annotation.Id var id: String? = null
)

@Document
@CompoundIndex(def = "{'userId': 1, 'organizationId': 1}")
class UserOrgSession(
    val userId: Long,
    val organizationId: Long,
    @Enumerated(EnumType.STRING) var role: Role,
    @Indexed(expireAfter = "5m") val createdDate: Date = Date(),
    @org.springframework.data.annotation.Id var id: String? = null
)

@Entity
class Message(
    val hashId: String,
    val botUsername: String?,
    @ManyToOne val employee: Employee,
    @ColumnDefault("false") var used: Boolean,
    var retryCount: Int,
) : BaseEntity()

@Entity
class BotHashId(
    val hashId: String,
    val botUsername: String?,
    @ManyToOne val organization: Organization,
    @ManyToOne val user: User,
    @ColumnDefault("false") var used: Boolean,
    var retryCount: Int
) : BaseEntity()
@Entity
class Subscriber(
    var chatId: String,
    @ManyToOne val user: User,
    @Enumerated(EnumType.STRING) var language: Language,
    var username: String? = null,
    var botUsername: String? = null,
    @ManyToOne var organization: Organization? = null,
    var active: Boolean = true,
    var complaintCount: Int = 0
) : BaseEntity()


@Entity
class TaskSubscriber(
    @ManyToOne val subscriber: Subscriber,
    @ManyToOne val task: Task,
    val isImmutable: Boolean
) : BaseEntity()

@Entity
class BoardNotificationSettings(
    @OneToOne val board: Board,
    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING) var actions: MutableSet<TaskAction>,
) : BaseEntity()