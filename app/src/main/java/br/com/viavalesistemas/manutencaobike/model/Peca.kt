package br.com.viavalesistemas.manutencaobike.model

import java.time.LocalDate

data class Peca(
    val id: Long = 0,
    val bikeId: Long,
    val tipoPecaId: Long,
    val tipoNome: String,
    val descricao: String?,
    val kmInstalacao: Double,
    val vidaUtilKm: Int,
    val dataInstalacao: LocalDate,
    val custoCentavos: Long?,
    val kmRemocao: Double? = null,
    val dataRemocao: LocalDate? = null,
) {
    val instalada: Boolean get() = kmRemocao == null

    fun kmRodados(kmAtualBike: Double): Double =
        ((kmRemocao ?: kmAtualBike) - kmInstalacao).coerceAtLeast(0.0)
}
