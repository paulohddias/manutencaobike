package br.com.viavalesistemas.manutencaobike.model

data class TipoServico(
    val id: Long = 0,
    val nome: String,
    val intervaloKm: Int? = null,
    val intervaloMeses: Int? = null,
) {
    val temIntervalo: Boolean get() = (intervaloKm ?: 0) > 0 || (intervaloMeses ?: 0) > 0
}
