package itpu.uz.itpuhrms.services.tourniquetEmployee

import itpu.uz.itpuhrms.EmployeeStatus
import itpu.uz.itpuhrms.EmployeeTourniquetData
import itpu.uz.itpuhrms.base.BaseRepository
import org.springframework.stereotype.Repository


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