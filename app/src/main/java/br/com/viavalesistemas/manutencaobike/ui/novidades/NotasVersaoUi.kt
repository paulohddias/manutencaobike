package br.com.viavalesistemas.manutencaobike.ui.novidades

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.com.viavalesistemas.manutencaobike.model.NotasVersao
import br.com.viavalesistemas.manutencaobike.ui.formatarData
import br.com.viavalesistemas.manutencaobike.ui.theme.marca
import java.time.LocalDate

/** Folha "O que há de novo" mostrada uma vez depois de cada atualização. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolhaNovidades(versoes: List<NotasVersao>, onFechar: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onFechar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            Text("O que há de novo", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Versão ${versoes.first().versao}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            versoes.forEachIndexed { indice, notas ->
                // Se o usuário pulou versões, as anteriores aparecem abaixo com o próprio título.
                if (indice > 0) {
                    HorizontalDivider(Modifier.padding(vertical = 16.dp))
                    Text("Versão ${notas.versao}", style = MaterialTheme.typography.titleMedium)
                }
                ConteudoNotas(notas, Modifier.padding(top = 16.dp))
            }
            Button(onClick = onFechar, modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) { Text("Entendi") }
        }
    }
}

@Composable
fun ConteudoNotas(notas: NotasVersao, modifier: Modifier = Modifier) {
    val marca = MaterialTheme.marca
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SecaoNotas("Novidades", Icons.Default.AutoAwesome, MaterialTheme.colorScheme.primary, notas.novidades)
        SecaoNotas("Correções", Icons.Default.BugReport, marca.atencao, notas.correcoes)
        SecaoNotas("Melhorias técnicas", Icons.Default.Engineering, MaterialTheme.colorScheme.secondary, notas.melhorias)
    }
}

@Composable
private fun SecaoNotas(titulo: String, icone: ImageVector, cor: Color, itens: List<String>) {
    if (itens.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icone, contentDescription = null, tint = cor, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(titulo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = cor)
        }
        itens.forEach { item ->
            Row {
                Text("•", modifier = Modifier.width(20.dp), textAlign = TextAlign.Center)
                Text(item, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            }
        }
    }
}

fun dataDasNotas(notas: NotasVersao): String =
    runCatching { formatarData(LocalDate.parse(notas.data)) }.getOrDefault(notas.data)
