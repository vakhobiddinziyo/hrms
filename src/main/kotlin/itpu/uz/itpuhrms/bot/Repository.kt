package itpu.uz.itpuhrms.bot

import itpu.uz.itpuhrms.BotHashId
import itpu.uz.itpuhrms.Message
import itpu.uz.itpuhrms.base.BaseRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository


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