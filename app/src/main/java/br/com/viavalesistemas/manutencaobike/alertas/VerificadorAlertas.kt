package br.com.viavalesistemas.manutencaobike.alertas

import br.com.viavalesistemas.manutencaobike.data.repositorio.BikeRepository
import br.com.viavalesistemas.manutencaobike.data.repositorio.CatalogoRepository
import br.com.viavalesistemas.manutencaobike.data.repositorio.PecaRepository
import br.com.viavalesistemas.manutencaobike.data.repositorio.ServicoRepository
import br.com.viavalesistemas.manutencaobike.model.Alerta
import br.com.viavalesistemas.manutencaobike.model.alertasDaBike
import java.time.LocalDate
import javax.inject.Inject

class VerificadorAlertas @Inject constructor(
    private val bikeRepository: BikeRepository,
    private val pecaRepository: PecaRepository,
    private val servicoRepository: ServicoRepository,
    private val catalogoRepository: CatalogoRepository,
    private val notificados: AlertasNotificadosStore,
) {
    /**
     * Devolve só os alertas que ainda não foram notificados. Alertas resolvidos saem do conjunto
     * salvo, então voltam a ser notificados se reaparecerem (ex.: a peça nova também gastar).
     */
    suspend fun alertasNovos(hoje: LocalDate = LocalDate.now()): List<Alerta> {
        val tiposServico = catalogoRepository.listarTiposServico()
        val pecasPorBike = pecaRepository.listarInstaladas().groupBy { it.bikeId }
        val servicosPorBike = servicoRepository.listarTodos().groupBy { it.bikeId }

        val atuais = bikeRepository.listarAtivas().flatMap { bike ->
            alertasDaBike(bike, pecasPorBike[bike.id].orEmpty(), servicosPorBike[bike.id].orEmpty(), tiposServico, hoje)
        }

        val jaNotificados = notificados.chaves()
        notificados.substituir(atuais.mapTo(mutableSetOf()) { it.chave })
        return atuais.filter { it.chave !in jaNotificados }
    }
}
