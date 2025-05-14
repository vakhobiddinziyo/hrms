package itpu.uz.itpuhrms.services.tableDate

import itpu.uz.itpuhrms.TableDate
import itpu.uz.itpuhrms.TableDateType
import itpu.uz.itpuhrms.base.BaseRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

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