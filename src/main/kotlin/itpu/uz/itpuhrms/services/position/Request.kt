package itpu.uz.itpuhrms.services.position

import itpu.uz.itpuhrms.Level


data class PositionRequest(
    val name: String,
    var level: Level,
    val permissions: MutableSet<Long>
)

data class PositionAdminRequest(
    val name: String,
    val level: Level,
    val permission: MutableSet<Long>,
    val organizationId: Long
)
