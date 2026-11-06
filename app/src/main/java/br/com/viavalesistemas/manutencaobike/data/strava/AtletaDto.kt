package br.com.viavalesistemas.manutencaobike.data.strava

import kotlinx.serialization.Serializable

@Serializable
data class AtletaDto(
    val firstname: String? = null,
    val lastname: String? = null,
    val bikes: List<EquipamentoResumoDto> = emptyList(),
)

@Serializable
data class EquipamentoResumoDto(
    val id: String,
    val name: String? = null,
    /** Em metros. */
    val distance: Double = 0.0,
    val retired: Boolean = false,
)
