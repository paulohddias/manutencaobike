package br.com.viavalesistemas.manutencaobike.model

data class TipoPeca(
    val id: Long = 0,
    val nome: String,
    val vidaUtilKmPadrao: Int? = null,
)
