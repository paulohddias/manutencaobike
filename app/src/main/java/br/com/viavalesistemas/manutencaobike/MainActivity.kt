package br.com.viavalesistemas.manutencaobike

import android.content.Intent
import android.graphics.Color
import androidx.activity.SystemBarStyle
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import br.com.viavalesistemas.manutencaobike.data.repositorio.StravaRepository
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.viavalesistemas.manutencaobike.ui.navegacao.AppNavHost
import br.com.viavalesistemas.manutencaobike.ui.novidades.FolhaNovidades
import br.com.viavalesistemas.manutencaobike.ui.theme.ManutencaobikeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Todas as telas começam com o cabeçalho escuro da marca, então os ícones da status bar são sempre claros.
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
        // Só na primeira criação: após rotação o mesmo intent seria processado de novo.
        if (savedInstanceState == null) tratarIntent(intent)
        setContent {
            ManutencaobikeTheme {
                AppNavHost(mensagensGlobais = viewModel.mensagens)
                val novidades by viewModel.novidades.collectAsStateWithLifecycle()
                if (novidades.isNotEmpty()) FolhaNovidades(novidades, onFechar = viewModel::onNovidadesVistas)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        tratarIntent(intent)
    }

    private fun tratarIntent(intent: Intent) {
        val uri = intent.data ?: return
        if (uri.scheme == StravaRepository.ESQUEMA_CALLBACK) viewModel.onRetornoStrava(uri)
    }
}
