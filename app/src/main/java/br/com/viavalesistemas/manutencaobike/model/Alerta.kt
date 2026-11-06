package br.com.viavalesistemas.manutencaobike.model

import java.time.LocalDate

enum class OrigemAlerta { PECA, SERVICO }

data class Alerta(
    /** Identifica o alerta entre execuções, para não notificar o mesmo item duas vezes. */
    val chave: String,
    val origem: OrigemAlerta,
    val bikeId: Long,
    val bikeNome: String,
    val item: String,
    val status: StatusDesgaste,
    val fracao: Double,
)

fun alertasDaBike(
    bike: Bike,
    pecasDaBike: List<Peca>,
    servicosDaBike: List<Servico>,
    tiposServico: List<TipoServico>,
    hoje: LocalDate,
): List<Alerta> {
    val alertasPecas = pecasDaBike
        .filter { it.instalada }
        .map { it.desgaste(bike.kmAtual) }
        .filter { it.status != StatusDesgaste.OK }
        .map {
            Alerta(
                chave = "peca:${it.peca.id}:${it.status}",
                origem = OrigemAlerta.PECA,
                bikeId = bike.id,
                bikeNome = bike.nome,
                item = it.peca.tipoNome,
                status = it.status,
                fracao = it.fracao,
            )
        }

    val alertasServicos = tiposServico
        .filter { it.temIntervalo }
        .map { situacaoServico(it, servicosDaBike, bike.kmAtual, hoje) }
        .filter { it.status != StatusDesgaste.OK && it.fracao != null }
        .map {
            Alerta(
                chave = "servico:${bike.id}:${it.tipo.id}:${it.ultimo?.id}:${it.status}",
                origem = OrigemAlerta.SERVICO,
                bikeId = bike.id,
                bikeNome = bike.nome,
                item = it.tipo.nome,
                status = it.status,
                fracao = it.fracao ?: 0.0,
            )
        }

    return alertasPecas + alertasServicos
}
