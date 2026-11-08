package br.com.viavalesistemas.manutencaobike.ui.bikes

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.viavalesistemas.manutencaobike.R
import br.com.viavalesistemas.manutencaobike.model.Bike
import br.com.viavalesistemas.manutencaobike.model.StatusDesgaste
import br.com.viavalesistemas.manutencaobike.ui.componentes.AnelDesgaste
import br.com.viavalesistemas.manutencaobike.ui.componentes.AvatarBike
import br.com.viavalesistemas.manutencaobike.ui.componentes.ColetarEfeitos
import br.com.viavalesistemas.manutencaobike.ui.componentes.LocalSnackbarHostState
import br.com.viavalesistemas.manutencaobike.ui.componentes.Pilula
import br.com.viavalesistemas.manutencaobike.ui.componentes.corDoStatus
import br.com.viavalesistemas.manutencaobike.ui.formatarKm
import br.com.viavalesistemas.manutencaobike.ui.theme.ManutencaobikeTheme
import br.com.viavalesistemas.manutencaobike.ui.theme.marca

@Composable
fun BikesScreen(
    onAbrirBike: (Long) -> Unit,
    onAdicionarBike: () -> Unit,
    onAbrirCatalogo: () -> Unit,
    onAbrirBackup: () -> Unit,
    onAbrirNovidades: () -> Unit,
    viewModel: BikesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbarHostState.current
    val context = LocalContext.current

    ColetarEfeitos(viewModel.efeitos) { efeito ->
        when (efeito) {
            is BikesEfeito.AbrirNavegador -> try {
                context.startActivity(Intent(Intent.ACTION_VIEW, efeito.url.toUri()))
            } catch (e: ActivityNotFoundException) {
                snackbar.showSnackbar("Nenhum navegador encontrado para abrir o Strava.")
            }
            is BikesEfeito.Mensagem -> snackbar.showSnackbar(efeito.texto)
        }
    }
    PedirPermissaoNotificacao()

    BikesContent(
        uiState = uiState,
        onAbrirBike = onAbrirBike,
        onAdicionarBike = onAdicionarBike,
        onAbrirCatalogo = onAbrirCatalogo,
        onAbrirBackup = onAbrirBackup,
        onAbrirNovidades = onAbrirNovidades,
        onConectarStrava = viewModel::onConectarStrava,
        onSincronizar = viewModel::onSincronizar,
        onDesconectarStrava = viewModel::onDesconectarStrava,
    )
}

@Composable
private fun BikesContent(
    uiState: BikesUiState,
    onAbrirBike: (Long) -> Unit,
    onAdicionarBike: () -> Unit,
    onAbrirCatalogo: () -> Unit,
    onAbrirBackup: () -> Unit,
    onAbrirNovidades: () -> Unit,
    onConectarStrava: () -> Unit,
    onSincronizar: () -> Unit,
    onDesconectarStrava: () -> Unit,
) {
    var mostrarInativas by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        // O cabeçalho desenha por trás da status bar; cada parte trata o próprio inset.
        contentWindowInsets = WindowInsets(0),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdicionarBike,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Nova bike") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.navigationBarsPadding(),
            )
        },
        snackbarHost = { SnackbarHost(LocalSnackbarHostState.current, Modifier.navigationBarsPadding()) },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            item {
                Cabecalho(
                    uiState = uiState,
                    onAbrirCatalogo = onAbrirCatalogo,
                    onAbrirBackup = onAbrirBackup,
                    onAbrirNovidades = onAbrirNovidades,
                    onConectarStrava = onConectarStrava,
                    onSincronizar = onSincronizar,
                    onDesconectarStrava = onDesconectarStrava,
                )
            }
            when {
                uiState.carregando -> item {
                    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                uiState.bikes.isEmpty() && uiState.bikesInativas.isEmpty() -> item { EstadoVazio() }
                else -> {
                    item {
                        Text(
                            "Suas bikes",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp),
                        )
                    }
                    if (uiState.bikes.isEmpty()) {
                        item {
                            Text(
                                "Nenhuma bike ativa no momento.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 20.dp),
                            )
                        }
                    }
                    items(uiState.bikes, key = { it.bike.id }) { resumo ->
                        CartaoBike(
                            resumo = resumo,
                            onClick = { onAbrirBike(resumo.bike.id) },
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                    if (uiState.bikesInativas.isNotEmpty()) {
                        item(key = "titulo_inativas") {
                            TituloInativas(
                                quantidade = uiState.bikesInativas.size,
                                aberto = mostrarInativas,
                                onClick = { mostrarInativas = !mostrarInativas },
                            )
                        }
                        if (mostrarInativas) {
                            items(uiState.bikesInativas, key = { it.bike.id }) { resumo ->
                                CartaoBike(
                                    resumo = resumo,
                                    onClick = { onAbrirBike(resumo.bike.id) },
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Cabecalho(
    uiState: BikesUiState,
    onAbrirCatalogo: () -> Unit,
    onAbrirBackup: () -> Unit,
    onAbrirNovidades: () -> Unit,
    onConectarStrava: () -> Unit,
    onSincronizar: () -> Unit,
    onDesconectarStrava: () -> Unit,
) {
    val marca = MaterialTheme.marca
    var menuAberto by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(Brush.verticalGradient(listOf(marca.cabecalho, marca.cabecalhoFim)))
            .statusBarsPadding()
            .padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.logo_marca),
                contentDescription = "Logo VVS",
                modifier = Modifier.size(44.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Manutenção Bike", style = MaterialTheme.typography.titleLarge, color = marca.sobreCabecalho)
                Text(
                    uiState.nomeAtleta?.let { "Olá, ${it.substringBefore(' ')}!" } ?: "Sua bike sempre em dia",
                    style = MaterialTheme.typography.bodyMedium,
                    color = marca.sobreCabecalhoSuave,
                )
            }
            if (uiState.stravaConectado) {
                if (uiState.sincronizando) {
                    CircularProgressIndicator(
                        color = marca.sobreCabecalho,
                        strokeWidth = 2.dp,
                        modifier = Modifier.padding(12.dp).size(24.dp),
                    )
                } else {
                    IconButton(onClick = onSincronizar) {
                        Icon(Icons.Default.Sync, contentDescription = "Sincronizar com o Strava", tint = marca.sobreCabecalho)
                    }
                }
            }
            Box {
                IconButton(onClick = { menuAberto = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Mais opções", tint = marca.sobreCabecalho)
                }
                DropdownMenu(expanded = menuAberto, onDismissRequest = { menuAberto = false }) {
                    DropdownMenuItem(
                        text = { Text("Tipos de peças e serviços") },
                        leadingIcon = { Icon(Icons.Default.Category, contentDescription = null) },
                        onClick = { menuAberto = false; onAbrirCatalogo() },
                    )
                    DropdownMenuItem(
                        text = { Text("Backup na nuvem") },
                        leadingIcon = { Icon(Icons.Default.CloudUpload, contentDescription = null) },
                        onClick = { menuAberto = false; onAbrirBackup() },
                    )
                    DropdownMenuItem(
                        text = { Text("Novidades") },
                        leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                        onClick = { menuAberto = false; onAbrirNovidades() },
                    )
                    if (uiState.stravaConectado) {
                        DropdownMenuItem(
                            text = { Text("Desconectar do Strava") },
                            leadingIcon = { Icon(Icons.Default.LinkOff, contentDescription = null) },
                            onClick = { menuAberto = false; onDesconectarStrava() },
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Row(Modifier.padding(end = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Indicador("Bikes", uiState.bikes.size.toString(), marca.sobreCabecalho, Modifier.weight(1f))
            Indicador("Para trocar", uiState.bikes.sumOf { it.itensTrocar }.toString(), marca.trocar, Modifier.weight(1f))
            Indicador("Atenção", uiState.bikes.sumOf { it.itensAtencao }.toString(), marca.atencao, Modifier.weight(1f))
        }

        Spacer(Modifier.height(16.dp))
        Box(Modifier.padding(end = 12.dp)) {
            when {
                uiState.stravaConectado -> Pilula("● Strava conectado", marca.strava)
                uiState.stravaConfigurado -> Button(
                    onClick = onConectarStrava,
                    colors = ButtonDefaults.buttonColors(containerColor = marca.strava, contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) { Text("Conectar com o Strava") }
                else -> Text(
                    "Para importar do Strava, preencha strava.clientId no local.properties.",
                    style = MaterialTheme.typography.bodySmall,
                    color = marca.sobreCabecalhoSuave,
                )
            }
        }
    }
}

@Composable
private fun Indicador(rotulo: String, valor: String, cor: Color, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(MaterialTheme.shapes.medium)
            .background(Color.White.copy(alpha = 0.08f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(valor, style = MaterialTheme.typography.headlineSmall, color = cor)
        Text(rotulo, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.marca.sobreCabecalhoSuave)
    }
}

@Composable
private fun CartaoBike(resumo: BikeResumo, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val bike = resumo.bike
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier
                .padding(16.dp)
                .alpha(if (bike.ativa) 1f else 0.6f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AvatarBike(resumo.foto, tamanho = 56.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(bike.nome, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                bike.descricao?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Pilula(formatarKm(bike.kmAtual), MaterialTheme.colorScheme.secondary)
                    if (bike.doStrava) Pilula("Strava", MaterialTheme.marca.strava)
                }
            }
            Spacer(Modifier.width(12.dp))
            if (!bike.ativa) {
                Pilula("Inativa", MaterialTheme.colorScheme.outline)
            } else if (resumo.temItensAcompanhados) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AnelDesgaste(resumo.maiorDesgaste, resumo.status)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        textoSituacao(resumo),
                        style = MaterialTheme.typography.labelSmall,
                        color = corDoStatus(resumo.status),
                    )
                }
            } else {
                Text(
                    "Sem peças\ncadastradas",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

private fun textoSituacao(resumo: BikeResumo): String = when {
    resumo.itensTrocar > 0 -> "${resumo.itensTrocar} vencido(s)"
    resumo.itensAtencao > 0 -> "Atenção"
    else -> "Em dia"
}

@Composable
private fun TituloInativas(quantidade: Int, aberto: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = if (aberto) "Ocultar inativas" else "Mostrar inativas", onClick = onClick)
            .padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 4.dp),
    ) {
        Text(
            "Inativas ($quantidade)",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Icon(
            if (aberto) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(12.dp),
        )
    }
}

@Composable
private fun EstadoVazio() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 40.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.DirectionsBike,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(48.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text("Nenhuma bike ainda", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(4.dp))
        Text(
            "Conecte o Strava para importar suas bikes ou toque em \"Nova bike\" para cadastrar manualmente.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PedirPermissaoNotificacao() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        val permissao = Manifest.permission.POST_NOTIFICATIONS
        if (ContextCompat.checkSelfPermission(context, permissao) != PackageManager.PERMISSION_GRANTED) {
            launcher.launch(permissao)
        }
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun BikesContentPreview() {
    ManutencaobikeTheme {
        CompositionLocalProvider(LocalSnackbarHostState provides remember { SnackbarHostState() }) {
            BikesContent(
                uiState = BikesUiState(
                    carregando = false,
                    stravaConfigurado = true,
                    stravaConectado = true,
                    nomeAtleta = "Paulo Dias",
                    bikes = listOf(
                        BikeResumo(Bike(1, "Speed", "Specialized", "Allez", 8_450.0, "b1"), 1, 0, 1.05),
                        BikeResumo(Bike(2, "MTB", "Caloi", "Elite", 2_130.0), 0, 2, 0.86),
                        BikeResumo(Bike(3, "Urbana", kmAtual = 640.0), 0, 0, 0.32),
                    ),
                    bikesInativas = listOf(BikeResumo(Bike(4, "Antiga", kmAtual = 12_000.0, ativa = false), 0, 0)),
                ),
                onAbrirBike = {}, onAdicionarBike = {}, onAbrirCatalogo = {}, onAbrirBackup = {}, onAbrirNovidades = {},
                onConectarStrava = {}, onSincronizar = {}, onDesconectarStrava = {},
            )
        }
    }
}
