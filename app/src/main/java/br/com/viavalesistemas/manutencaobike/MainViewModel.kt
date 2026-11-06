package br.com.viavalesistemas.manutencaobike

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.viavalesistemas.manutencaobike.data.repositorio.StravaRepository
import br.com.viavalesistemas.manutencaobike.ui.mensagemParaUsuario
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val stravaRepository: StravaRepository,
) : ViewModel() {

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
