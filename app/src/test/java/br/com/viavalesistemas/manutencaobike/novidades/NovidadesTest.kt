package br.com.viavalesistemas.manutencaobike.novidades

import br.com.viavalesistemas.manutencaobike.BuildConfig
import br.com.viavalesistemas.manutencaobike.data.novidades.NovidadesRepository
import br.com.viavalesistemas.manutencaobike.model.NotasVersao
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate

/** Impede gerar uma versão sem as notas dela em assets/novidades.json. */
class NovidadesTest {

    // Sem ignoreUnknownKeys: um campo com nome errado no JSON deve falhar aqui, não sumir em silêncio.
    private val notas = NovidadesRepository.lerNotas(
        Json,
        File("src/main/assets/${NovidadesRepository.ARQUIVO}").readText(),
    )

    @Test
    fun `versao atual tem notas`() {
        val atual = notas.find { it.codigo == BuildConfig.VERSION_CODE }
        assertNotNull("Adicione a versão ${BuildConfig.VERSION_CODE} em novidades.json", atual)
        assertEquals("versao no novidades.json difere do versionName", BuildConfig.VERSION_NAME, atual!!.versao)
        assertFalse("As notas da versão atual estão vazias", atual.vazia)
    }

    @Test
    fun `descricoes tem no maximo 400 caracteres`() {
        notas.forEach { versao ->
            val tamanho = versao.descricao?.length ?: 0
            assertTrue(
                "Descrição da versão ${versao.versao} tem $tamanho caracteres (máximo ${NotasVersao.LIMITE_DESCRICAO})",
                tamanho <= NotasVersao.LIMITE_DESCRICAO,
            )
        }
    }

    @Test
    fun `codigos sao unicos e datas validas`() {
        assertEquals(notas.size, notas.map { it.codigo }.toSet().size)
        notas.forEach { LocalDate.parse(it.data) }
    }
}
