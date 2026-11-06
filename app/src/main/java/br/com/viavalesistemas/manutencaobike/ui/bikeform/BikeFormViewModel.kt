package br.com.viavalesistemas.manutencaobike.ui.bikeform

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import br.com.viavalesistemas.manutencaobike.data.repositorio.BikeRepository
import br.com.viavalesistemas.manutencaobike.model.Bike
import br.com.viavalesistemas.manutencaobike.ui.lerInteiro
import br.com.viavalesistemas.manutencaobike.ui.mensagemParaUsuario
import br.com.viavalesistemas.manutencaobike.ui.navegacao.RotaBikeForm
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToLong

data class BikeFormUiState(
    val nome: String = "",
    val marca: String = "",
    val modelo: String = "",
    val km: String = "0",
    val bikeOriginal: Bike? = null,
    val salvando: Boolean = false,
) {
    val editando: Boolean get() = bikeOriginal != null
    val kmEditavel: Boolean get() = bikeOriginal?.doStrava != true
    val podeSalvar: Boolean get() = nome.isNotBlank() && lerInteiro(km) != null && !salvando
}

sealed interface BikeFormEfeito {
    data object Voltar : BikeFormEfeito
    data class Mensagem(val texto: String) : BikeFormEfeito
}

@HiltViewModel
class BikeFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val bikeRepository: BikeRepository,
) : ViewModel() {

    private val bikeId = savedStateHandle.toRoute<RotaBikeForm>().bikeId

    private val _uiState = MutableStateFlow(BikeFormUiState())
    val uiState = _uiState.asStateFlow()

    private val _efeitos = Channel<BikeFormEfeito>(Channel.BUFFERED)
    val efeitos = _efeitos.receiveAsFlow()

    init {
        if (bikeId != 0L) {
            viewModelScope.launch {
                bikeRepository.observarBike(bikeId).first()?.let { bike ->
                    _uiState.update {
                        it.copy(
                            nome = bike.nome,
                            marca = bike.marca.orEmpty(),
                            modelo = bike.modelo.orEmpty(),
                            km = bike.kmAtual.roundToLong().toString(),
                            bikeOriginal = bike,
                        )
                    }
                }
            }
        }
    }

    fun onNomeChange(valor: String) = _uiState.update { it.copy(nome = valor) }
    fun onMarcaChange(valor: String) = _uiState.update { it.copy(marca = valor) }
    fun onModeloChange(valor: String) = _uiState.update { it.copy(modelo = valor) }
    fun onKmChange(valor: String) = _uiState.update { it.copy(km = valor.filter(Char::isDigit)) }

    fun onSalvar() {
        val estado = _uiState.value
        if (!estado.podeSalvar) return
        val base = estado.bikeOriginal ?: Bike(nome = "", kmAtual = 0.0)
        val bike = base.copy(
            nome = estado.nome.trim(),
            marca = estado.marca.trim().ifBlank { null },
            modelo = estado.modelo.trim().ifBlank { null },
            kmAtual = if (estado.kmEditavel) lerInteiro(estado.km)!!.toDouble() else base.kmAtual,
        )
        viewModelScope.launch {
            _uiState.update { it.copy(salvando = true) }
            bikeRepository.salvar(bike)
                .onSuccess { _efeitos.send(BikeFormEfeito.Voltar) }
                .onFailure {
                    _uiState.update { s -> s.copy(salvando = false) }
                    _efeitos.send(BikeFormEfeito.Mensagem(it.mensagemParaUsuario()))
                }
        }
    }
}
