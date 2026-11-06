package br.com.viavalesistemas.manutencaobike.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DesgasteTest {

    private val hoje = LocalDate.of(2026, 11, 6)

    private fun corrente(kmInstalacao: Double, vidaUtil: Int = 3_000, kmRemocao: Double? = null) = Peca(
        id = 1, bikeId = 1, tipoPecaId = 1, tipoNome = "Corrente", descricao = null,
        kmInstalacao = kmInstalacao, vidaUtilKm = vidaUtil, dataInstalacao = hoje,
        custoCentavos = null, kmRemocao = kmRemocao,
    )

    private val revisao = TipoServico(id = 7, nome = "Revisão geral", intervaloKm = 2_000, intervaloMeses = 6)

    private fun servico(data: LocalDate, km: Double, id: Long = 1) =
        Servico(id, bikeId = 1, tipoServicoId = revisao.id, tipoNome = revisao.nome, data = data, km = km, custoCentavos = null, observacoes = null)

    @Test
    fun `peca abaixo de 80 por cento esta OK`() {
        val desgaste = corrente(kmInstalacao = 1_000.0).desgaste(kmAtualBike = 3_000.0)
        assertEquals(2_000.0, desgaste.kmRodados, 0.001)
        assertEquals(StatusDesgaste.OK, desgaste.status)
        assertEquals(1_000.0, desgaste.kmRestantes, 0.001)
    }

    @Test
    fun `peca a partir de 80 por cento pede atencao`() {
        assertEquals(StatusDesgaste.ATENCAO, corrente(0.0).desgaste(2_400.0).status)
    }

    @Test
    fun `peca no limite precisa ser trocada`() {
        val desgaste = corrente(0.0).desgaste(3_100.0)
        assertEquals(StatusDesgaste.TROCAR, desgaste.status)
        assertTrue(desgaste.kmRestantes < 0)
    }

    @Test
    fun `peca removida para de contar km`() {
        val desgaste = corrente(kmInstalacao = 1_000.0, kmRemocao = 3_500.0).desgaste(kmAtualBike = 9_000.0)
        assertEquals(2_500.0, desgaste.kmRodados, 0.001)
    }

    @Test
    fun `servico nunca feito nao gera alerta`() {
        val situacao = situacaoServico(revisao, emptyList(), kmAtualBike = 5_000.0, hoje = hoje)
        assertNull(situacao.fracao)
        assertEquals(StatusDesgaste.OK, situacao.status)
    }

    @Test
    fun `servico vence por km`() {
        val situacao = situacaoServico(revisao, listOf(servico(hoje.minusMonths(1), 1_000.0)), kmAtualBike = 3_100.0, hoje = hoje)
        assertEquals(3_000.0, situacao.proximoKm!!, 0.001)
        assertEquals(StatusDesgaste.TROCAR, situacao.status)
    }

    @Test
    fun `servico vence por tempo mesmo sem rodar`() {
        val situacao = situacaoServico(revisao, listOf(servico(hoje.minusMonths(7), 1_000.0)), kmAtualBike = 1_100.0, hoje = hoje)
        assertEquals(StatusDesgaste.TROCAR, situacao.status)
    }

    @Test
    fun `usa o servico mais recente`() {
        val servicos = listOf(servico(hoje.minusMonths(10), 0.0, id = 1), servico(hoje.minusDays(10), 2_900.0, id = 2))
        val situacao = situacaoServico(revisao, servicos, kmAtualBike = 3_000.0, hoje = hoje)
        assertEquals(2L, situacao.ultimo?.id)
        assertEquals(StatusDesgaste.OK, situacao.status)
    }

    @Test
    fun `alertas da bike incluem pecas e servicos vencidos`() {
        val bike = Bike(id = 1, nome = "Speed", kmAtual = 3_100.0)
        val alertas = alertasDaBike(
            bike = bike,
            pecasDaBike = listOf(corrente(0.0)),
            servicosDaBike = listOf(servico(hoje.minusMonths(1), 1_000.0)),
            tiposServico = listOf(revisao),
            hoje = hoje,
        )
        assertEquals(setOf(OrigemAlerta.PECA, OrigemAlerta.SERVICO), alertas.map { it.origem }.toSet())
        assertTrue(alertas.all { it.status == StatusDesgaste.TROCAR })
    }
}
