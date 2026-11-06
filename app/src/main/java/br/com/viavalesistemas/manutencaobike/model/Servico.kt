package br.com.viavalesistemas.manutencaobike.model

import java.time.LocalDate

data class Servico(
    val id: Long = 0,
    val bikeId: Long,
    val tipoServicoId: Long,
    val tipoNome: String,
    val data: LocalDate,
    val km: Double,
    val custoCentavos: Long?,
    val observacoes: String?,
)
