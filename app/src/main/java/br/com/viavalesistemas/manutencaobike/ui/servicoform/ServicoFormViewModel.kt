package br.com.viavalesistemas.manutencaobike.ui.servicoform

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import br.com.viavalesistemas.manutencaobike.data.repositorio.BikeRepository
import br.com.viavalesistemas.manutencaobike.data.repositorio.CatalogoRepository
import br.com.viavalesistemas.manutencaobike.data.repositorio.ServicoRepository
import br.com.viavalesistemas.manutencaobike.model.Servico
import br.com.viavalesistemas.manutencaobike.model.TipoServico
import br.com.viavalesistemas.manutencaobike.ui.lerCentavos
import br.com.viavalesistemas.manutencaobike.ui.lerInteiro
import br.com.viavalesistemas.manutencaobike.ui.mensagemParaUsuario
import br.com.viavalesistemas.manutencaobike.ui.navegacao.RotaServicoForm
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import kotlin.math.roundToLong

data class ServicoFormUiState(
    val tipos: List<TipoServico> = emptyList(),
    val tipoSelecionado: TipoServico? = null,
    val data: LocalDate = LocalDate.now(),
    val km: String = "",
    val custo: String = "",
    val observacoes: String = "",
    val salvando: Boolean = false,
) {
    val custoValido: Boolean get() = custo.isBlank() || lerCentavos(custo) != null
    val podeSalvar: Boolean get() = tipoSelecionado != null && lerInteiro(km) != null && custoValido && !salvando
}

sealed interface ServicoFormEfeito {
    data object Voltar : ServicoFormEfeito
    data class Mensagem(val texto: String) : ServicoFormEfeito
}

@HiltViewModel
class ServicoFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    bikeRepository: BikeRepository,
    catalogoRepository: CatalogoRepository,
    private val servicoRepository: ServicoRepository,
) : ViewModel() {

    private val bikeId = savedStateHandle.toRoute<RotaServicoForm>().bikeId

    private val _uiState = MutableStateFlow(ServicoFormUiState())
    val uiState = _uiState.asStateFlow()

    private val _efeitos = Channel<ServicoFormEfeito>(Channel.BUFFERED)
    val efeitos = _efeitos.receiveAsFlow()

    init {
        viewModelScope.launch {
            val tipos = catalogoRepository.observarTiposServico().first()
            val km = bikeRepository.observarBike(bikeId).first()?.kmAtual ?: 0.0
            _uiState.update { it.copy(tipos = tipos, km = km.roundToLong().toString()) }
        }
    }

    fun onTipoChange(tipo: TipoServico) = _uiState.update { it.copy(tipoSelecionado = tipo) }
    fun onDataChange(valor: LocalDate) = _uiState.update { it.copy(data = valor) }
    fun onKmChange(valor: String) = _uiState.update { it.copy(km = valor.filter(Char::isDigit)) }
    fun onCustoChange(valor: String) = _uiState.update { it.copy(custo = valor) }
    fun onObservacoesChange(valor: String) = _uiState.update { it.copy(observacoes = valor) }

    fun onSalvar() {
        val estado = _uiState.value
        val tipo = estado.tipoSelecionado ?: return
        if (!estado.podeSalvar) return
        val servico = Servico(
            bikeId = bikeId,
            tipoServicoId = tipo.id,
            tipoNome = tipo.nome,
            data = estado.data,
            km = lerInteiro(estado.km)!!.toDouble(),
            custoCentavos = estado.custo.takeIf { it.isNotBlank() }?.let(::lerCentavos),
            observacoes = estado.observacoes.trim().ifBlank { null },
        )
        viewModelScope.launch {
            _uiState.update { it.copy(salvando = true) }
            servicoRepository.registrar(servico)
                .onSuccess { _efeitos.send(ServicoFormEfeito.Voltar) }
                .onFailure {
                    _uiState.update { s -> s.copy(salvando = false) }
                    _efeitos.send(ServicoFormEfeito.Mensagem(it.mensagemParaUsuario()))
                }
        }
    }
}
