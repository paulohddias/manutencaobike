package br.com.viavalesistemas.manutencaobike.ui.bikes

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.window.DialogProperties
import br.com.viavalesistemas.manutencaobike.data.repositorio.BikeExcluida

@Composable
fun DialogoBikesExcluidas(
    bikes: List<BikeExcluida>,
    onTrazerDeVolta: (List<String>) -> Unit,
    onManterExcluidas: () -> Unit,
) {
    // Nenhuma marcada de início: trazer de volta precisa ser uma escolha consciente.
    var marcadas by rememberSaveable { mutableStateOf(emptyList<String>()) }
    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        icon = { Icon(Icons.Default.Restore, contentDescription = null) },
        title = { Text("Bikes excluídas") },
        text = {
            Column {
                Text(
                    "Você excluiu estas bikes no app, mas elas continuam no seu Strava. " +
                        "Marque as que quer trazer de volta. Elas voltam sem o histórico de peças e serviços.",
                )
                bikes.forEach { bike ->
                    val marcada = bike.gearId in marcadas
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .toggleable(marcada, role = Role.Checkbox) {
                                marcadas = if (it) marcadas + bike.gearId else marcadas - bike.gearId
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = marcada, onCheckedChange = null)
                        Text(bike.nome, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onTrazerDeVolta(marcadas) }, enabled = marcadas.isNotEmpty()) { Text("Trazer de volta") }
        },
        dismissButton = { TextButton(onClick = onManterExcluidas) { Text("Manter excluídas") } },
    )
}
