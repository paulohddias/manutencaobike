package br.com.viavalesistemas.manutencaobike.ui.bikedetalhe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.viavalesistemas.manutencaobike.model.DesgastePeca
import br.com.viavalesistemas.manutencaobike.ui.formatarData
import br.com.viavalesistemas.manutencaobike.ui.formatarKm
import br.com.viavalesistemas.manutencaobike.ui.formatarMoeda
import br.com.viavalesistemas.manutencaobike.ui.formatarPercentual

@Composable
fun AbaHistorico(uiState: BikeDetalheUiState) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item { TituloSecao("Custos") }
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LinhaCusto("Peças", uiState.custoPecas)
                    LinhaCusto("Serviços", uiState.custoServicos)
                    HorizontalDivider()
                    LinhaCusto("Total", uiState.custoTotal, destaque = true)
                }
            }
        }
        item { TituloSecao("Peças substituídas") }
        if (uiState.pecasRemovidas.isEmpty()) {
            item { TextoVazio("Quando você trocar uma peça, verá aqui quantos km ela durou de verdade.") }
        }
        items(uiState.pecasRemovidas, key = { it.peca.id }) { ItemPecaRemovida(it) }
    }
}

@Composable
private fun LinhaCusto(rotulo: String, centavos: Long, destaque: Boolean = false) {
    val estilo = if (destaque) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(rotulo, style = estilo)
        Text(formatarMoeda(centavos), style = estilo, fontWeight = if (destaque) FontWeight.Bold else null)
    }
}

@Composable
private fun ItemPecaRemovida(desgaste: DesgastePeca) {
    val peca = desgaste.peca
    ElevatedCard(Modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = { Text(peca.tipoNome + (peca.descricao?.let { " · $it" } ?: "")) },
            supportingContent = {
                Column {
                    Text("Durou ${formatarKm(desgaste.kmRodados)} (${formatarPercentual(desgaste.fracao)} da vida útil prevista)")
                    Text(
                        "${formatarData(peca.dataInstalacao)} → ${peca.dataRemocao?.let(::formatarData).orEmpty()}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            },
            trailingContent = peca.custoCentavos?.let { { Text(formatarMoeda(it)) } },
        )
    }
}
