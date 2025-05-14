package itpu.uz.itpuhrms.services

import itpu.uz.itpuhrms.*
import itpu.uz.itpuhrms.DeactivatedUserException
import itpu.uz.itpuhrms.EmployeeNotFoundException
import itpu.uz.itpuhrms.OrganizationNotFoundException
import itpu.uz.itpuhrms.UserSessionNotFoundException
import itpu.uz.itpuhrms.security.getOSession
import itpu.uz.itpuhrms.security.userId
import itpu.uz.itpuhrms.services.employee.EmployeeRepository
import itpu.uz.itpuhrms.services.organization.OrganizationRepository
import itpu.uz.itpuhrms.services.userOrgSession.UserOrgSessionRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service


interface ExtraService {
    fun getOrgFromCurrentUser(): Organization
    fun getEmployeeFromCurrentUser(): Employee
    fun getSessionRole(): Role
}

@Service
class ExtraServiceImpl(
    private val userOrgSessionRepository: UserOrgSessionRepository,
    private val employeeRepository: EmployeeRepository,
    private val organizationRepository: OrganizationRepository
) : ExtraService {

    override fun getOrgFromCurrentUser(): Organization {
        val sessionId = getOSession()
        val userOrgSession = userOrgSessionRepository.findByIdOrNull(sessionId)
            ?: throw UserSessionNotFoundException()
        if (userOrgSession.userId != userId())
            throw UserSessionNotFoundException()

        return organizationRepository.findByIdAndDeletedFalse(userOrgSession.organizationId)
            ?: throw OrganizationNotFoundException()
    }


    override fun getEmployeeFromCurrentUser(): Employee {
        val sessionId = getOSession()
        val userOrgSession = userOrgSessionRepository.findByIdOrNull(sessionId)
            ?: throw UserSessionNotFoundException()
        if (userOrgSession.userId != userId())
            throw UserSessionNotFoundException()

        return employeeRepository.findByUserIdAndOrganizationIdAndDeletedFalse(
            userOrgSession.userId,
            userOrgSession.organizationId
        )?.let {
            if (it.status == Status.DEACTIVATED) throw DeactivatedUserException()
            it
        } ?: throw EmployeeNotFoundException()
    }

    override fun getSessionRole(): Role {
        val sessionId = getOSession()
        val userOrgSession = userOrgSessionRepository.findByIdOrNull(sessionId)
            ?: throw UserSessionNotFoundException()
        if (userOrgSession.userId != userId())
            throw UserSessionNotFoundException()
        return userOrgSession.role
    }
}