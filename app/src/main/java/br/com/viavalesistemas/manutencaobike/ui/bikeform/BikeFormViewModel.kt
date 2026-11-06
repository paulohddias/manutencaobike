package br.com.viavalesistemas.manutencaobike.ui.bikeform

import android.net.Uri
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
import java.io.File
import javax.inject.Inject
import kotlin.math.roundToLong

data class BikeFormUiState(
    val nome: String = "",
    val marca: String = "",
    val modelo: String = "",
    val km: String = "0",
    val ativa: Boolean = true,
    val foto: File? = null,
    val bikeOriginal: Bike? = null,
    val processandoFoto: Boolean = false,
    val salvando: Boolean = false,
) {
    val editando: Boolean get() = bikeOriginal != null
    val kmEditavel: Boolean get() = bikeOriginal?.doStrava != true
    val podeSalvar: Boolean get() = nome.isNotBlank() && lerInteiro(km) != null && !salvando && !processandoFoto
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

    private var fotoAtual: String? = null
    private var salvou = false

    init {
        if (bikeId != 0L) {
            viewModelScope.launch {
                bikeRepository.observarBike(bikeId).first()?.let { bike ->
                    fotoAtual = bike.foto
                    _uiState.update {
                        it.copy(
                            nome = bike.nome,
                            marca = bike.marca.orEmpty(),
                            modelo = bike.modelo.orEmpty(),
                            km = bike.kmAtual.roundToLong().toString(),
                            ativa = bike.ativa,
                            foto = bikeRepository.arquivoDaFoto(bike),
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
    fun onAtivaChange(valor: Boolean) = _uiState.update { it.copy(ativa = valor) }

    fun uriParaCamera(): Uri = bikeRepository.uriParaCamera()

    fun onFotoEscolhida(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(processandoFoto = true) }
            bikeRepository.importarFoto(uri)
                .onSuccess { nome -> trocarFoto(nome) }
                .onFailure { _efeitos.send(BikeFormEfeito.Mensagem(it.mensagemParaUsuario())) }
            bikeRepository.limparCapturas()
            _uiState.update { it.copy(processandoFoto = false) }
        }
    }

    fun onRemoverFoto() {
        viewModelScope.launch { trocarFoto(null) }
    }

    private suspend fun trocarFoto(nome: String?) {
        fotoAtual?.let { descartarSeNaoSalva(it) }
        fotoAtual = nome
        _uiState.update { it.copy(foto = nome?.let(bikeRepository::arquivoDaFoto)) }
    }

    /** Fotos escolhidas nesta tela e ainda não salvas não devem ficar órfãs no armazenamento. */
    private suspend fun descartarSeNaoSalva(nome: String) {
        if (nome != _uiState.value.bikeOriginal?.foto) bikeRepository.descartarFoto(nome)
    }

    fun onSalvar() {
        val estado = _uiState.value
        if (!estado.podeSalvar) return
        val base = estado.bikeOriginal ?: Bike(nome = "", kmAtual = 0.0)
        val bike = base.copy(
            nome = estado.nome.trim(),
            marca = estado.marca.trim().ifBlank { null },
            modelo = estado.modelo.trim().ifBlank { null },
            kmAtual = if (estado.kmEditavel) lerInteiro(estado.km)!!.toDouble() else base.kmAtual,
            ativa = estado.ativa,
            foto = fotoAtual,
        )
        viewModelScope.launch {
            _uiState.update { it.copy(salvando = true) }
            bikeRepository.salvar(bike)
                .onSuccess {
                    salvou = true
                    _efeitos.send(BikeFormEfeito.Voltar)
                }
                .onFailure {
                    _uiState.update { s -> s.copy(salvando = false) }
                    _efeitos.send(BikeFormEfeito.Mensagem(it.mensagemParaUsuario()))
                }
        }
    }

    override fun onCleared() {
        val nova = fotoAtual
        if (!salvou && nova != null && nova != _uiState.value.bikeOriginal?.foto) {
            bikeRepository.arquivoDaFoto(nova).delete()
        }
    }
}
