package br.com.viavalesistemas.manutencaobike.ui.componentes

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

data class Confirmacao(
    val titulo: String,
    val texto: String,
    val acao: String,
    val onConfirmar: () -> Unit,
)

@Composable
fun DialogoConfirmacao(confirmacao: Confirmacao, onFechar: () -> Unit) {
    AlertDialog(
        onDismissRequest = onFechar,
        title = { Text(confirmacao.titulo) },
        text = { Text(confirmacao.texto) },
        confirmButton = {
            TextButton(onClick = {
                confirmacao.onConfirmar()
                onFechar()
            }) { Text(confirmacao.acao, color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = onFechar) { Text("Cancelar") } },
    )
}
