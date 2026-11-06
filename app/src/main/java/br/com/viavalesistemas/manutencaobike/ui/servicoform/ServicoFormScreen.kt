package br.com.viavalesistemas.manutencaobike.ui.servicoform

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
import br.com.viavalesistemas.manutencaobike.ui.componentes.CampoData
import br.com.viavalesistemas.manutencaobike.ui.componentes.CampoSelecao
import br.com.viavalesistemas.manutencaobike.ui.componentes.ColetarEfeitos
import br.com.viavalesistemas.manutencaobike.ui.componentes.LocalSnackbarHostState
import br.com.viavalesistemas.manutencaobike.ui.componentes.TopoComVoltar

@Composable
fun ServicoFormScreen(onVoltar: () -> Unit, viewModel: ServicoFormViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbarHostState.current
    ColetarEfeitos(viewModel.efeitos) { efeito ->
        when (efeito) {
            ServicoFormEfeito.Voltar -> onVoltar()
            is ServicoFormEfeito.Mensagem -> snackbar.showSnackbar(efeito.texto)
        }
    }

    Scaffold(
        topBar = { TopoComVoltar("Registrar serviço", onVoltar) },
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
            CampoSelecao(
                rotulo = "Tipo de serviço *",
                opcoes = uiState.tipos,
                selecionada = uiState.tipoSelecionado,
                textoDe = { it.nome },
                onSelecionar = viewModel::onTipoChange,
                modifier = Modifier.fillMaxWidth(),
            )
            CampoData(
                rotulo = "Data",
                data = uiState.data,
                onDataChange = viewModel::onDataChange,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = uiState.km,
                onValueChange = viewModel::onKmChange,
                label = { Text("Km da bike no serviço *") },
                supportingText = { Text("Já vem com o km atual; ajuste se o serviço foi feito antes.") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = uiState.custo,
                onValueChange = viewModel::onCustoChange,
                label = { Text("Custo (R$)") },
                isError = !uiState.custoValido,
                supportingText = if (!uiState.custoValido) {
                    { Text("Use o formato 123,45") }
                } else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = uiState.observacoes,
                onValueChange = viewModel::onObservacoesChange,
                label = { Text("Observações") },
                placeholder = { Text("Oficina, o que foi feito…") },
                minLines = 3,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
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
