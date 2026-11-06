package br.com.viavalesistemas.manutencaobike.model

data class Bike(
    val id: Long = 0,
    val nome: String,
    val marca: String? = null,
    val modelo: String? = null,
    val kmAtual: Double,
    val stravaGearId: String? = null,
    val arquivada: Boolean = false,
) {
    val doStrava: Boolean get() = stravaGearId != null
    val descricao: String? get() = listOfNotNull(marca, modelo).joinToString(" ").ifBlank { null }
}
