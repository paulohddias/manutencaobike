package br.com.viavalesistemas.manutencaobike.ui.backup

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.viavalesistemas.manutencaobike.data.backup.BackupRemoto
import br.com.viavalesistemas.manutencaobike.ui.componentes.BotaoConectarStrava
import br.com.viavalesistemas.manutencaobike.ui.componentes.ColetarEfeitos
import br.com.viavalesistemas.manutencaobike.ui.componentes.Confirmacao
import br.com.viavalesistemas.manutencaobike.ui.componentes.DialogoConfirmacao
import br.com.viavalesistemas.manutencaobike.ui.componentes.LocalSnackbarHostState
import br.com.viavalesistemas.manutencaobike.ui.componentes.TopoComVoltar
import br.com.viavalesistemas.manutencaobike.ui.formatarDataHora
import br.com.viavalesistemas.manutencaobike.ui.formatarTamanho
import br.com.viavalesistemas.manutencaobike.ui.theme.ManutencaobikeTheme
import br.com.viavalesistemas.manutencaobike.ui.theme.marca
import java.time.Instant

@Composable
fun BackupScreen(onVoltar: () -> Unit, viewModel: BackupViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbarHostState.current
    val context = LocalContext.current
    ColetarEfeitos(viewModel.efeitos) { efeito ->
        when (efeito) {
            is BackupEfeito.Mensagem -> snackbar.showSnackbar(efeito.texto)
            is BackupEfeito.AbrirNavegador -> try {
                context.startActivity(Intent(Intent.ACTION_VIEW, efeito.url.toUri()))
            } catch (e: ActivityNotFoundException) {
                snackbar.showSnackbar("Nenhum navegador encontrado para abrir o Strava.")
            }
            BackupEfeito.ReiniciarApp -> reiniciarApp(context)
        }
    }
    BackupContent(uiState, viewModel, onVoltar)
}

/** O banco restaurado só entra em uso quando o processo recomeça (ver PacoteBackup). */
private fun reiniciarApp(context: Context) {
    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return
    context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
    (context as? Activity)?.finishAffinity()
    Runtime.getRuntime().exit(0)
}

@Composable
private fun BackupContent(uiState: BackupUiState, actions: BackupActions, onVoltar: () -> Unit) {
    var confirmacao by remember { mutableStateOf<Confirmacao?>(null) }

    Scaffold(
        topBar = {
            TopoComVoltar("Backup na nuvem", onVoltar) {
                if (uiState.conectado) {
                    IconButton(onClick = actions::onAtualizarLista, enabled = !uiState.carregandoLista) {
                        Icon(Icons.Default.Refresh, contentDescription = "Atualizar lista", tint = MaterialTheme.marca.sobreCabecalho)
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(LocalSnackbarHostState.current) },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            if (!uiState.conectado) {
                item { CartaoDesconectado(actions::onConectarStrava) }
                return@LazyColumn
            }
            item { CartaoStatus(uiState, actions) }
            item {
                Text(
                    "Backups salvos",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    "Guardamos os 5 mais recentes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            when {
                uiState.carregandoLista && uiState.backups.isEmpty() -> item {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                uiState.backups.isEmpty() -> item {
                    Text(
                        "Nenhum backup no servidor ainda.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> items(uiState.backups, key = { it.id }) { backup ->
                    ItemBackup(
                        backup = backup,
                        habilitado = !uiState.ocupado,
                        onRestaurar = {
                            confirmacao = Confirmacao(
                                titulo = "Restaurar backup?",
                                texto = "Os dados atuais deste celular (bikes, peças, serviços e fotos) serão " +
                                    "substituídos pelos do backup de ${formatarDataHora(backup.criadoEm)}. " +
                                    "O app será reiniciado.",
                                acao = "Restaurar",
                                onConfirmar = { actions.onRestaurar(backup) },
                            )
                        },
                    )
                }
            }
            item {
                TextButton(
                    onClick = {
                        confirmacao = Confirmacao(
                            titulo = "Apagar meus dados do servidor?",
                            texto = "Todos os seus backups serão apagados do servidor e o app será desconectado " +
                                "do Strava. Os dados deste celular não são afetados. Não dá para desfazer.",
                            acao = "Apagar",
                            onConfirmar = actions::onExcluirDados,
                        )
                    },
                    enabled = !uiState.ocupado,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.padding(top = 16.dp),
                ) { Text("Apagar meus dados do servidor") }
            }
        }
    }

    confirmacao?.let { DialogoConfirmacao(it, onFechar = { confirmacao = null }) }
}

@Composable
private fun CartaoDesconectado(onConectar: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.CloudOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text("Conecte o Strava para usar o backup", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Sua conta do Strava identifica você no servidor, assim seus backups podem ser restaurados " +
                    "em outro celular sem precisar de senha.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            BotaoConectarStrava(onConectar)
        }
    }
}

@Composable
private fun CartaoStatus(uiState: BackupUiState, actions: BackupActions) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (uiState.ultimoBackupEm != null) Icons.Default.CloudDone else Icons.Default.CloudUpload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Último backup deste celular", style = MaterialTheme.typography.labelMedium)
                    Text(
                        uiState.ultimoBackupEm?.let(::formatarDataHora) ?: "Nunca",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
            uiState.nomeAtleta?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Conta: $it (Strava)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = actions::onFazerBackup,
                enabled = !uiState.ocupado,
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                if (uiState.operacao == Operacao.ENVIANDO) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Enviando…")
                } else {
                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Fazer backup agora")
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Backup automático", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Uma vez por dia, quando houver alterações",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = uiState.automatico, onCheckedChange = actions::onAutomaticoChange)
            }
        }
    }
}

@Composable
private fun ItemBackup(backup: BackupRemoto, habilitado: Boolean, onRestaurar: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(formatarDataHora(backup.criadoEm), style = MaterialTheme.typography.titleSmall)
                Text(
                    formatarTamanho(backup.tamanhoBytes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedButton(onClick = onRestaurar, enabled = habilitado) {
                Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Restaurar")
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 700)
@Composable
private fun BackupContentPreview() {
    ManutencaobikeTheme {
        CompositionLocalProvider(LocalSnackbarHostState provides remember { SnackbarHostState() }) {
            BackupContent(
                uiState = BackupUiState(
                    conectado = true,
                    nomeAtleta = "Paulo Dias",
                    ultimoBackupEm = Instant.now(),
                    backups = listOf(BackupRemoto("1", Instant.now(), 2_400_000), BackupRemoto("2", Instant.now(), 2_100_000)),
                ),
                actions = object : BackupActions {
                    override fun onConectarStrava() = Unit
                    override fun onFazerBackup() = Unit
                    override fun onRestaurar(backup: BackupRemoto) = Unit
                    override fun onAutomaticoChange(ativo: Boolean) = Unit
                    override fun onAtualizarLista() = Unit
                    override fun onExcluirDados() = Unit
                },
                onVoltar = {},
            )
        }
    }
}
