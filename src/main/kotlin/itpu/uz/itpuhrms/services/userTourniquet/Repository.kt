package itpu.uz.itpuhrms.services.userTourniquet

import itpu.uz.itpuhrms.*
import itpu.uz.itpuhrms.base.BaseRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.*

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


