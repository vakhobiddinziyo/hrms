package itpu.uz.itpuhrms.services.stateTemplate

import itpu.uz.itpuhrms.StateTemplate
import itpu.uz.itpuhrms.Status

data class StateTemplateResponse(
    val id: Long,
    val name: String,
    val status: Status
) {
    companion object {
        fun toDto(stateTemplate: StateTemplate) = StateTemplateResponse(
            stateTemplate.id!!,
            stateTemplate.name,
            stateTemplate.status
        )
    }
}
