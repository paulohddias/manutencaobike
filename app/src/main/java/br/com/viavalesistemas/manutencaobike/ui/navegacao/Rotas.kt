package br.com.viavalesistemas.manutencaobike.ui.navegacao

import kotlinx.serialization.Serializable

@Serializable
data object RotaBikes

/** bikeId = 0 cadastra uma bike nova. */
@Serializable
data class RotaBikeForm(val bikeId: Long = 0L)

@Serializable
data class RotaBikeDetalhe(val bikeId: Long)

/** tipoPecaId = 0 deixa o usuário escolher o tipo. */
@Serializable
data class RotaPecaForm(val bikeId: Long, val tipoPecaId: Long = 0L)

@Serializable
data class RotaServicoForm(val bikeId: Long)

@Serializable
data object RotaCatalogo

@Serializable
data object RotaBackup
