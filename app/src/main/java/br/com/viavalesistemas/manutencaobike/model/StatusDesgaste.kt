package br.com.viavalesistemas.manutencaobike.model

enum class StatusDesgaste { OK, ATENCAO, TROCAR }

const val LIMITE_ATENCAO = 0.8

fun statusPorFracao(fracao: Double): StatusDesgaste = when {
    fracao >= 1.0 -> StatusDesgaste.TROCAR
    fracao >= LIMITE_ATENCAO -> StatusDesgaste.ATENCAO
    else -> StatusDesgaste.OK
}
