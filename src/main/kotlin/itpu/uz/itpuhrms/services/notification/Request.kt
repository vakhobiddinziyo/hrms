package itpu.uz.itpuhrms.services.notification

import itpu.uz.itpuhrms.TaskAction


data class BoardSettingsRequest(
    val boardId: Long,
    val actions: MutableSet<TaskAction>
)

data class BoardSettingsUpdateRequest(
    val actions: MutableSet<TaskAction>
)
