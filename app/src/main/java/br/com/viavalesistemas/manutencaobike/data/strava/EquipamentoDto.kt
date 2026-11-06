package br.com.viavalesistemas.manutencaobike.data.strava

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EquipamentoDto(
    val id: String,
    val name: String? = null,
    @SerialName("brand_name") val brandName: String? = null,
    @SerialName("model_name") val modelName: String? = null,
    /** Em metros. */
    val distance: Double = 0.0,
    val retired: Boolean = false,
)
