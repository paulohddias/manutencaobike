package br.com.viavalesistemas.manutencaobike.ui.bikes

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.viavalesistemas.manutencaobike.model.Bike
import br.com.viavalesistemas.manutencaobike.model.StatusDesgaste
import br.com.viavalesistemas.manutencaobike.ui.componentes.ColetarEfeitos
import br.com.viavalesistemas.manutencaobike.ui.componentes.LocalSnackbarHostState
import br.com.viavalesistemas.manutencaobike.ui.componentes.RotuloStatus
import br.com.viavalesistemas.manutencaobike.ui.formatarKm
import br.com.viavalesistemas.manutencaobike.ui.theme.ManutencaobikeTheme

private val LaranjaStrava = Color(0xFFFC4C02)

@Composable
fun BikesScreen(
    onAbrirBike: (Long) -> Unit,
    onAdicionarBike: () -> Unit,
    onAbrirCatalogo: () -> Unit,
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
        onConectarStrava = viewModel::onConectarStrava,
        onSincronizar = viewModel::onSincronizar,
        onDesconectarStrava = viewModel::onDesconectarStrava,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BikesContent(
    uiState: BikesUiState,
    onAbrirBike: (Long) -> Unit,
    onAdicionarBike: () -> Unit,
    onAbrirCatalogo: () -> Unit,
    onConectarStrava: () -> Unit,
    onSincronizar: () -> Unit,
    onDesconectarStrava: () -> Unit,
) {
    var menuAberto by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Minhas bikes") },
                actions = {
                    if (uiState.stravaConectado) {
                        if (uiState.sincronizando) {
                            CircularProgressIndicator(Modifier.padding(12.dp).size(24.dp), strokeWidth = 2.dp)
                        } else {
                            IconButton(onClick = onSincronizar) {
                                Icon(Icons.Default.Sync, contentDescription = "Sincronizar com o Strava")
                            }
                        }
                    }
                    IconButton(onClick = { menuAberto = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Mais opções")
                    }
                    DropdownMenu(expanded = menuAberto, onDismissRequest = { menuAberto = false }) {
                        DropdownMenuItem(
                            text = { Text("Tipos de peças e serviços") },
                            leadingIcon = { Icon(Icons.Default.Category, contentDescription = null) },
                            onClick = {
                                menuAberto = false
                                onAbrirCatalogo()
                            },
                        )
                        if (uiState.stravaConectado) {
                            DropdownMenuItem(
                                text = { Text("Desconectar do Strava") },
                                onClick = {
                                    menuAberto = false
                                    onDesconectarStrava()
                                },
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdicionarBike,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Bike manual") },
            )
        },
        snackbarHost = { SnackbarHost(LocalSnackbarHostState.current) },
    ) { padding ->
        if (uiState.carregando) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            item {
                CartaoStrava(
                    configurado = uiState.stravaConfigurado,
                    conectado = uiState.stravaConectado,
                    nomeAtleta = uiState.nomeAtleta,
                    onConectar = onConectarStrava,
                )
            }
            if (uiState.bikes.isEmpty()) {
                item {
                    Text(
                        "Nenhuma bike ainda. Conecte o Strava para importar suas bikes ou cadastre uma manualmente.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                }
            }
            items(uiState.bikes, key = { it.bike.id }) { resumo ->
                CartaoBike(resumo = resumo, onClick = { onAbrirBike(resumo.bike.id) })
            }
        }
    }
}

@Composable
private fun CartaoStrava(configurado: Boolean, conectado: Boolean, nomeAtleta: String?, onConectar: () -> Unit) {
    when {
        conectado -> Text(
            text = "Strava conectado" + (nomeAtleta?.let { " · $it" } ?: ""),
            style = MaterialTheme.typography.labelLarge,
            color = LaranjaStrava,
        )
        !configurado -> Card(Modifier.fillMaxWidth()) {
            Text(
                "Para importar bikes do Strava, preencha strava.clientId e strava.clientSecret no local.properties e recompile o app.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp),
            )
        }
        else -> Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Importe suas bikes do Strava", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.size(4.dp))
                Text(
                    "A quilometragem é atualizada automaticamente para calcular o desgaste das peças.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.size(12.dp))
                Button(
                    onClick = onConectar,
                    colors = ButtonDefaults.buttonColors(containerColor = LaranjaStrava, contentColor = Color.White),
                ) { Text("Conectar com o Strava") }
            }
        }
    }
}

@Composable
private fun CartaoBike(resumo: BikeResumo, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.AutoMirrored.Filled.DirectionsBike,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp),
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(resumo.bike.nome, style = MaterialTheme.typography.titleMedium)
                resumo.bike.descricao?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    formatarKm(resumo.bike.kmAtual) + if (resumo.bike.doStrava) " · Strava" else "",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (resumo.itensTrocar > 0) RotuloStatus(StatusDesgaste.TROCAR, textoTrocar = "${resumo.itensTrocar} vencido(s)")
                if (resumo.itensAtencao > 0) RotuloStatus(StatusDesgaste.ATENCAO)
            }
        }
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

@Preview(showBackground = true)
@Composable
private fun BikesContentPreview() {
    ManutencaobikeTheme {
        androidx.compose.runtime.CompositionLocalProvider(
            LocalSnackbarHostState provides remember { androidx.compose.material3.SnackbarHostState() },
        ) {
            BikesContent(
                uiState = BikesUiState(
                    carregando = false,
                    stravaConfigurado = true,
                    bikes = listOf(
                        BikeResumo(Bike(1, "Speed", "Specialized", "Allez", 8_450.0, "b1"), itensTrocar = 1, itensAtencao = 0),
                        BikeResumo(Bike(2, "MTB", "Caloi", "Elite", 2_130.0), itensTrocar = 0, itensAtencao = 2),
                    ),
                ),
                onAbrirBike = {}, onAdicionarBike = {}, onAbrirCatalogo = {},
                onConectarStrava = {}, onSincronizar = {}, onDesconectarStrava = {},
            )
        }
    }
}
