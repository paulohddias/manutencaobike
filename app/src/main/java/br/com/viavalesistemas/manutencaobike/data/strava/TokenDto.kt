package br.com.viavalesistemas.manutencaobike.data.strava

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TokenDto(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_at") val expiresAt: Long,
    val athlete: AtletaResumoDto? = null,
)

@Serializable
data class AtletaResumoDto(
    val firstname: String? = null,
    val lastname: String? = null,
)
