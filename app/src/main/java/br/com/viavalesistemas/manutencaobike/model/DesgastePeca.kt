package br.com.viavalesistemas.manutencaobike.model

data class DesgastePeca(val peca: Peca, val kmRodados: Double) {
    val fracao: Double get() = if (peca.vidaUtilKm > 0) kmRodados / peca.vidaUtilKm else 0.0
    val kmRestantes: Double get() = peca.vidaUtilKm - kmRodados
    val status: StatusDesgaste get() = statusPorFracao(fracao)
}

fun Peca.desgaste(kmAtualBike: Double) = DesgastePeca(this, kmRodados(kmAtualBike))
