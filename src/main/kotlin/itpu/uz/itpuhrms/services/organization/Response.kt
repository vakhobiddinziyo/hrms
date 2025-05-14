package itpu.uz.itpuhrms.services.organization

import itpu.uz.itpuhrms.Organization
import itpu.uz.itpuhrms.Status
import itpu.uz.itpuhrms.UserOrgStore

data class OrgAdminResponse(
    var id: Long,
    val name: String,
    val description: String?,
    val status: Status,
    val tin: String,
    val isActive: Boolean,
    val granted: Boolean?
) {
    companion object {
        fun toDto(org: Organization, store: UserOrgStore? = null): OrgAdminResponse {
            return org.let {
                OrgAdminResponse(
                    it.id!!,
                    it.name,
                    it.description,
                    it.status,
                    it.tin,
                    it.isActive,
                    store?.granted
                )
            }
        }
    }
}

interface StatisticResponse {
    val departmentAmount: Long
    val positionAmount: Long
    val totalEmployeeAmount: Long
    val busyEmployeeAmount: Long
    val vacantEmployeeAmount: Long
    val maleEmployeesAmount: Long
    val femaleEmployeesAmount: Long
}
