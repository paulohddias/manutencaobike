package br.com.viavalesistemas.manutencaobike.ui.bikedetalhe

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import br.com.viavalesistemas.manutencaobike.model.StatusDesgaste
import br.com.viavalesistemas.manutencaobike.ui.componentes.AbaMarca
import br.com.viavalesistemas.manutencaobike.ui.componentes.BotaoAdicionar
import br.com.viavalesistemas.manutencaobike.ui.theme.marca
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.ui.layout.ContentScale
import br.com.viavalesistemas.manutencaobike.ui.componentes.Pilula
import coil3.compose.AsyncImage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.viavalesistemas.manutencaobike.model.Bike
import br.com.viavalesistemas.manutencaobike.ui.componentes.ColetarEfeitos
import br.com.viavalesistemas.manutencaobike.ui.componentes.Confirmacao
import br.com.viavalesistemas.manutencaobike.ui.componentes.DialogoConfirmacao
import br.com.viavalesistemas.manutencaobike.ui.componentes.LocalSnackbarHostState
import br.com.viavalesistemas.manutencaobike.ui.componentes.TopoComVoltar
import br.com.viavalesistemas.manutencaobike.ui.formatarKm
import br.com.viavalesistemas.manutencaobike.ui.formatarMoeda
import br.com.viavalesistemas.manutencaobike.ui.lerInteiro
import kotlin.math.roundToLong

private enum class Aba(val titulo: String) { PECAS("Peças"), SERVICOS("Serviços"), HISTORICO("Histórico") }

@Composable
fun BikeDetalheScreen(
    onVoltar: () -> Unit,
    onEditarBike: (Long) -> Unit,
    onInstalarPeca: (bikeId: Long, tipoPecaId: Long) -> Unit,
    onRegistrarServico: (Long) -> Unit,
    viewModel: BikeDetalheViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbarHostState.current
    ColetarEfeitos(viewModel.efeitos) { efeito ->
        when (efeito) {
            BikeDetalheEfeito.Voltar -> onVoltar()
            is BikeDetalheEfeito.InstalarPeca -> onInstalarPeca(efeito.bikeId, efeito.tipoPecaId)
            is BikeDetalheEfeito.Mensagem -> snackbar.showSnackbar(efeito.texto)
        }
    }

    BikeDetalheContent(
        uiState = uiState,
        actions = viewModel,
        onVoltar = onVoltar,
        onEditarBike = { onEditarBike(viewModel.bikeId) },
        onInstalarPeca = { onInstalarPeca(viewModel.bikeId, 0L) },
        onRegistrarServico = { onRegistrarServico(viewModel.bikeId) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BikeDetalheContent(
    uiState: BikeDetalheUiState,
    actions: BikeDetalheActions,
    onVoltar: () -> Unit,
    onEditarBike: () -> Unit,
    onInstalarPeca: () -> Unit,
    onRegistrarServico: () -> Unit,
) {
    var abaSelecionada by rememberSaveable { mutableIntStateOf(0) }
    var confirmacao by remember { mutableStateOf<Confirmacao?>(null) }
    var editandoKm by rememberSaveable { mutableStateOf(false) }
    val bike = uiState.bike
    val aba = Aba.entries[abaSelecionada]

    Scaffold(
        topBar = {
            TopoComVoltar(titulo = bike?.nome.orEmpty(), onVoltar = onVoltar) {
                if (bike != null) {
                    MenuBike(
                        bike = bike,
                        onEditar = onEditarBike,
                        onAtualizarKm = { editandoKm = true },
                        onAlternarAtiva = {
                            if (bike.ativa) {
                                confirmacao = Confirmacao(
                                    titulo = "Inativar ${bike.nome}?",
                                    texto = "Ela vai para a seção de inativas e para de gerar alertas. " +
                                        "Peças e serviços continuam salvos e você pode reativar quando quiser.",
                                    acao = "Inativar",
                                    onConfirmar = actions::onAlternarAtiva,
                                )
                            } else {
                                actions.onAlternarAtiva()
                            }
                        },
                        onExcluir = {
                            confirmacao = Confirmacao(
                                titulo = "Excluir ${bike.nome}?",
                                texto = "Todas as peças e serviços desta bike também serão apagados.",
                                acao = "Excluir",
                                onConfirmar = actions::onExcluirBike,
                            )
                        },
                    )
                }
            }
        },
        floatingActionButton = {
            when (aba) {
                Aba.PECAS -> BotaoAdicionar("Instalar peça", onInstalarPeca)
                Aba.SERVICOS -> BotaoAdicionar("Registrar serviço", onRegistrarServico)
                Aba.HISTORICO -> Unit
            }
        },
        snackbarHost = { SnackbarHost(LocalSnackbarHostState.current) },
    ) { padding ->
        if (uiState.carregando || bike == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                if (uiState.carregando) CircularProgressIndicator() else Text("Bike não encontrada")
            }
            return@Scaffold
        }
        Column(Modifier.fillMaxSize().padding(padding)) {
            CabecalhoBike(bike, uiState)
            PrimaryTabRow(
                selectedTabIndex = abaSelecionada,
                containerColor = MaterialTheme.colorScheme.background,
                modifier = Modifier.padding(top = 4.dp),
            ) {
                Aba.entries.forEachIndexed { indice, item ->
                    AbaMarca(item.titulo, abaSelecionada == indice) { abaSelecionada = indice }
                }
            }
            when (aba) {
                Aba.PECAS -> AbaPecas(uiState.pecasInstaladas, actions, onPedirConfirmacao = { confirmacao = it })
                Aba.SERVICOS -> AbaServicos(uiState, actions, bike.kmAtual, onPedirConfirmacao = { confirmacao = it })
                Aba.HISTORICO -> AbaHistorico(uiState)
            }
        }
    }

    confirmacao?.let { DialogoConfirmacao(it, onFechar = { confirmacao = null }) }
    if (editandoKm && bike != null) {
        DialogoAtualizarKm(
            kmAtual = bike.kmAtual,
            onConfirmar = {
                actions.onAtualizarKm(it)
                editandoKm = false
            },
            onFechar = { editandoKm = false },
        )
    }
}

/** Continua a barra superior escura, com os números principais da bike (e a foto ao fundo, se houver). */
@Composable
private fun CabecalhoBike(bike: Bike, uiState: BikeDetalheUiState) {
    val marca = MaterialTheme.marca
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(Brush.verticalGradient(listOf(marca.cabecalho, marca.cabecalhoFim))),
    ) {
        if (uiState.foto != null) {
            AsyncImage(
                model = uiState.foto,
                contentDescription = "Foto da ${bike.nome}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
            // Escurece a foto de cima para baixo para os números continuarem legíveis.
            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            0f to marca.cabecalho.copy(alpha = 0.55f),
                            0.45f to marca.cabecalhoFim.copy(alpha = 0.35f),
                            1f to marca.cabecalhoFim.copy(alpha = 0.95f),
                        ),
                    ),
            )
        }
        ConteudoCabecalho(bike, uiState, comFoto = uiState.foto != null)
    }
}

@Composable
private fun ConteudoCabecalho(bike: Bike, uiState: BikeDetalheUiState, comFoto: Boolean) {
    val marca = MaterialTheme.marca
    Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 20.dp)) {
        if (comFoto) Spacer(Modifier.height(120.dp))
        if (!bike.ativa) {
            Pilula("Inativa · sem alertas", marca.sobreCabecalhoSuave)
            Spacer(Modifier.height(8.dp))
        }
        Text(
            bike.descricao ?: if (bike.doStrava) "Sincronizada com o Strava" else "Cadastro manual",
            style = MaterialTheme.typography.bodyMedium,
            color = marca.sobreCabecalhoSuave,
        )
        Text(formatarKm(bike.kmAtual), style = MaterialTheme.typography.displaySmall, color = marca.sobreCabecalho)
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val pecasVencidas = uiState.pecasInstaladas.count { it.status == StatusDesgaste.TROCAR }
            val servicosVencidos = uiState.situacoesServico.count { it.status == StatusDesgaste.TROCAR }
            NumeroCabecalho("Peças", uiState.pecasInstaladas.size.toString(), marca.sobreCabecalho, Modifier.weight(1f))
            NumeroCabecalho(
                "Vencidos",
                (pecasVencidas + servicosVencidos).toString(),
                if (pecasVencidas + servicosVencidos > 0) marca.trocar else marca.sobreCabecalho,
                Modifier.weight(1f),
            )
            NumeroCabecalho("Gasto", formatarMoeda(uiState.custoTotal), marca.sobreCabecalho, Modifier.weight(1.4f))
        }
    }
}

@Composable
private fun NumeroCabecalho(rotulo: String, valor: String, cor: Color, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(MaterialTheme.shapes.medium)
            .background(Color.White.copy(alpha = 0.08f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(valor, style = MaterialTheme.typography.titleMedium, color = cor, maxLines = 1)
        Text(rotulo, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.marca.sobreCabecalhoSuave)
    }
}

@Composable
private fun MenuBike(
    bike: Bike,
    onEditar: () -> Unit,
    onAtualizarKm: () -> Unit,
    onAlternarAtiva: () -> Unit,
    onExcluir: () -> Unit,
) {
    var aberto by remember { mutableStateOf(false) }
    IconButton(onClick = { aberto = true }) { Icon(Icons.Default.MoreVert, contentDescription = "Mais opções") }
    DropdownMenu(expanded = aberto, onDismissRequest = { aberto = false }) {
        DropdownMenuItem(
            text = { Text("Editar / foto") },
            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
            onClick = { aberto = false; onEditar() },
        )
        // Bikes do Strava recebem o km na sincronização e são removidas por lá.
        if (!bike.doStrava) {
            DropdownMenuItem(
                text = { Text("Atualizar km") },
                leadingIcon = { Icon(Icons.Default.Speed, contentDescription = null) },
                onClick = { aberto = false; onAtualizarKm() },
            )
        }
        DropdownMenuItem(
            text = { Text(if (bike.ativa) "Inativar bike" else "Reativar bike") },
            leadingIcon = {
                Icon(if (bike.ativa) Icons.Default.Archive else Icons.Default.Unarchive, contentDescription = null)
            },
            onClick = { aberto = false; onAlternarAtiva() },
        )
        if (!bike.doStrava) {
            DropdownMenuItem(
                text = { Text("Excluir bike") },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                onClick = { aberto = false; onExcluir() },
            )
        }
    }
}

@Composable
private fun DialogoAtualizarKm(kmAtual: Double, onConfirmar: (Int) -> Unit, onFechar: () -> Unit) {
    var texto by rememberSaveable { mutableStateOf(kmAtual.roundToLong().toString()) }
    val km = lerInteiro(texto)
    AlertDialog(
        onDismissRequest = onFechar,
        title = { Text("Quilometragem atual") },
        text = {
            OutlinedTextField(
                value = texto,
                onValueChange = { texto = it.filter(Char::isDigit) },
                label = { Text("km no odômetro") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        },
        confirmButton = { TextButton(onClick = { km?.let(onConfirmar) }, enabled = km != null) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = onFechar) { Text("Cancelar") } },
    )
}
