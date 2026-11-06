package br.com.viavalesistemas.manutencaobike.ui.bikes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.viavalesistemas.manutencaobike.data.repositorio.BikeRepository
import br.com.viavalesistemas.manutencaobike.data.repositorio.CatalogoRepository
import br.com.viavalesistemas.manutencaobike.data.repositorio.PecaRepository
import br.com.viavalesistemas.manutencaobike.data.repositorio.ServicoRepository
import br.com.viavalesistemas.manutencaobike.data.repositorio.StravaRepository
import br.com.viavalesistemas.manutencaobike.model.Bike
import br.com.viavalesistemas.manutencaobike.model.StatusDesgaste
import br.com.viavalesistemas.manutencaobike.model.alertasDaBike
import br.com.viavalesistemas.manutencaobike.model.desgaste
import br.com.viavalesistemas.manutencaobike.model.situacaoServico
import br.com.viavalesistemas.manutencaobike.model.statusPorFracao
import br.com.viavalesistemas.manutencaobike.ui.mensagemParaUsuario
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import javax.inject.Inject

data class BikeResumo(
    val bike: Bike,
    val itensTrocar: Int,
    val itensAtencao: Int,
    /** Desgaste do item mais gasto (peça ou serviço); 0 quando não há nada cadastrado. */
    val maiorDesgaste: Double = 0.0,
    val temItensAcompanhados: Boolean = true,
    val foto: File? = null,
) {
    val status: StatusDesgaste get() = statusPorFracao(maiorDesgaste)
}

data class BikesUiState(
    val carregando: Boolean = true,
    val bikes: List<BikeResumo> = emptyList(),
    val bikesInativas: List<BikeResumo> = emptyList(),
    val stravaConfigurado: Boolean = false,
    val stravaConectado: Boolean = false,
    val nomeAtleta: String? = null,
    val sincronizando: Boolean = false,
)

sealed interface BikesEfeito {
    data class AbrirNavegador(val url: String) : BikesEfeito
    data class Mensagem(val texto: String) : BikesEfeito
}

@HiltViewModel
class BikesViewModel @Inject constructor(
    private val bikeRepository: BikeRepository,
    pecaRepository: PecaRepository,
    servicoRepository: ServicoRepository,
    catalogoRepository: CatalogoRepository,
    private val stravaRepository: StravaRepository,
) : ViewModel() {

    private val sincronizando = MutableStateFlow(false)

    private val resumos = combine(
        bikeRepository.observarBikes(),
        pecaRepository.observarInstaladas(),
        servicoRepository.observarTodos(),
        catalogoRepository.observarTiposServico(),
    ) { bikes, pecas, servicos, tiposServico ->
        val hoje = LocalDate.now()
        val pecasPorBike = pecas.groupBy { it.bikeId }
        val servicosPorBike = servicos.groupBy { it.bikeId }
        bikes.map { bike ->
            val pecasDaBike = pecasPorBike[bike.id].orEmpty()
            val servicosDaBike = servicosPorBike[bike.id].orEmpty()
            val alertas = alertasDaBike(bike, pecasDaBike, servicosDaBike, tiposServico, hoje)
            val desgastes = pecasDaBike.map { it.desgaste(bike.kmAtual).fracao } +
                tiposServico.filter { it.temIntervalo }.mapNotNull { situacaoServico(it, servicosDaBike, bike.kmAtual, hoje).fracao }
            BikeResumo(
                bike = bike,
                foto = bikeRepository.arquivoDaFoto(bike),
                itensTrocar = alertas.count { it.status == StatusDesgaste.TROCAR },
                itensAtencao = alertas.count { it.status == StatusDesgaste.ATENCAO },
                maiorDesgaste = desgastes.maxOrNull() ?: 0.0,
                temItensAcompanhados = desgastes.isNotEmpty(),
            )
        }
    }

    val uiState: StateFlow<BikesUiState> = combine(
        resumos,
        stravaRepository.conectado,
        stravaRepository.nomeAtleta,
        sincronizando,
    ) { bikes, conectado, nomeAtleta, sincronizando ->
        val (ativas, inativas) = bikes.partition { it.bike.ativa }
        BikesUiState(
            carregando = false,
            bikes = ativas,
            bikesInativas = inativas,
            stravaConfigurado = stravaRepository.configurado,
            stravaConectado = conectado,
            nomeAtleta = nomeAtleta,
            sincronizando = sincronizando,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BikesUiState(stravaConfigurado = stravaRepository.configurado),
    )

    private val _efeitos = Channel<BikesEfeito>(Channel.BUFFERED)
    val efeitos = _efeitos.receiveAsFlow()

    fun onConectarStrava() {
        _efeitos.trySend(BikesEfeito.AbrirNavegador(stravaRepository.urlAutorizacao()))
    }

    fun onSincronizar() {
        if (sincronizando.value) return
        viewModelScope.launch {
            sincronizando.value = true
            try {
                stravaRepository.sincronizarBikes()
                    .onSuccess { _efeitos.send(BikesEfeito.Mensagem("$it bike(s) atualizada(s) do Strava.")) }
                    .onFailure { _efeitos.send(BikesEfeito.Mensagem(it.mensagemParaUsuario())) }
            } finally {
                sincronizando.value = false
            }
        }
    }

    fun onDesconectarStrava() {
        viewModelScope.launch {
            stravaRepository.desconectar()
            _efeitos.send(BikesEfeito.Mensagem("Strava desconectado. Suas bikes continuam salvas."))
        }
    }
}
