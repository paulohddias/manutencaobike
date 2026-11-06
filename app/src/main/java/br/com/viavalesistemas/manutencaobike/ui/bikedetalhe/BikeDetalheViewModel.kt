package br.com.viavalesistemas.manutencaobike.ui.bikedetalhe

import androidx.compose.runtime.Stable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import br.com.viavalesistemas.manutencaobike.data.repositorio.BikeRepository
import br.com.viavalesistemas.manutencaobike.data.repositorio.CatalogoRepository
import br.com.viavalesistemas.manutencaobike.data.repositorio.PecaRepository
import br.com.viavalesistemas.manutencaobike.data.repositorio.ServicoRepository
import br.com.viavalesistemas.manutencaobike.model.Bike
import br.com.viavalesistemas.manutencaobike.model.DesgastePeca
import br.com.viavalesistemas.manutencaobike.model.Peca
import br.com.viavalesistemas.manutencaobike.model.Servico
import br.com.viavalesistemas.manutencaobike.model.SituacaoServico
import br.com.viavalesistemas.manutencaobike.model.desgaste
import br.com.viavalesistemas.manutencaobike.model.situacaoServico
import br.com.viavalesistemas.manutencaobike.ui.mensagemParaUsuario
import br.com.viavalesistemas.manutencaobike.ui.navegacao.RotaBikeDetalhe
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import javax.inject.Inject

data class BikeDetalheUiState(
    val carregando: Boolean = true,
    val bike: Bike? = null,
    val foto: File? = null,
    val pecasInstaladas: List<DesgastePeca> = emptyList(),
    val pecasRemovidas: List<DesgastePeca> = emptyList(),
    val situacoesServico: List<SituacaoServico> = emptyList(),
    val servicos: List<Servico> = emptyList(),
) {
    val custoPecas: Long get() = (pecasInstaladas + pecasRemovidas).sumOf { it.peca.custoCentavos ?: 0L }
    val custoServicos: Long get() = servicos.sumOf { it.custoCentavos ?: 0L }
    val custoTotal: Long get() = custoPecas + custoServicos
}

sealed interface BikeDetalheEfeito {
    data object Voltar : BikeDetalheEfeito
    data class InstalarPeca(val bikeId: Long, val tipoPecaId: Long) : BikeDetalheEfeito
    data class Mensagem(val texto: String) : BikeDetalheEfeito
}

@Stable
interface BikeDetalheActions {
    fun onTrocarPeca(peca: Peca)
    fun onRemoverPeca(peca: Peca)
    fun onExcluirPeca(peca: Peca)
    fun onExcluirServico(servico: Servico)
    fun onAtualizarKm(km: Int)
    fun onExcluirBike()
    fun onAlternarAtiva()
}

@HiltViewModel
class BikeDetalheViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val bikeRepository: BikeRepository,
    private val pecaRepository: PecaRepository,
    private val servicoRepository: ServicoRepository,
    catalogoRepository: CatalogoRepository,
) : ViewModel(), BikeDetalheActions {

    val bikeId = savedStateHandle.toRoute<RotaBikeDetalhe>().bikeId

    val uiState: StateFlow<BikeDetalheUiState> = combine(
        bikeRepository.observarBike(bikeId),
        pecaRepository.observarDaBike(bikeId),
        servicoRepository.observarDaBike(bikeId),
        catalogoRepository.observarTiposServico(),
    ) { bike, pecas, servicos, tiposServico ->
        if (bike == null) return@combine BikeDetalheUiState(carregando = false)
        val (instaladas, removidas) = pecas.map { it.desgaste(bike.kmAtual) }.partition { it.peca.instalada }
        val hoje = LocalDate.now()
        BikeDetalheUiState(
            carregando = false,
            bike = bike,
            foto = bikeRepository.arquivoDaFoto(bike),
            pecasInstaladas = instaladas.sortedByDescending { it.fracao },
            pecasRemovidas = removidas.sortedByDescending { it.peca.dataRemocao },
            situacoesServico = tiposServico
                .filter { it.temIntervalo }
                .map { situacaoServico(it, servicos, bike.kmAtual, hoje) }
                .sortedWith(compareByDescending<SituacaoServico> { it.ultimo != null }.thenByDescending { it.fracao }),
            servicos = servicos,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BikeDetalheUiState())

    private val _efeitos = Channel<BikeDetalheEfeito>(Channel.BUFFERED)
    val efeitos = _efeitos.receiveAsFlow()

    private val kmAtual: Double get() = uiState.value.bike?.kmAtual ?: 0.0

    override fun onTrocarPeca(peca: Peca) {
        viewModelScope.launch {
            pecaRepository.remover(peca.id, kmAtual, LocalDate.now())
                .onSuccess { _efeitos.send(BikeDetalheEfeito.InstalarPeca(bikeId, peca.tipoPecaId)) }
                .onFailure { mensagem(it) }
        }
    }

    override fun onRemoverPeca(peca: Peca) {
        viewModelScope.launch {
            pecaRepository.remover(peca.id, kmAtual, LocalDate.now()).onFailure { mensagem(it) }
        }
    }

    override fun onExcluirPeca(peca: Peca) {
        viewModelScope.launch { pecaRepository.excluir(peca.id).onFailure { mensagem(it) } }
    }

    override fun onExcluirServico(servico: Servico) {
        viewModelScope.launch { servicoRepository.excluir(servico.id).onFailure { mensagem(it) } }
    }

    override fun onAtualizarKm(km: Int) {
        viewModelScope.launch { bikeRepository.atualizarKm(bikeId, km.toDouble()).onFailure { mensagem(it) } }
    }

    override fun onExcluirBike() {
        viewModelScope.launch {
            bikeRepository.excluir(bikeId)
                .onSuccess { _efeitos.send(BikeDetalheEfeito.Voltar) }
                .onFailure { mensagem(it) }
        }
    }

    override fun onAlternarAtiva() {
        val bike = uiState.value.bike ?: return
        viewModelScope.launch {
            bikeRepository.definirAtiva(bikeId, !bike.ativa)
                .onSuccess {
                    val texto = if (bike.ativa) "Bike inativada. Ela não gera mais alertas." else "Bike reativada."
                    _efeitos.send(BikeDetalheEfeito.Mensagem(texto))
                }
                .onFailure { mensagem(it) }
        }
    }

    private suspend fun mensagem(erro: Throwable) {
        _efeitos.send(BikeDetalheEfeito.Mensagem(erro.mensagemParaUsuario()))
    }
}
