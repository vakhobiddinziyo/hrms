package itpu.uz.itpuhrms.services.userTourniquetResult

import itpu.uz.itpuhrms.UserTourniquetResult
import itpu.uz.itpuhrms.base.BaseRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

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