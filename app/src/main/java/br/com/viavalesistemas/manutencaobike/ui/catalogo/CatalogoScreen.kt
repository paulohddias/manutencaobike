package br.com.viavalesistemas.manutencaobike.ui.catalogo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.viavalesistemas.manutencaobike.model.TipoPeca
import br.com.viavalesistemas.manutencaobike.model.TipoServico
import androidx.compose.material3.MaterialTheme
import br.com.viavalesistemas.manutencaobike.ui.componentes.AbaMarca
import br.com.viavalesistemas.manutencaobike.ui.componentes.BotaoAdicionar
import br.com.viavalesistemas.manutencaobike.ui.componentes.ColetarEfeitos
import br.com.viavalesistemas.manutencaobike.ui.componentes.Confirmacao
import br.com.viavalesistemas.manutencaobike.ui.componentes.DialogoConfirmacao
import br.com.viavalesistemas.manutencaobike.ui.componentes.LocalSnackbarHostState
import br.com.viavalesistemas.manutencaobike.ui.componentes.TopoComVoltar
import br.com.viavalesistemas.manutencaobike.ui.formatarKm
import br.com.viavalesistemas.manutencaobike.ui.lerInteiro

@Composable
fun CatalogoScreen(onVoltar: () -> Unit, viewModel: CatalogoViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbarHostState.current
    ColetarEfeitos(viewModel.mensagens) { snackbar.showSnackbar(it) }

    var aba by rememberSaveable { mutableIntStateOf(0) }
    // null = fechado; TipoPeca/TipoServico com id 0 = novo.
    var editandoPeca by remember { mutableStateOf<TipoPeca?>(null) }
    var editandoServico by remember { mutableStateOf<TipoServico?>(null) }
    var confirmacao by remember { mutableStateOf<Confirmacao?>(null) }

    Scaffold(
        topBar = { TopoComVoltar("Peças e serviços", onVoltar) },
        floatingActionButton = {
            BotaoAdicionar(if (aba == 0) "Novo tipo de peça" else "Novo tipo de serviço", onClick = {
                if (aba == 0) editandoPeca = TipoPeca(nome = "") else editandoServico = TipoServico(nome = "")
            })
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            PrimaryTabRow(selectedTabIndex = aba, containerColor = MaterialTheme.colorScheme.background) {
                AbaMarca("Peças", aba == 0) { aba = 0 }
                AbaMarca("Serviços", aba == 1) { aba = 1 }
            }
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (aba == 0) {
                    items(uiState.tiposPeca, key = { it.id }) { tipo ->
                        ItemCatalogo(
                            titulo = tipo.nome,
                            subtitulo = tipo.vidaUtilKmPadrao?.let { "Vida útil: ${formatarKm(it.toDouble())}" } ?: "Sem vida útil padrão",
                            onClick = { editandoPeca = tipo },
                            onExcluir = {
                                confirmacao = Confirmacao("Excluir ${tipo.nome}?", "O tipo sai do catálogo.", "Excluir") {
                                    viewModel.onExcluirTipoPeca(tipo)
                                }
                            },
                        )
                    }
                } else {
                    items(uiState.tiposServico, key = { it.id }) { tipo ->
                        ItemCatalogo(
                            titulo = tipo.nome,
                            subtitulo = descricaoIntervalo(tipo),
                            onClick = { editandoServico = tipo },
                            onExcluir = {
                                confirmacao = Confirmacao("Excluir ${tipo.nome}?", "O tipo sai do catálogo.", "Excluir") {
                                    viewModel.onExcluirTipoServico(tipo)
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    editandoPeca?.let { tipo ->
        DialogoTipoPeca(
            inicial = tipo,
            onSalvar = {
                viewModel.onSalvarTipoPeca(it)
                editandoPeca = null
            },
            onFechar = { editandoPeca = null },
        )
    }
    editandoServico?.let { tipo ->
        DialogoTipoServico(
            inicial = tipo,
            onSalvar = {
                viewModel.onSalvarTipoServico(it)
                editandoServico = null
            },
            onFechar = { editandoServico = null },
        )
    }
    confirmacao?.let { DialogoConfirmacao(it, onFechar = { confirmacao = null }) }
}

private fun descricaoIntervalo(tipo: TipoServico): String {
    val partes = listOfNotNull(
        tipo.intervaloKm?.takeIf { it > 0 }?.let { "a cada ${formatarKm(it.toDouble())}" },
        tipo.intervaloMeses?.takeIf { it > 0 }?.let { if (it == 1) "todo mês" else "a cada $it meses" },
    )
    return if (partes.isEmpty()) "Sem lembrete" else partes.joinToString(" ou ").replaceFirstChar(Char::uppercase)
}

@Composable
private fun ItemCatalogo(titulo: String, subtitulo: String, onClick: () -> Unit, onExcluir: () -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = { Text(titulo) },
            supportingContent = { Text(subtitulo) },
            trailingContent = {
                IconButton(onClick = onExcluir) { Icon(Icons.Default.Delete, contentDescription = "Excluir $titulo") }
            },
            modifier = Modifier.clickable(onClick = onClick),
        )
    }
}

@Composable
private fun DialogoTipoPeca(inicial: TipoPeca, onSalvar: (TipoPeca) -> Unit, onFechar: () -> Unit) {
    var nome by rememberSaveable { mutableStateOf(inicial.nome) }
    var vidaUtil by rememberSaveable { mutableStateOf(inicial.vidaUtilKmPadrao?.toString().orEmpty()) }
    AlertDialog(
        onDismissRequest = onFechar,
        title = { Text(if (inicial.id == 0L) "Novo tipo de peça" else "Editar tipo de peça") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
                OutlinedTextField(
                    value = vidaUtil,
                    onValueChange = { vidaUtil = it.filter(Char::isDigit) },
                    label = { Text("Vida útil padrão (km)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = nome.isNotBlank(),
                onClick = { onSalvar(inicial.copy(nome = nome.trim(), vidaUtilKmPadrao = lerInteiro(vidaUtil))) },
            ) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onFechar) { Text("Cancelar") } },
    )
}

@Composable
private fun DialogoTipoServico(inicial: TipoServico, onSalvar: (TipoServico) -> Unit, onFechar: () -> Unit) {
    var nome by rememberSaveable { mutableStateOf(inicial.nome) }
    var km by rememberSaveable { mutableStateOf(inicial.intervaloKm?.toString().orEmpty()) }
    var meses by rememberSaveable { mutableStateOf(inicial.intervaloMeses?.toString().orEmpty()) }
    AlertDialog(
        onDismissRequest = onFechar,
        title = { Text(if (inicial.id == 0L) "Novo tipo de serviço" else "Editar tipo de serviço") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
                Text("Lembrete (o que vencer primeiro)")
                OutlinedTextField(
                    value = km,
                    onValueChange = { km = it.filter(Char::isDigit) },
                    label = { Text("A cada quantos km") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                OutlinedTextField(
                    value = meses,
                    onValueChange = { meses = it.filter(Char::isDigit) },
                    label = { Text("A cada quantos meses") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = nome.isNotBlank(),
                onClick = {
                    onSalvar(inicial.copy(nome = nome.trim(), intervaloKm = lerInteiro(km), intervaloMeses = lerInteiro(meses)))
                },
            ) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onFechar) { Text("Cancelar") } },
    )
}
