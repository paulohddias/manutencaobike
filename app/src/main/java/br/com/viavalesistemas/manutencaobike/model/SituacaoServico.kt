package br.com.viavalesistemas.manutencaobike.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class SituacaoServico(
    val tipo: TipoServico,
    val ultimo: Servico?,
    val proximoKm: Double?,
    val proximaData: LocalDate?,
    /** O maior entre o progresso por km e por tempo; null quando nunca foi feito. */
    val fracao: Double?,
) {
    val status: StatusDesgaste get() = fracao?.let(::statusPorFracao) ?: StatusDesgaste.OK
}

fun situacaoServico(
    tipo: TipoServico,
    servicosDaBike: List<Servico>,
    kmAtualBike: Double,
    hoje: LocalDate,
): SituacaoServico {
    val ultimo = servicosDaBike
        .filter { it.tipoServicoId == tipo.id }
        .maxWithOrNull(compareBy<Servico>({ it.data }, { it.km }))
        ?: return SituacaoServico(tipo, null, null, null, null)

    val intervaloKm = tipo.intervaloKm?.takeIf { it > 0 }
    val proximoKm = intervaloKm?.let { ultimo.km + it }
    val fracaoKm = intervaloKm?.let { (kmAtualBike - ultimo.km).coerceAtLeast(0.0) / it }

    val proximaData = tipo.intervaloMeses?.takeIf { it > 0 }?.let { ultimo.data.plusMonths(it.toLong()) }
    val fracaoTempo = proximaData?.let {
        val diasIntervalo = ChronoUnit.DAYS.between(ultimo.data, it).coerceAtLeast(1)
        ChronoUnit.DAYS.between(ultimo.data, hoje).coerceAtLeast(0).toDouble() / diasIntervalo
    }

    return SituacaoServico(
        tipo = tipo,
        ultimo = ultimo,
        proximoKm = proximoKm,
        proximaData = proximaData,
        fracao = listOfNotNull(fracaoKm, fracaoTempo).maxOrNull(),
    )
}
