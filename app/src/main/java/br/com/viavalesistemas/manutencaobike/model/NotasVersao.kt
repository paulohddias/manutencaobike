package br.com.viavalesistemas.manutencaobike.model

import kotlinx.serialization.Serializable

/** Uma entrada de assets/novidades.json. [codigo] é o versionCode da versão. */
@Serializable
data class NotasVersao(
    val versao: String,
    val codigo: Int,
    val data: String,
    val novidades: List<String> = emptyList(),
    val correcoes: List<String> = emptyList(),
    val melhorias: List<String> = emptyList(),
) {
    val vazia: Boolean get() = novidades.isEmpty() && correcoes.isEmpty() && melhorias.isEmpty()
}
