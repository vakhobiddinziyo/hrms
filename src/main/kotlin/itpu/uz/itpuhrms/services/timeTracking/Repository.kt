package itpu.uz.itpuhrms.services.timeTracking

import itpu.uz.itpuhrms.TimeTracking
import itpu.uz.itpuhrms.base.BaseRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalTime
import java.util.*

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