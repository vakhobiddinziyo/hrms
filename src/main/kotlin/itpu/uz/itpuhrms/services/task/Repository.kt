package itpu.uz.itpuhrms.services.task

import itpu.uz.itpuhrms.Task
import itpu.uz.itpuhrms.TaskPriority
import itpu.uz.itpuhrms.TaskSubscriber
import itpu.uz.itpuhrms.base.BaseRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

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
