package br.com.viavalesistemas.manutencaobike

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.viavalesistemas.manutencaobike.data.atualizacao.AtualizacaoRepository
import br.com.viavalesistemas.manutencaobike.data.novidades.NovidadesRepository
import br.com.viavalesistemas.manutencaobike.data.repositorio.StravaRepository
import br.com.viavalesistemas.manutencaobike.model.NotasVersao
import br.com.viavalesistemas.manutencaobike.ui.mensagemParaUsuario
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val stravaRepository: StravaRepository,
    private val novidadesRepository: NovidadesRepository,
    private val atualizacaoRepository: AtualizacaoRepository,
) : ViewModel() {

    private val _atualizacao = MutableStateFlow<Int?>(null)
    /** versionCode novo publicado no cadastro de versões da VVS; null = nada a avisar. */
    val atualizacao = _atualizacao.asStateFlow()

    private val _novidades = MutableStateFlow<List<NotasVersao>>(emptyList())
    /** Notas das versões instaladas desde a última vez que o usuário abriu o app; vazio = nada a mostrar. */
    val novidades = _novidades.asStateFlow()

    init {
        viewModelScope.launch { _novidades.value = novidadesRepository.pendentes() }
        viewModelScope.launch { _atualizacao.value = atualizacaoRepository.versaoDisponivel() }
    }

    fun onAtualizacaoDispensada() {
        val versao = _atualizacao.value ?: return
        _atualizacao.value = null
        viewModelScope.launch { atualizacaoRepository.dispensar(versao) }
    }

    /** Foi para a Play Store: volta a avisar na próxima abertura se ainda não atualizou. */
    fun onAtualizacaoAberta() {
        _atualizacao.value = null
    }

    fun onNovidadesVistas() {
        _novidades.value = emptyList()
        viewModelScope.launch { novidadesRepository.marcarVistas() }
    }

    private val _mensagens = Channel<String>(Channel.BUFFERED)
    val mensagens = _mensagens.receiveAsFlow()

    fun onRetornoStrava(uri: Uri) {
        val codigo = uri.getQueryParameter("code")
        if (uri.getQueryParameter("error") != null || codigo == null) {
            _mensagens.trySend("Conexão com o Strava cancelada.")
            return
        }
        val escopos = uri.getQueryParameter("scope").orEmpty().split(',')
        if ("profile:read_all" !in escopos) {
            _mensagens.trySend("Autorize o acesso ao perfil completo para importar suas bikes.")
            return
        }

        viewModelScope.launch {
            _mensagens.send("Conectando ao Strava…")
            val resultado = stravaRepository.conectar(codigo).mapCatching { stravaRepository.sincronizarBikes().getOrThrow() }
            resultado
                .onSuccess { _mensagens.send("Strava conectado: $it bike(s) importada(s).") }
                .onFailure { _mensagens.send(it.mensagemParaUsuario()) }
        }
    }
}
