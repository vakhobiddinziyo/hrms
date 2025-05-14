package itpu.uz.itpuhrms.services.organization

import itpu.uz.itpuhrms.Organization
import itpu.uz.itpuhrms.Status
import itpu.uz.itpuhrms.base.BaseRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

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