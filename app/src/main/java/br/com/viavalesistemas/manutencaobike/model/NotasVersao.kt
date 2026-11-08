package br.com.viavalesistemas.manutencaobike.model

import kotlinx.serialization.Serializable

/** Uma entrada de assets/novidades.json. [codigo] é o versionCode da versão. */
@Serializable
data class NotasVersao(
    val versao: String,
    val codigo: Int,
    val data: String,
    /** Descrição completa da versão (até [LIMITE_DESCRICAO] caracteres), mostrada antes das listas. */
    val descricao: String? = null,
    val novidades: List<String> = emptyList(),
    val correcoes: List<String> = emptyList(),
    val melhorias: List<String> = emptyList(),
) {
    companion object {
        const val LIMITE_DESCRICAO = 400
    }

    val vazia: Boolean get() = descricao.isNullOrBlank() && novidades.isEmpty() && correcoes.isEmpty() && melhorias.isEmpty()
}
