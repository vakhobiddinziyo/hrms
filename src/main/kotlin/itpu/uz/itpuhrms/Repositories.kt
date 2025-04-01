package itpu.uz.itpuhrms

import jakarta.persistence.EntityManager
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.support.JpaEntityInformation
import org.springframework.data.jpa.repository.support.SimpleJpaRepository
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.data.repository.findByIdOrNull
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.RequestParam
import java.time.LocalDate
import java.time.LocalTime
import java.util.*

@NoRepositoryBean
interface BaseRepository<T : BaseEntity> : JpaRepository<T, Long>, JpaSpecificationExecutor<T> {
    fun findByIdAndDeletedFalse(id: Long): T?
    fun trash(id: Long): T?
    fun trashList(ids: List<Long>): List<T?>
    fun findAllNotDeleted(): List<T>
    fun findAllNotDeleted(pageable: Pageable): Page<T>
}

class BaseRepositoryImpl<T : BaseEntity>(
    entityInformation: JpaEntityInformation<T, Long>, entityManager: EntityManager
) : SimpleJpaRepository<T, Long>(entityInformation, entityManager), BaseRepository<T> {

    val isNotDeletedSpecification = Specification<T> { root, _, cb -> cb.equal(root.get<Boolean>("deleted"), false) }

    override fun findByIdAndDeletedFalse(id: Long) = findByIdOrNull(id)?.run { if (deleted) null else this }

    @Transactional
    override fun trash(id: Long): T? = findByIdOrNull(id)?.run {
        deleted = true
        save(this)
    }

    override fun findAllNotDeleted(): List<T> = findAll(isNotDeletedSpecification)
    override fun findAllNotDeleted(pageable: Pageable): Page<T> = findAll(isNotDeletedSpecification, pageable)
    override fun trashList(ids: List<Long>): List<T?> = ids.map { trash(it) }
}

@Repository
interface UserRepository : BaseRepository<User> {
    fun findByUsernameAndDeletedFalse(username: String): User?

    @Query(
        """
        select u.*
        from users u
        where ((:role is null or u.role = :role))
          and not u.role = 'DEVELOPER'
          and u.deleted = false
          and (:search is null or u.full_name ilike concat('%', :search, '%'))
        order by u.username
    """, countQuery = """
        select count(u.id)
        from users u
        where ((:role is null or u.role = :role))
          and not u.role = 'DEVELOPER'
          and u.deleted = false
          and (:search is null or u.full_name ilike concat('%', :search, '%'))
    """, nativeQuery = true
    )
    fun findAllUser(@Param("role") role: String?, search: String?, pageable: Pageable): Page<User>

    @Query(
        """
        select u.*
        from users u
                 join employee e on u.id = e.user_id
        where e.organization_id = :orgId
          and e.deleted = false
          and u.deleted = false
    """, nativeQuery = true
    )
    fun findOrganizationUsers(orgId: Long): MutableList<User>
    fun existsByUsername(username: String): Boolean

}

@Repository
interface DepartmentRepository : BaseRepository<Department> {
    fun findAllByOrganizationIdAndDeletedFalseOrderByIdDesc(organizationId: Long, pageable: Pageable): Page<Department>
    fun findByOrganizationIdAndDepartmentTypeAndDeletedFalse(orgId: Long, departmentType: DepartmentType): Department?
    fun findAllByOrganizationIdAndDepartmentTypeAndDeletedFalse(
        orgId: Long,
        departmentType: DepartmentType
    ): List<Department>

    fun findByIdAndOrganizationIdAndDeletedFalse(id: Long, organizationId: Long): Department?

    fun existsByIdAndOrganizationIdAndDeletedFalse(departmentId: Long, organizationId: Long): Boolean
    fun existsByParentDepartmentIdAndDeletedFalse(parentDepartmentId: Long): Boolean

    @Query(
        """
        select d 
        from Department d
        where d.deleted = false
          and d.organization.id = :orgId
          and (:parentId is null and d.parentDepartment is null or d.parentDepartment.id = :parentId)
    """
    )
    fun findOrganizationDepartments(orgId: Long, parentId: Long?): List<Department>
}

@Repository
interface OrganizationRepository : BaseRepository<Organization> {
    fun existsByTinAndDeletedFalse(tin: String): Boolean

    @Query(
        """select o from Organization o 
                 where (:search is null or o.name ilike concat(':search', '%') or o.tin ilike concat(':search', '%'))
                  and (:active is null or o.isActive = :active)
                  and (:status is null or o.status = :status)
                  and o.deleted = false
                  order by o.name"""
    )
    fun findAllByFilter(
        @Param("search") search: String?,
        @Param("status") status: Status?,
        @Param("active") active: Boolean?,
        pageable: Pageable
    ): Page<Organization>

    @Query(
        """
        select e.organization 
          from Employee e 
          where e.user.id = :userId
    """
    )
    fun findUserOrganizations(userId: Long): MutableList<Organization>

    @Query(
        """
          select *
          from (select (select count(d.id)
                        from department d
                        where d.organization_id = :orgId
                          and d.deleted = false)                             as departmentAmount,
                       (select count(p.id)
                        from position p
                        where p.organization_id = :orgId
                          and p.deleted = false)                             as positionAmount,
                       count(e.id)                                           as totalEmployeeAmount,
                       count(case when e.ph_status = 'BUSY' then e.id end)   as busyEmployeeAmount,
                       count(case when e.ph_status = 'VACANT' then e.id end) as vacantEmployeeAmount,
                       count(case when uc.gender = 'MALE' then uc.id end)    as maleEmployeesAmount,
                       count(case when uc.gender = 'FEMALE' then uc.id end)  as femaleEmployeesAmount
                from employee e
                         left join users u on e.user_id = u.id
                         left join user_credentials uc on u.id = uc.user_id
                where e.organization_id = :orgId
                  and e.deleted = false
                  and e.deleted = false) as employee_counts;
    """, nativeQuery = true
    )
    fun findOrganizationStatistics(orgId: Long): StatisticResponse
}

@Repository
interface UserCredentialsRepository : BaseRepository<UserCredentials> {
    fun findByPinfl(pinfl: String): UserCredentials?
    fun findByUserIdAndDeletedFalse(userId: Long): UserCredentials?
    fun findByPinflAndDeletedFalse(pinfl: String): UserCredentials?
    fun existsByPinfl(pinfl: String): Boolean
    fun existsByUserIdAndDeletedFalse(userId: Long): Boolean

    @Query(
        """
            select uc.*
            from user_credentials uc
                     join users u on uc.user_id = u.id
            where u.role = 'USER'
              and (:search is null or uc.fio ilike concat(:search, '%')
                or uc.card_serial_number ilike concat(:search, '%')
                or uc.pinfl ilike concat(:search, '%'))
              and (:gender is null or uc.gender = :gender)
              and u.deleted = false
    """, nativeQuery = true,
        countQuery = """
            select count(uc.id)
            from user_credentials uc
                     join users u on uc.user_id = u.id
            where u.role = 'USER'
              and (:search is null or uc.fio ilike concat(:search, '%')
                or uc.card_serial_number ilike concat(:search, '%')
                or uc.pinfl ilike concat(:search, '%'))
              and (:gender is null or uc.gender = :gender)
              and u.deleted = false
        """
    )
    fun findClientsWithFilter(
        @Param("search") search: String?,
        @Param("gender") gender: String?,
        pageable: Pageable
    ): Page<UserCredentials>


    fun existsByCardSerialNumber(cardSerialNumber: String): Boolean
}

@Repository
interface EmployeeRepository : BaseRepository<Employee> {


    @Query(
        """
        select e.*
        from employee e
                 join users u on e.user_id = u.id
        where e.deleted = false
          and e.ph_status = 'BUSY'
          and e.status = 'ACTIVE'
          and e.organization_id = :orgId
          and (:search is null or u.full_name ilike concat('%', :search, '%'))
          and not exists(select *
                         from project p
                                  join project_employee pe on p.id = pe.project_id
                         where pe.employee_id = e.id
                           and p.id = :projectId
                           and pe.deleted = false)
    """,
        countQuery = """
        select count(e.id)
        from employee e
                 join users u on e.user_id = u.id
        where e.deleted = false
          and e.ph_status = 'BUSY'
          and e.status = 'ACTIVE'
          and e.organization_id = :orgId
          and (:search is null or u.full_name ilike concat('%', :search, '%'))
          and not exists(select *
                         from project p
                                  join project_employee pe on p.id = pe.project_id
                         where pe.employee_id = e.id
                           and p.id = :projectId
                           and pe.deleted = false)
        """, nativeQuery = true
    )
    fun findAllEmployeeForProject(orgId: Long, projectId: Long, search: String?, pageable: Pageable): Page<Employee>

    @Query("""
        select e.imageAsset.hashId 
        from Employee e 
        where e.user.id = :userId and e.organization.id = :orgId
    """)
    fun findOwnerPhotoHashIdByUserIdAndOrgId(@Param("userId") employeeId: Long, @Param("orgId") orgId: Long): String?

    fun existsByUserIdAndOrganizationIdAndDeletedFalse(userId: Long, organizationId: Long): Boolean
    fun existsByUserIdAndOrganizationIdAndPhStatusAndDeletedFalse(
        userId: Long,
        organizationId: Long,
        phStatus: PositionHolderStatus
    ): Boolean

    // Mashi joyda oylash kerak Organization status, activligi va deleted  atributlariga, Va User deleted field ga
    fun findByUserIdAndOrganizationIdAndDeletedFalse(userId: Long, organizationId: Long): Employee?

    fun findAllByOrganizationIdAndPhStatusAndDeletedFalse(
        organizationId: Long,
        phStatus: PositionHolderStatus
    ): MutableList<Employee>

    fun findByIdAndPhStatusAndDeletedFalse(id: Long, phStatus: PositionHolderStatus): Employee?
    fun existsByImageAssetId(imageAssetId: Long): Boolean

    fun findAllByPositionAndDeletedFalse(position: Position): List<Employee>
    fun existsByUserIdAndPhStatusAndDeletedFalse(userId: Long, phStatus: PositionHolderStatus): Boolean

    @Query(
        """
             select e.*
             from employee e
                      join users u on e.user_id = u.id
                      join organization o on e.organization_id = o.id
                      join table_date td on o.id = td.organization_id
                      left join user_tourniquet ut on u.id = ut.user_id and ut.table_date_id = td.id
             where e.ph_status = 'BUSY'
               and e.status = 'ACTIVE'
               and e.deleted = false
               and td.deleted = false
               and u.deleted = false
               and o.id = :orgId
               and ut.id is null
               and td.date = :midnight
    """, countQuery = """
             select count(distinct e.id)
             from employee e
                      join users u on e.user_id = u.id
                      join organization o on e.organization_id = o.id
                      join table_date td on o.id = td.organization_id
                      left join user_tourniquet ut on u.id = ut.user_id and ut.table_date_id = td.id
             where e.ph_status = 'BUSY'
               and e.status = 'ACTIVE'
               and e.deleted = false
               and td.deleted = false
               and u.deleted = false
               and o.id = :orgId
               and ut.id is null
               and td.date = :midnight
    """,
        nativeQuery = true
    )
    fun findOrganizationAbsentEmployees(
        @Param("midnight") midnight: Date,
        @Param("orgId") orgId: Long,
        pageable: Pageable
    ): Page<Employee>

    @Query(
        """
            select coalesce(sum(case when extract(year from age(current_date, uc.birthday)) < 30 then 1 else 0 end),
                            0)                                                                                          as underThirty,
                   coalesce(sum(case
                                    when extract(year from age(current_date, uc.birthday)) between 30 and 40 then 1
                                    else 0 end),
                            0)                                                                                          as betweenThirtyAndForty,
                   coalesce(sum(case when extract(year from age(current_date, uc.birthday)) > 40 then 1 else 0 end), 0) as overForty
            from employee e
                     join users u on e.user_id = u.id
                     join user_credentials uc on u.id = uc.user_id
            where e.organization_id = :orgId
              and e.deleted = false
              and u.deleted = false
              and uc.deleted = false
              and e.ph_status = 'BUSY'
    """, nativeQuery = true
    )
    fun findOrganizationEmployeeAgeStatistics(orgId: Long): EmployeeAgeStatisticResponse

    @Query(
        """
        with employee_events as (select extract(epoch from wdc.start_hour + interval '15 minutes') * 1000                       as start_hour,
                                        min(case
                                                when ut.type = 'IN'
                                                    then extract(epoch from (ut.time - date_trunc('day', ut.time))) * 1000 end) as event_time
                                 from department d
                                          join user_employment_history ueh on d.id = ueh.department_id
                                          join table_date td on :orgId = td.organization_id
                                          join working_date_config wdc
                                               on wdc.organization_id = :orgId and wdc.day = trim(to_char(td.date, 'DAY'))
                                          left join user_tourniquet ut on ut.user_id = :userId and td.id = ut.table_date_id
                                 where d.organization_id = :orgId
                                   and td.type = 'WORK_DAY'
                                   and td.deleted = false
                                   and td.date between :startDate and :endDate
                                   and td.date <= current_timestamp at time zone 'Asia/Tashkent'
                                   and wdc.deleted = false
                                   and ((hired_date between :startDate and :endDate)
                                     or (:startDate between ueh.hired_date and coalesce(ueh.dismissed_date,
                                                                                        current_timestamp at time zone
                                                                                        'Asia/Tashkent'))
                                     or (:startDate between ueh.hired_date and coalesce(ueh.dismissed_date,
                                                                                        current_timestamp at time zone
                                                                                        'Asia/Tashkent')))
                                 group by td.id, wdc.id)
        select count(case when e.event_time > e.start_hour then 1 end)  as lateDaysAmount,
               count(case when e.event_time <= e.start_hour then 1 end) as earlyDaysAmount,
               count(case when e.event_time is null then 1 end)         as absentDaysAmount
        from employee_events e
        """, nativeQuery = true
    )
    fun findUserWorkingDaysStatistics(
        orgId: Long,
        userId: Long,
        startDate: Date,
        endDate: Date
    ): UserWorkingStatisticResponse


    fun existsByPositionIdAndDeletedFalse(positionId: Long): Boolean
    fun existsByDepartmentIdAndDeletedFalse(departmentId: Long): Boolean


    @Query("""
        select exists(
                select e from Employee e 
                join ProjectEmployee pe on pe.employee.id = e.id
                where  e.status != :status
                and pe.id in :ids
        )   
    """)
    fun existsByIdInAndStatusNot(ids: List<Long>, status: Status): Boolean

}

@Repository
interface PositionRepository : BaseRepository<Position> {

        @Query("""
        select p from Position p 
        where p.deleted = false 
        and (:organizationId is null or p.organization.id = :organizationId)
        order by p.id desc 
    """)
        fun findAllByOrganizationIdAndDeletedFalseOrderByIdDesc(organizationId: Long?, pageable: Pageable): Page<Position>
}

@Repository
interface FileAssetRepository : BaseRepository<FileAsset> {
    fun findByHashIdAndDeletedFalse(hashId: String): FileAsset?
    fun existsByHashId(hashId: String): Boolean
    fun findAllByActiveFalseAndDeletedFalse(): List<FileAsset>
    fun findAllByHashIdInAndDeletedFalse(hashId: List<String>): MutableList<FileAsset>
}

@Repository
interface ProjectRepository : BaseRepository<Project> {
    @Query(
        """select p from Project p 
                 where (:departmentId is null  or p.department.id = :departmentId) 
                 and p.deleted = false"""
    )
    fun findAllByDepartmentIdAndDeletedFalse(@Param("departmentId") departmentId: Long?): List<Project>

    @Query(
        """select p from Project p 
                 where (:departmentId is null  or p.department.id = :departmentId) 
                 and p.deleted = false"""
    )
    fun findAllByDepartmentIdAndDeletedFalse(
        @Param("departmentId") departmentId: Long?,
        pageable: Pageable
    ): Page<Project>

    fun findByIdAndDepartmentOrganizationIdAndDeletedFalse(id: Long, organizationId: Long): Project?
}

@Repository
interface ProjectEmployeeRepository : BaseRepository<ProjectEmployee> {
    @Query(
        """
        select pe.*
        from project_employee pe
                 join employee e on pe.employee_id = e.id
                 join users u on e.user_id = u.id
        where (:projectId is null or pe.project_id = :projectId)
          and pe.deleted = false
          and (:search is null or u.full_name ilike concat('%', :search, '%'))
          and (:status is null or e.status = :status)
        order by (e.id = :employeeId) desc, u.full_name
    """, nativeQuery = true,
        countQuery = """
            select count(pe.id)
            from project_employee pe
                     join employee e on pe.employee_id = e.id
                     join users u on e.user_id = u.id
            where (:projectId is null or pe.project_id = :projectId)
              and pe.deleted = false
              and (:search is null or u.full_name ilike concat('%', :search, '%'))
        """
    )
    fun findAllProjectId(
        @Param("projectId") projectId: Long?,
        employeeId: Long,
        search: String?,
        status: String?,
        pageable: Pageable
    ): Page<ProjectEmployee>

    fun findByProjectIdAndEmployeeIdAndDeletedFalse(projectId: Long, employeeId: Long): ProjectEmployee?
    fun existsByProjectIdAndEmployeeIdAndDeletedFalse(projectId: Long, employeeId: Long): Boolean

    @Query(
        """
        select count(e.id) = :employeeCount
        from project_employee pe
                 join employee e on pe.employee_id = e.id
        where pe.project_id = :projectId
          and e.deleted = false
          and e.ph_status = 'BUSY'
          and pe.deleted = false
          and pe.id in (:employeeIds)
    """, nativeQuery = true
    )
    fun existsByProjectAndEmployeeIds(projectId: Long, employeeIds: List<Long>, employeeCount: Long): Boolean
    fun existsByProjectIdAndDeletedFalse(projectId: Long): Boolean
    fun existsByEmployeeIdAndDeletedFalse(employeeId: Long): Boolean
    fun findByEmployee(employee: Employee): List<ProjectEmployee>
}

@Repository
interface BoardRepository : BaseRepository<Board> {
    fun findAllByDeletedFalseOrderByIdDesc(pageable: Pageable): Page<Board>

    @Query(
        value = """
        select *
        from board b
        where b.project_id = :projectId
          and b.deleted = false
          and (:search is null or b.name ilike concat('%', :search, '%'))
        """,
        countQuery = """
        select count(b.id)
        from board b
        where b.project_id = :projectId
          and b.deleted = false
          and (:search is null or b.name ilike concat('%', :search, '%'))
        """,
        nativeQuery = true
    )
    fun findAllByOrganizationIdAndDeletedFalse(
        projectId: Long,
        search: String?,
        pageable: Pageable
    ): Page<Board>

    fun existsByProjectIdAndDeletedFalse(projectId: Long): Boolean
}

@Repository
interface StateRepository : BaseRepository<State> {
    @Query(
        """
        select s from State s
        where (:boardId is null or s.board.id = :boardId)
        and s.deleted = false
        order by s.order asc
    """
    )
    fun findAllByBoardId(@RequestParam boardId: Long?, pageable: Pageable): Page<State>
    fun findAllByBoardIdAndDeletedFalseOrderByOrder(boardId: Long): MutableList<State>
    fun findTopByBoardIdAndDeletedFalseOrderByOrderDesc(boardId: Long): State?
    fun existsByBoardIdAndDeletedFalse(boardId: Long): Boolean
    fun findByIdAndBoardIdAndDeletedFalse(id: Long, boardId: Long): State?
    fun findByBoardAndOrder(board: Board, order: Short): State?
}

@Repository
interface StateValidationRepository : BaseRepository<StateValidation> {

}

@Repository
interface TaskRepository : BaseRepository<Task> {
    @Query(
        """
        select t from Task t
        where t.board.id = :boardId
         and t.deleted = false
         and (:parentId is null or t.parentTask.id = :parentId)
        order by t.order 
    """
    )
    fun findAllByParentId(parentId: Long?, boardId: Long, pageable: Pageable): Page<Task>
    fun findAllByIdInAndDeletedFalse(ids: MutableList<Long>): MutableList<Task>

    @Query(
        """
        select exists(select u.*
                      from users u
                               join employee e on u.id = e.user_id
                               join project_employee pe on e.id = pe.employee_id
                               join task_project_employee tpe on pe.id = tpe.employees_id
                               join task t on tpe.task_id = t.id
                      where u.id = :userId
                       and t.id = :taskId
                       and u.deleted = false
                       and e.deleted = false
                       and pe.deleted = false
                       and t.deleted = false
                       and e.ph_status = 'BUSY')
    """, nativeQuery = true
    )
    fun existsByProjectEmployeeUser(userId: Long, taskId: Long): Boolean
    fun findAllByStateIdAndDeletedFalseOrderByOrder(stateId: Long): MutableList<Task>
    fun findTopByStateIdAndDeletedFalseOrderByOrderDesc(stateId: Long): Task?
    fun findTopByParentTaskIdAndDeletedFalseOrderByOrderDesc(parentTaskId: Long): Task?
    fun existsByStateIdAndDeletedFalse(stateId: Long): Boolean
    fun findAllByParentTaskIdAndDeletedFalse(parentTaskId: Long): MutableList<Task>

    @Query(
        """
        select exists(select t.*
              from task t
                       join task_project_employee tpe on t.id = tpe.task_id
                       join project_employee pe on tpe.employees_id = pe.id
                       join employee e on pe.employee_id = e.id
                       join board b on t.board_id = b.id
              where e.deleted = false
                and pe.deleted = false
                and t.deleted = false
                and e.ph_status = 'BUSY'
                and e.id = :employeeId
                and b.project_id = :projectId)
    """, nativeQuery = true
    )
    fun existsByProjectIdAndEmployeeIdAndDeletedFalse(projectId: Long, employeeId: Long): Boolean

    @Query("""
    select t.*,
           s.ordered as state_order,
           p.name as project_name
from task t
join task_project_employee pe ON pe.task_id = t.id
join state s on t.state_id = s.id
join board b on t.board_id = b.id
join project p on b.project_id = p.id
where pe.employees_id IN (:projectEmployeeIds)
  and t.deleted = false
  and s.immutable = true
  and s.ordered = (
      select MIN(i_s.ordered) 
      from state i_s
      where i_s.immutable = true
        and i_s.deleted = false
        and i_s.board_id = s.board_id
  ) order by t.id
""", nativeQuery = true)
    fun findOpenedTasks(@Param("projectEmployeeIds") projectEmployeeIds: List<Long>): List<Task>


    @Query("""
    select t.*,
           s.ordered as state_order,
           p.name as project_name
from task t
join task_project_employee pe ON pe.task_id = t.id
join state s on t.state_id = s.id
join board b on t.board_id = b.id
join project p on b.project_id = p.id
where pe.employees_id IN (:projectEmployeeIds)
  and t.deleted = false
  and s.immutable = true
  and s.ordered = (
      select MAX(i_s.ordered) 
      from state i_s
      where i_s.immutable = true
        and i_s.deleted = false
        and i_s.board_id = s.board_id
  ) order by t.id
""", nativeQuery = true)
    fun findClosedTasks(@Param("projectEmployeeIds") projectEmployeeIds: List<Long>): List<Task>


    @Query("""
    select
         t.*,
         p.name as project_name
    from task t
    join task_project_employee pe on pe.task_id = t.id
    join board b on t.board_id = b.id
    join project p on b.project_id = p.id
    where pe.employees_id in (:projectEmployeeIds)
       and t.deleted = false
       and extract(epoch from date_trunc('day', t.end_date)) >= extract(epoch from current_date)
       and extract(epoch from date_trunc('day', t.end_date)) < extract(epoch from (current_date + interval '3 days'))
    order by t.id
""", nativeQuery = true)
    fun findUpcomingTasks(@Param("projectEmployeeIds") projectEmployeeIds: List<Long>): List<Task>

    @Query("""
        select t
from Task t
where t.deleted = false
and t.parentTask.id is null 
and t.board.project.department.organization.id = :organizationId
and t.board.id = :boardId
and ((:stateName is null) or (t.state.name = :stateName))
and ((:priority is null) or (t.priority = :priority))
    """)
    fun findAllForOrgAdmin(boardId: Long, organizationId: Long, stateName: String?, priority: TaskPriority?, pageable: Pageable): Page<Task>

}

@Repository
interface CommentRepository : BaseRepository<Comment> {
    fun findAllByTaskIdAndDeletedFalseOrderByIdDesc(taskId: Long, pageable: Pageable): Page<Comment>
    fun findByIdAndOwnerIdAndDeletedFalse(id: Long, ownerId: Long): Comment?
}

@Repository
interface TaskActionHistoryRepository : BaseRepository<TaskActionHistory> {
    fun findAllByTaskIdAndDeletedFalseOrderByIdDesc(taskId: Long, pageable: Pageable): Page<TaskActionHistory>

    @Query(
        """
        select h
        from TaskActionHistory h
        where h.action = 'ADD_COMMENT'
          and h.comment.deleted = false
          and h.task.deleted = false
          and h.task.id = :taskId
        order by h.id desc 
    """
    )
    fun findAllCommentsActionHistoryByTaskId(taskId: Long, pageable: Pageable): Page<TaskActionHistory>
    fun findByCommentIdAndDeletedFalse(commentId: Long): TaskActionHistory?
    fun findTopByTaskIdAndDeletedFalseOrderById(taskId: Long): TaskActionHistory?
}

@Repository
interface PermissionRepository : BaseRepository<Permission> {
    fun findAllByIdInAndDeletedFalse(ids: MutableSet<Long>): MutableSet<Permission>
}

@Repository
interface UserOrgStoreRepository : BaseRepository<UserOrgStore> {
    fun findByOrganizationIdAndUserIdAndDeletedFalse(orgId: Long, userId: Long): UserOrgStore?
    fun existsByUserIdAndOrganizationIdAndDeletedFalse(userId: Long, orgId: Long): Boolean
    fun findAllByUserIdAndDeletedFalseOrderByIdDesc(userId: Long, pageable: Pageable): Page<UserOrgStore>
    fun findAllByUserAndDeletedFalse(user: User): List<UserOrgStore>
    fun findAllByUserAndRoleAndDeletedFalse(user: User, role: Role): List<UserOrgStore>
    fun findByUserIdAndOrganizationIdAndDeletedFalse(userId: Long, organizationId: Long): UserOrgStore?
    fun findFirstByUserIdAndDeletedFalseOrderById(userId: Long): UserOrgStore?
}

@Repository
interface TourniquetRepository : BaseRepository<Tourniquet> {
    fun findAllByOrganizationIdAndDeletedFalse(organizationId: Long, pageable: Pageable): Page<Tourniquet>
    fun existsByName(name: String): Boolean
    fun findByNameAndDeletedFalse(name: String): Tourniquet?
    fun findAllByOrganizationIdAndDeletedFalse(organizationId: Long): MutableList<Tourniquet>

    @Query(
        """
        select t 
        from Tourniquet t 
        where t.deleted = false 
          and (:orgId is null or t.organization.id = :orgId)
    """
    )
    fun findTourniquetsByDeletedFalseAndFilter(orgId: Long?, pageable: Pageable): Page<Tourniquet>
}

@Repository
interface WorkingDateConfigRepository : BaseRepository<WorkingDateConfig> {
    fun existsByOrganizationIdAndDayAndDeletedFalse(organizationId: Long, day: DayOfWeek): Boolean
    fun findByOrganizationIdAndDayAndDeletedFalse(organizationId: Long, day: DayOfWeek): WorkingDateConfig?
    fun findAllByOrganizationIdAndDeletedFalse(organizationId: Long): List<WorkingDateConfig>
}

@Repository
interface UserEmploymentHistoryRepository : BaseRepository<UserEmploymentHistory> {
    @Query(
        """
        select h  from UserEmploymentHistory h
            where h.user.id = :userId
            order by h.department.id,h.id
    """
    )
    fun findAllByUserIdAndDeletedFalse(userId: Long, pageable: Pageable): Page<UserEmploymentHistory>

    fun findTopByUserIdAndDepartmentOrganizationIdAndDeletedFalseOrderByIdDesc(
        userId: Long,
        organizationId: Long
    ): UserEmploymentHistory?

    fun findTopByUserIdAndDepartmentOrganizationIdAndDeletedFalseOrderByIdAsc(
        userId: Long,
        organizationId: Long
    ): UserEmploymentHistory?
}

interface UserTourniquetRepository : BaseRepository<UserTourniquet> {

    @Query("""
        select count(ut) > 0 from UserTourniquet ut 
        where ut.user.id = :userId 
        and ut.type = :type 
        and date(ut.time) = :date
    """)
    fun existsByUserIdAndTourniquetTypeAndDate(
        @Param("userId") userId: Long,
        @Param("type") type: UserTourniquetType,
        @Param("date") date: LocalDate
    ): Boolean

    fun findTopByUserIdAndTourniquetOrganizationIdAndDeletedFalseAndTimeIsBeforeOrderByTimeDesc(
        userId: Long,
        organizationId: Long,
        time: Date
    ): UserTourniquet?

    @Query(
        """
        select distinct ut.*
        from user_tourniquet ut
                 join users u on ut.user_id = u.id
                 join employee e on u.id = e.user_id
                 join (select user_id,
                              organization_id,
                              max(time) as latest_time
                       from user_tourniquet
                       group by user_id, organization_id) lt
                      on ut.user_id = lt.user_id and ut.time = lt.latest_time
        where e.deleted = false
          and u.deleted = false
          and ut.time < :midnight
          and ut.type = 'IN'
    """, nativeQuery = true
    )
    fun findLastInUserTourniquets(midnight: Date): MutableList<UserTourniquet>

    @Query(
        """
       SELECT CASE
           WHEN EXISTS(SELECT ut.*
                       FROM user_tourniquet ut
                       WHERE ut.user_id = :userId
                         AND ut.organization_id = :organizationId
                         AND age(:targetTime, ut.time) <= interval '1 minute'
                         AND ut.time <= :targetTime
                         AND ut.deleted = FALSE
                       ORDER BY ut.time desc) THEN TRUE
           ELSE FALSE
           END
    """, nativeQuery = true
    )
    fun existsByLastUserTourniquetAtIntervalTime(
        userId: Long,
        organizationId: Long,
        targetTime: Date
    ): Boolean

    @Query(
        """
       SELECT CASE
           WHEN EXISTS(SELECT ut.*
                       FROM user_tourniquet ut
                       WHERE ut.user_id = :userId
                         AND ut.organization_id = :organizationId
                         AND age(:targetTime, ut.time) <= interval '1 minute'
                         AND ut.time <= :targetTime
                         AND ut.deleted = FALSE
                         AND ut.type = :type
                       ORDER BY ut.time desc) THEN TRUE
           ELSE FALSE
           END
    """, nativeQuery = true
    )
    fun existsByLastUserTourniquetAtIntervalTimeByType(
        userId: Long,
        organizationId: Long,
        targetTime: Date,
        type: String
    ): Boolean


    @Query(
        """
        select ut.*
        from user_tourniquet ut
                 join users u on ut.user_id = u.id
                 join employee e on u.id = e.user_id
                 join organization o on e.organization_id = o.id
                 join table_date td on o.id = td.organization_id and ut.table_date_id = td.id
        where o.id = :orgId
          and td.id = :tableDateId
          and e.deleted = false
          and u.deleted = false
          and u.id = :userId
        order by ut.user_id, ut.time
    """, nativeQuery = true
    )
    fun findOrganizationUserDailyEvents(orgId: Long, userId: Long, tableDateId: Long): MutableList<UserTourniquet>

    @Modifying
    @Query(
        """
          update tourniquet_tracker as tr
          set deleted = true
          from table_date as td
                   join organization as o on td.organization_id = o.id
          where tr.table_date_id = td.id
            and tr.user_id = :userId
            and td.organization_id = :orgId
            and tr.table_date_id = :tableDateId
    """, nativeQuery = true
    )
    fun updateOldOrganizationUserDailyEventsDeleted(orgId: Long, userId: Long, tableDateId: Long)

}

@Repository
interface UserTourniquetResultRepository : BaseRepository<UserTourniquetResult> {

    @Query(
        """
        select *
        from user_tourniquet_result utr
        where date_time between :startDate and :endDate
    """, nativeQuery = true
    )
    fun findOrganizationResults(startDate: Date, endDate: Date): MutableList<UserTourniquetResult>
}

@Repository
interface TimeTrackingRepository : BaseRepository<TimeTracking> {
    @Query(
        """
        select t from TimeTracking t
        where (:taskId is null or t.task.id = :taskId)
        and t.deleted = false
        and t.deleted = false
        order by t.id desc 
    """
    )
    fun findAllByTaskId(@Param("taskId") taskId: Long?, pageable: Pageable): Page<TimeTracking>
    fun findByIdAndOwnerIdAndDeletedFalse(id: Long, ownerId: Long): TimeTracking?
    fun findAllByTaskIdAndDeletedFalse(taskId: Long): List<TimeTracking>

    @Query(
        """
        select coalesce(sum(tt.duration),0) 
        from TimeTracking tt
        where tt.owner.id = :userId
          and tt.tableDate.id = :tableDateId
          and tt.deleted = false
          and tt.task.deleted = false
    """
    )
    fun findUserTimeTrackingAmountByDate(userId: Long, tableDateId: Long): Long

    @Query(
        """
        select exists(select *
                      from time_tracking tt
                               join table_date td on tt.table_date_id = td.id
                      where tt.deleted = false
                        and td.date between :startDate and :endDate
                        and tt.task_id = :taskId
                        and ((:startTime > tt.start_time and :startTime < tt.end_time)
                          or (tt.end_time > :startTime and tt.end_time < :endTime)
                          or (tt.start_time > :startTime and tt.start_time < :endTime)
                          or (tt.start_time = :startTime and tt.end_time = :endTime)
                          ))
    """, nativeQuery = true
    )
    fun existsByTaskIdAndDateAndTimeAndDeletedFalse(
        taskId: Long,
        startDate: Date,
        endDate: Date,
        startTime: LocalTime,
        endTime: LocalTime
    ): Boolean

    @Query(
        """
        select exists(select *
                      from time_tracking tt
                               join table_date td on tt.table_date_id = td.id
                      where tt.id != :timeTrackingId
                        and td.date between :startDate and :endDate
                        and tt.task_id = :taskId
                        and tt.deleted = false
                        and ((:startTime > tt.start_time and :startTime < tt.end_time)
                          or (tt.end_time > :startTime and tt.end_time < :endTime)
                          or (tt.start_time > :startTime and tt.start_time < :endTime)
                          or (tt.start_time = :startTime and tt.end_time = :endTime)
                          ))
    """, nativeQuery = true
    )
    fun existsByTaskIdAndDateAndTimeAndDeletedFalse(
        timeTrackingId: Long,
        taskId: Long,
        startDate: Date,
        endDate: Date,
        startTime: LocalTime,
        endTime: LocalTime
    ): Boolean

}

@Repository
interface TableDateRepository : BaseRepository<TableDate> {
    fun existsByDateAndOrganizationIdAndTypeAndDeletedFalse(
        date: Date,
        organizationId: Long,
        type: TableDateType
    ): Boolean

    @Query(
        """
        select td.*
        from table_date td
                 join organization o on td.organization_id = o.id
        where td.deleted = false
          and td.organization_id = :orgId
          and cast(td.date as date)
            between cast(:startDate as date) and cast(:endDate as date)
        order by td.date 
    """, nativeQuery = true
    )
    fun findByOrganizationIdAndDeletedFalsePageable(
        orgId: Long,
        startDate: Date,
        endDate: Date,
        pageable: Pageable
    ): Page<TableDate>

    @Query(
        """
        select td.*
        from table_date td
                 join organization o on td.organization_id = o.id
        where td.deleted = false
          and td.organization_id = :orgId
          and cast(td.date as date)
            between cast(:startDate as date) and cast(:endDate as date)
        order by td.date 
    """, nativeQuery = true
    )
    fun findByOrganizationIdAndDeletedFalse(
        orgId: Long,
        startDate: Date,
        endDate: Date
    ): MutableList<TableDate>

    @Query(
        """
        select td.*
        from   table_date td
               join organization o on td.organization_id = o.id
        where  td.deleted = false
               and td.organization_id = :organizationId
               and cast(td.date as date) = cast(:targetDate as date);
    """, nativeQuery = true
    )
    fun findByDateAndOrganizationIdAndDeletedFalse(targetDate: Date, organizationId: Long): TableDate?

    @Query(
        """
        SELECT EXISTS (select td.*
               from table_date td
                        join organization o on td.organization_id = o.id
               where td.deleted = false
                 and td.organization_id = :organizationId
                 and cast(td.date as date) = cast(:targetDate as date))
    """, nativeQuery = true
    )
    fun existByDateAndOrganizationIdAndDeletedFalse(targetDate: Date, organizationId: Long): Boolean

    fun existsByIdAndOrganizationIdAndDeletedFalse(tableDateId: Long, organizationId: Long): Boolean
}

@Repository
interface StateTemplateRepository : BaseRepository<StateTemplate> {
    @Query("select s from StateTemplate s where s.status = :status and (s.name = 'OPEN' or s.name = 'CLOSED')")
    fun findDefaultTemplatesByStatus(@Param("status") status: Status): List<StateTemplate>
}

@Repository
interface EmployeeTourniquetDataRepository : BaseRepository<EmployeeTourniquetData> {

    fun existsByEmployeeIdAndTourniquetIdAndDeletedFalse(employeeId: Long, tourniquetId: Long): Boolean

    fun findAllByTourniquetIdAndStatusInAndDeletedFalseOrderById(
        tourniquetId: Long,
        status: MutableList<EmployeeStatus>
    ): MutableList<EmployeeTourniquetData>

    fun findByEmployeeIdAndTourniquetIdAndDeletedFalse(employeeId: Long, tourniquetId: Long): EmployeeTourniquetData?
    fun findAllByEmployeeIdAndDeletedFalse(employeeId: Long): MutableList<EmployeeTourniquetData>
    fun findAllByTourniquetIdAndDeletedFalse(tourniquetId: Long): MutableList<EmployeeTourniquetData>
    fun findByStatusInAndDeletedFalse(status: MutableList<EmployeeStatus>): MutableList<EmployeeTourniquetData>
}

@Repository
interface TourniquetTrackerRepository : BaseRepository<TourniquetTracker> {

}

@Repository
interface VisitorRepository : MongoRepository<Visitor, String> {
    fun findByHashIdAndOrganizationId(hashId: String, organizationId: Long): Visitor?
    fun existsByHashIdAndOrganizationId(hashId: String, organizationId: Long): Boolean
    fun deleteByHashId(hashId: String)
}

@Repository
interface UnknownPersonRepository : MongoRepository<UnknownPerson, String> {
    fun findAllByOrganizationId(organizationId: Long, pageable: Pageable): Page<UnknownPerson>
}

@Repository
interface UserOrgSessionRepository : MongoRepository<UserOrgSession, String> {
    fun findFirstByUserIdAndOrganizationIdOrderByCreatedDateDesc(userId: Long, organizationId: Long): UserOrgSession?
}

@Repository
interface TourniquetClientRepository : BaseRepository<TourniquetClient> {
    fun existsByUsername(username: String): Boolean

    @Query(
        """
        select tc.*
        from tourniquet_client tc
        where tc.deleted = false
          and (:search is null or tc.username ilike concat('%', :search, '%'))
          and tc.organization_id = :orgId
    """, nativeQuery = true,
        countQuery = """
            select count(tc.id)
            from tourniquet_client tc
            where tc.deleted = false
              and (:search is null or tc.username ilike concat('%', :search, '%'))
              and tc.organization_id = :orgId
        """
    )
    fun findOrganizationTourniquetClients(search: String?, orgId: Long, pageable: Pageable): Page<TourniquetClient>
    fun findAllByOrganizationIdAndDeletedFalse(organizationId: Long, pageable: Pageable): Page<TourniquetClient>
    fun findByUsernameAndDeletedFalse(username: String): TourniquetClient?
}

@Repository
interface MessageRepository : BaseRepository<Message> {
    fun findByHashIdAndDeletedFalse(hashId: String): Message?

    @Query(
        """
    select count(*)
    from message m
    where m.employee_id = :employeeId
      and m.deleted = false
      and m.created_date > (now() - (interval '1' minute) * :otpMessageIntervalMin)
      and (:botUsername is null or m.bot_username = :botUsername)

    """, nativeQuery = true
    )
    fun getLastMessagesAmount(employeeId: Long, otpMessageIntervalMin: Int, botUsername: String): Int

    @Query(
        """
           select m.*
           from message m
           where m.employee_id = :employeeId
           and m.created_date > (now() - (interval '1' minute) * :otpMessageIntervalMin)
           and m.deleted = false
           and (:botUsername is null or m.bot_username = :botUsername)
           order by m.created_date desc
           limit 1
    """, nativeQuery = true
    )
    fun findLastMessageAtInterval(employeeId: Long, otpMessageIntervalMin: Int, botUsername: String): Message?
}

@Repository
interface SubscriberRepository : BaseRepository<Subscriber> {

    fun findByIdAndUserIdAndDeletedFalse(id: Long, userId: Long): Subscriber?
    fun findByUserIdAndBotUsernameAndDeletedFalse(userId: Long, botUsername: String): Subscriber?
    fun existsByChatIdAndBotUsernameAndDeletedFalse(chatId: String, botUsername: String): Boolean
    fun findByChatIdAndDeletedFalse(chatId: String): Subscriber?
    fun findByChatIdAndBotUsernameAndDeletedFalse(chatId: String, botUsername: String): Subscriber?

}

@Repository
interface TaskSubscriberRepository : BaseRepository<TaskSubscriber> {
    fun existsByTaskIdAndSubscriberIdAndDeletedFalse(taskId: Long, subscriberId: Long): Boolean
    fun findAllBySubscriberIdAndTaskBoardIdAndDeletedFalse(
        subscriberId: Long,
        boardId: Long
    ): MutableList<TaskSubscriber>

    fun findAllBySubscriberIdAndTaskBoardProjectIdAndDeletedFalse(
        subscriberId: Long,
        projectId: Long
    ): MutableList<TaskSubscriber>

    fun findAllByTaskIdAndDeletedFalse(taskId: Long): MutableList<TaskSubscriber>

    @Modifying
    @Query(
        """
        update task_subscriber ts
        set deleted = true
        from task t
                 join task_project_employee tpe on t.id = tpe.task_id
                 join project_employee pe on tpe.employees_id = pe.id
        where t.id = ts.task_id
          and ts.deleted = false
          and ts.is_immutable = false
          and pe.id in (:employeeIds)
          and t.id = :taskId
    """, nativeQuery = true
    )
    fun removeTaskSubscribersByEmployee(employeeIds: List<Long>, taskId: Long)

    fun findBySubscriberIdAndTaskId(subscriberId: Long, taskId: Long): TaskSubscriber?
}

@Repository
interface BoardNotificationSettingsRepository : BaseRepository<BoardNotificationSettings> {
    fun existsByBoardIdAndDeletedFalse(boardId: Long): Boolean
    fun findByBoardIdAndDeletedFalse(boardId: Long): BoardNotificationSettings?
}

@Repository
interface UserAbsenceTrackerRepository : BaseRepository<UserAbsenceTracker> {

    fun existsByIdAndUserIdAndDeletedFalse(trackerId: Long, userId: Long): Boolean

    fun existsByUserIdAndTableDateIdAndDeletedFalse(userId: Long, tableDateId: Long): Boolean
    fun existsByIdIsNotAndUserIdAndTableDateIdAndDeletedFalse(trackerId: Long, userId: Long, tableDateId: Long): Boolean

    fun countByTableDateIdAndDeletedFalse(tableDateId: Long): Int

    fun findAllByTableDateIdAndDeletedFalse(tableDateId: Long?, pageable: Pageable): Page<UserAbsenceTracker>
}

@Repository
interface BotHashIdRepository : BaseRepository<BotHashId> {

    @Query(
        """
    select count(*)
    from bot_hash_id b
    where b.organization_id = :organizationId
      and b.deleted = false
      and b.created_date > (now() - (interval '1' minute) * :otpMessageIntervalMin)
      and (:botUsername is null or b.bot_username = :botUsername)

    """, nativeQuery = true
    )
    fun getLastHashAmount(organizationId: Long, otpMessageIntervalMin: Int, botUsername: String): Int

    @Query(
        """
           select b.*
           from bot_hash_id b
           where b.organization_id = :organizationId
           and b.created_date > (now() - (interval '1' minute) * :otpMessageIntervalMin)
           and b.deleted = false
           and (:botUsername is null or b.bot_username = :botUsername)
           order by b.created_date desc
           limit 1
    """, nativeQuery = true
    )
    fun findLastHashAtInterval(organizationId: Long, otpMessageIntervalMin: Int, botUsername: String): BotHashId?

    fun findByHashIdAndDeletedFalse(hashId: String): BotHashId?

}

