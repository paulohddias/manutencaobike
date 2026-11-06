package br.com.viavalesistemas.manutencaobike.ui.catalogo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.viavalesistemas.manutencaobike.data.repositorio.CatalogoRepository
import br.com.viavalesistemas.manutencaobike.model.TipoPeca
import br.com.viavalesistemas.manutencaobike.model.TipoServico
import br.com.viavalesistemas.manutencaobike.ui.mensagemParaUsuario
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CatalogoUiState(
    val tiposPeca: List<TipoPeca> = emptyList(),
    val tiposServico: List<TipoServico> = emptyList(),
)

@HiltViewModel
class CatalogoViewModel @Inject constructor(
    private val catalogoRepository: CatalogoRepository,
) : ViewModel() {

    val uiState: StateFlow<CatalogoUiState> = combine(
        catalogoRepository.observarTiposPeca(),
        catalogoRepository.observarTiposServico(),
        ::CatalogoUiState,
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CatalogoUiState())

    private val _mensagens = Channel<String>(Channel.BUFFERED)
    val mensagens = _mensagens.receiveAsFlow()

    fun onSalvarTipoPeca(tipo: TipoPeca) = executar { catalogoRepository.salvarTipoPeca(tipo) }
    fun onExcluirTipoPeca(tipo: TipoPeca) = executar { catalogoRepository.excluirTipoPeca(tipo.id) }
    fun onSalvarTipoServico(tipo: TipoServico) = executar { catalogoRepository.salvarTipoServico(tipo) }
    fun onExcluirTipoServico(tipo: TipoServico) = executar { catalogoRepository.excluirTipoServico(tipo.id) }

    private fun executar(acao: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            acao().onFailure { _mensagens.send(it.mensagemParaUsuario()) }
        }
    }
}
