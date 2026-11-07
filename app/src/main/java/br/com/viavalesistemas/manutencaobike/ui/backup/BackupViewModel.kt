package br.com.viavalesistemas.manutencaobike.ui.backup

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.viavalesistemas.manutencaobike.data.backup.BackupRemoto
import br.com.viavalesistemas.manutencaobike.data.backup.BackupRepository
import br.com.viavalesistemas.manutencaobike.data.repositorio.StravaRepository
import br.com.viavalesistemas.manutencaobike.ui.mensagemParaUsuario
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

enum class Operacao { ENVIANDO, RESTAURANDO, EXCLUINDO }

data class BackupUiState(
    val conectado: Boolean = false,
    val nomeAtleta: String? = null,
    val automatico: Boolean = true,
    val ultimoBackupEm: Instant? = null,
    val backups: List<BackupRemoto> = emptyList(),
    val carregandoLista: Boolean = false,
    val operacao: Operacao? = null,
) {
    val ocupado: Boolean get() = operacao != null
}

sealed interface BackupEfeito {
    data class Mensagem(val texto: String) : BackupEfeito
    data class AbrirNavegador(val url: String) : BackupEfeito
    data object ReiniciarApp : BackupEfeito
}

@Stable
interface BackupActions {
    fun onConectarStrava()
    fun onFazerBackup()
    fun onRestaurar(backup: BackupRemoto)
    fun onAutomaticoChange(ativo: Boolean)
    fun onAtualizarLista()
    fun onExcluirDados()
}

private data class EstadoLocal(
    val backups: List<BackupRemoto> = emptyList(),
    val carregandoLista: Boolean = false,
    val operacao: Operacao? = null,
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupRepository: BackupRepository,
    private val stravaRepository: StravaRepository,
) : ViewModel(), BackupActions {

    private val local = MutableStateFlow(EstadoLocal())

    val uiState: StateFlow<BackupUiState> = combine(
        stravaRepository.conectado,
        stravaRepository.nomeAtleta,
        backupRepository.automatico,
        backupRepository.ultimoBackupEm,
        local,
    ) { conectado, nome, automatico, ultimo, local ->
        BackupUiState(conectado, nome, automatico, ultimo, local.backups, local.carregandoLista, local.operacao)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BackupUiState())

    private val _efeitos = Channel<BackupEfeito>(Channel.BUFFERED)
    val efeitos = _efeitos.receiveAsFlow()

    init {
        // Recarrega a lista ao conectar (inclusive quando o login do Strava volta para esta tela).
        viewModelScope.launch {
            stravaRepository.conectado.distinctUntilChanged().filter { it }.collect { onAtualizarLista() }
        }
    }

    override fun onConectarStrava() {
        _efeitos.trySend(BackupEfeito.AbrirNavegador(stravaRepository.urlAutorizacao()))
    }

    override fun onAtualizarLista() {
        if (local.value.carregandoLista) return
        viewModelScope.launch {
            local.update { it.copy(carregandoLista = true) }
            backupRepository.listar()
                .onSuccess { lista -> local.update { it.copy(backups = lista) } }
                .onFailure { mensagem(it) }
            local.update { it.copy(carregandoLista = false) }
        }
    }

    override fun onFazerBackup() = executar(Operacao.ENVIANDO) {
        backupRepository.fazerBackup()
            .onSuccess {
                _efeitos.send(BackupEfeito.Mensagem("Backup enviado com sucesso."))
                onAtualizarLista()
            }
            .onFailure { mensagem(it) }
    }

    override fun onRestaurar(backup: BackupRemoto) = executar(Operacao.RESTAURANDO) {
        backupRepository.restaurar(backup.id)
            .onSuccess { _efeitos.send(BackupEfeito.ReiniciarApp) }
            .onFailure { mensagem(it) }
    }

    override fun onAutomaticoChange(ativo: Boolean) {
        viewModelScope.launch { backupRepository.definirAutomatico(ativo) }
    }

    override fun onExcluirDados() = executar(Operacao.EXCLUINDO) {
        backupRepository.excluirDadosDoServidor()
            .onSuccess {
                local.update { it.copy(backups = emptyList()) }
                _efeitos.send(BackupEfeito.Mensagem("Seus dados foram apagados do servidor."))
            }
            .onFailure { mensagem(it) }
    }

    private fun executar(operacao: Operacao, bloco: suspend () -> Unit) {
        if (local.value.operacao != null) return
        viewModelScope.launch {
            local.update { it.copy(operacao = operacao) }
            try {
                bloco()
            } finally {
                local.update { it.copy(operacao = null) }
            }
        }
    }

    private suspend fun mensagem(erro: Throwable) {
        _efeitos.send(BackupEfeito.Mensagem(erro.mensagemParaUsuario()))
    }
}
