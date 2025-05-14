package itpu.uz.itpuhrms.services.tourniquetClient

import itpu.uz.itpuhrms.TourniquetClient


data class ClientResponse(
    val id: Long,
    val username: String
) {
    companion object {
        fun toResponse(client: TourniquetClient) = client.run {
            ClientResponse(
                id!!,
                username
            )
        }
    }
}
