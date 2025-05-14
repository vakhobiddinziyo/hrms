package itpu.uz.itpuhrms.services.employee

import itpu.uz.itpuhrms.Employee
import itpu.uz.itpuhrms.Position
import itpu.uz.itpuhrms.PositionHolderStatus
import itpu.uz.itpuhrms.Status
import itpu.uz.itpuhrms.base.BaseRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*


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