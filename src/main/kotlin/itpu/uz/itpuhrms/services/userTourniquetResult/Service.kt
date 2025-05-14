package itpu.uz.itpuhrms.services.userTourniquetResult

import org.springframework.stereotype.Service
import java.util.*

interface UserTourniquetResultService {
    fun getOrganizationResult(starDate: Long, endDate: Long): List<UserTourniquetResultResponse>
}

@Service
class UserTourniquetResultServiceImpl(
    private val resultRepository: UserTourniquetResultRepository,
) : UserTourniquetResultService {
    override fun getOrganizationResult(starDate: Long, endDate: Long) =
        resultRepository.findOrganizationResults(Date(starDate), Date(endDate)).map {
            UserTourniquetResultResponse.toResponse(it)
        }
}