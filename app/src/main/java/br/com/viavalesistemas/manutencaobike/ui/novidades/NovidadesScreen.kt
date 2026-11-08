package br.com.viavalesistemas.manutencaobike.ui.novidades

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import br.com.viavalesistemas.manutencaobike.BuildConfig
import br.com.viavalesistemas.manutencaobike.data.novidades.NovidadesRepository
import br.com.viavalesistemas.manutencaobike.model.NotasVersao
import br.com.viavalesistemas.manutencaobike.ui.componentes.TopoComVoltar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NovidadesViewModel @Inject constructor(repository: NovidadesRepository) : ViewModel() {
    private val _versoes = MutableStateFlow<List<NotasVersao>>(emptyList())
    val versoes = _versoes.asStateFlow()

    init {
        viewModelScope.launch { _versoes.value = repository.todas().filterNot { it.vazia } }
    }
}

/** Histórico de todas as versões, aberto pelo menu da tela inicial. */
@Composable
fun NovidadesScreen(onVoltar: () -> Unit, viewModel: NovidadesViewModel = hiltViewModel()) {
    val versoes by viewModel.versoes.collectAsStateWithLifecycle()
    Scaffold(topBar = { TopoComVoltar("Novidades", onVoltar) }) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            item {
                Text(
                    "Você está na versão ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(versoes, key = { it.codigo }) { notas ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text("Versão ${notas.versao}", style = MaterialTheme.typography.titleLarge)
                        Text(
                            dataDasNotas(notas),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        ConteudoNotas(notas, Modifier.padding(top = 16.dp))
                    }
                }
            }
        }
    }
}
