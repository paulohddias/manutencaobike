package br.com.viavalesistemas.manutencaobike.ui.bikeform

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.viavalesistemas.manutencaobike.ui.componentes.ColetarEfeitos
import br.com.viavalesistemas.manutencaobike.ui.componentes.LocalSnackbarHostState
import br.com.viavalesistemas.manutencaobike.ui.componentes.TopoComVoltar

@Composable
fun BikeFormScreen(onVoltar: () -> Unit, viewModel: BikeFormViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbarHostState.current
    ColetarEfeitos(viewModel.efeitos) { efeito ->
        when (efeito) {
            BikeFormEfeito.Voltar -> onVoltar()
            is BikeFormEfeito.Mensagem -> snackbar.showSnackbar(efeito.texto)
        }
    }

    Scaffold(
        topBar = { TopoComVoltar(if (uiState.editando) "Editar bike" else "Nova bike", onVoltar) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            OutlinedTextField(
                value = uiState.nome,
                onValueChange = viewModel::onNomeChange,
                label = { Text("Nome / apelido *") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = uiState.marca,
                onValueChange = viewModel::onMarcaChange,
                label = { Text("Marca") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = uiState.modelo,
                onValueChange = viewModel::onModeloChange,
                label = { Text("Modelo") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = uiState.km,
                onValueChange = viewModel::onKmChange,
                label = { Text("Quilometragem atual (km)") },
                enabled = uiState.kmEditavel,
                supportingText = if (!uiState.kmEditavel) {
                    { Text("Atualizada automaticamente pelo Strava") }
                } else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = viewModel::onSalvar,
                enabled = uiState.podeSalvar,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) { Text("Salvar") }
        }
    }
}
