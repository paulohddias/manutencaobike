package br.com.viavalesistemas.manutencaobike.ui.pecaform

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import br.com.viavalesistemas.manutencaobike.data.repositorio.BikeRepository
import br.com.viavalesistemas.manutencaobike.data.repositorio.CatalogoRepository
import br.com.viavalesistemas.manutencaobike.data.repositorio.PecaRepository
import br.com.viavalesistemas.manutencaobike.model.Peca
import br.com.viavalesistemas.manutencaobike.model.TipoPeca
import br.com.viavalesistemas.manutencaobike.ui.lerCentavos
import br.com.viavalesistemas.manutencaobike.ui.lerInteiro
import br.com.viavalesistemas.manutencaobike.ui.mensagemParaUsuario
import br.com.viavalesistemas.manutencaobike.ui.navegacao.RotaPecaForm
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

data class PecaFormUiState(
    val tipos: List<TipoPeca> = emptyList(),
    val tipoSelecionado: TipoPeca? = null,
    val descricao: String = "",
    val vidaUtilKm: String = "",
    val kmJaRodados: String = "0",
    val dataInstalacao: LocalDate = LocalDate.now(),
    val custo: String = "",
    val kmAtualBike: Double = 0.0,
    val salvando: Boolean = false,
) {
    val custoValido: Boolean get() = custo.isBlank() || lerCentavos(custo) != null
    val podeSalvar: Boolean
        get() = tipoSelecionado != null && (lerInteiro(vidaUtilKm) ?: 0) > 0 && custoValido && !salvando
}

sealed interface PecaFormEfeito {
    data object Voltar : PecaFormEfeito
    data class Mensagem(val texto: String) : PecaFormEfeito
}

@HiltViewModel
class PecaFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    bikeRepository: BikeRepository,
    catalogoRepository: CatalogoRepository,
    private val pecaRepository: PecaRepository,
) : ViewModel() {

    private val rota = savedStateHandle.toRoute<RotaPecaForm>()

    private val _uiState = MutableStateFlow(PecaFormUiState())
    val uiState = _uiState.asStateFlow()

    private val _efeitos = Channel<PecaFormEfeito>(Channel.BUFFERED)
    val efeitos = _efeitos.receiveAsFlow()

    init {
        viewModelScope.launch {
            val tipos = catalogoRepository.observarTiposPeca().first()
            val bike = bikeRepository.observarBike(rota.bikeId).first()
            _uiState.update { it.copy(tipos = tipos, kmAtualBike = bike?.kmAtual ?: 0.0) }
            tipos.find { it.id == rota.tipoPecaId }?.let(::onTipoChange)
        }
    }

    fun onTipoChange(tipo: TipoPeca) = _uiState.update {
        it.copy(
            tipoSelecionado = tipo,
            vidaUtilKm = tipo.vidaUtilKmPadrao?.toString() ?: it.vidaUtilKm,
        )
    }

    fun onDescricaoChange(valor: String) = _uiState.update { it.copy(descricao = valor) }
    fun onVidaUtilChange(valor: String) = _uiState.update { it.copy(vidaUtilKm = valor.filter(Char::isDigit)) }
    fun onKmJaRodadosChange(valor: String) = _uiState.update { it.copy(kmJaRodados = valor.filter(Char::isDigit)) }
    fun onDataChange(valor: LocalDate) = _uiState.update { it.copy(dataInstalacao = valor) }
    fun onCustoChange(valor: String) = _uiState.update { it.copy(custo = valor) }

    fun onSalvar() {
        val estado = _uiState.value
        val tipo = estado.tipoSelecionado ?: return
        if (!estado.podeSalvar) return
        // Registrar "km já rodados" é mais fácil para o usuário do que lembrar o odômetro da bike no dia da instalação.
        val kmJaRodados = lerInteiro(estado.kmJaRodados) ?: 0
        val peca = Peca(
            bikeId = rota.bikeId,
            tipoPecaId = tipo.id,
            tipoNome = tipo.nome,
            descricao = estado.descricao.trim().ifBlank { null },
            kmInstalacao = estado.kmAtualBike - kmJaRodados,
            vidaUtilKm = lerInteiro(estado.vidaUtilKm)!!,
            dataInstalacao = estado.dataInstalacao,
            custoCentavos = estado.custo.takeIf { it.isNotBlank() }?.let(::lerCentavos),
        )
        viewModelScope.launch {
            _uiState.update { it.copy(salvando = true) }
            pecaRepository.instalar(peca)
                .onSuccess { _efeitos.send(PecaFormEfeito.Voltar) }
                .onFailure {
                    _uiState.update { s -> s.copy(salvando = false) }
                    _efeitos.send(PecaFormEfeito.Mensagem(it.mensagemParaUsuario()))
                }
        }
    }
}
