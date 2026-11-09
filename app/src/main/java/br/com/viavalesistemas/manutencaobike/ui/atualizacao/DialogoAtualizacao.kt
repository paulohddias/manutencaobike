package br.com.viavalesistemas.manutencaobike.ui.atualizacao

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri

@Composable
fun DialogoAtualizacao(onAtualizar: () -> Unit, onDepois: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDepois,
        icon = { Icon(Icons.Default.SystemUpdate, contentDescription = null) },
        title = { Text("Atualização disponível") },
        text = { Text("Uma nova versão do Manutenção Bike está disponível na Play Store, com melhorias e correções.") },
        confirmButton = {
            Button(onClick = {
                abrirPlayStore(context)
                onAtualizar()
            }) { Text("Atualizar") }
        },
        dismissButton = { TextButton(onClick = onDepois) { Text("Agora não") } },
    )
}

// Sempre a página do próprio app: nunca um link vindo do servidor de versões.
private fun abrirPlayStore(context: Context) {
    val pacote = context.packageName
    val intent = Intent(Intent.ACTION_VIEW, "market://details?id=$pacote".toUri())
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        context.startActivity(Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=$pacote".toUri()))
    }
}
