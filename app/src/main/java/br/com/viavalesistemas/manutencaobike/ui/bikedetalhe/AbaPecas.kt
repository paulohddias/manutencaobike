package br.com.viavalesistemas.manutencaobike.ui.bikedetalhe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.viavalesistemas.manutencaobike.model.DesgastePeca
import br.com.viavalesistemas.manutencaobike.ui.componentes.BarraDesgaste
import br.com.viavalesistemas.manutencaobike.ui.componentes.Confirmacao
import br.com.viavalesistemas.manutencaobike.ui.componentes.RotuloStatus
import br.com.viavalesistemas.manutencaobike.ui.formatarData
import br.com.viavalesistemas.manutencaobike.ui.formatarKm
import br.com.viavalesistemas.manutencaobike.ui.formatarPercentual
import kotlin.math.abs

@Composable
fun AbaPecas(
    pecas: List<DesgastePeca>,
    actions: BikeDetalheActions,
    onPedirConfirmacao: (Confirmacao) -> Unit,
) {
    if (pecas.isEmpty()) {
        TextoVazio("Nenhuma peça cadastrada. Instale uma peça para acompanhar o desgaste pela quilometragem.")
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(pecas, key = { it.peca.id }) { desgaste ->
            CartaoPeca(
                desgaste = desgaste,
                onTrocar = {
                    onPedirConfirmacao(
                        Confirmacao(
                            titulo = "Trocar ${desgaste.peca.tipoNome}?",
                            texto = "A peça atual vai para o histórico com ${formatarKm(desgaste.kmRodados)} rodados e você cadastra a nova em seguida.",
                            acao = "Trocar",
                            onConfirmar = { actions.onTrocarPeca(desgaste.peca) },
                        ),
                    )
                },
                onRemover = { actions.onRemoverPeca(desgaste.peca) },
                onExcluir = {
                    onPedirConfirmacao(
                        Confirmacao(
                            titulo = "Excluir registro?",
                            texto = "Use só para corrigir um cadastro errado. Para registrar uma troca, use \"Trocar\".",
                            acao = "Excluir",
                            onConfirmar = { actions.onExcluirPeca(desgaste.peca) },
                        ),
                    )
                },
            )
        }
    }
}

@Composable
private fun CartaoPeca(desgaste: DesgastePeca, onTrocar: () -> Unit, onRemover: () -> Unit, onExcluir: () -> Unit) {
    val peca = desgaste.peca
    var menuAberto by remember { mutableStateOf(false) }

    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(peca.tipoNome, style = MaterialTheme.typography.titleMedium)
                    peca.descricao?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                RotuloStatus(desgaste.status)
                IconButton(onClick = { menuAberto = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Opções da peça")
                }
                DropdownMenu(expanded = menuAberto, onDismissRequest = { menuAberto = false }) {
                    DropdownMenuItem(text = { Text("Remover sem substituir") }, onClick = { menuAberto = false; onRemover() })
                    DropdownMenuItem(text = { Text("Excluir registro") }, onClick = { menuAberto = false; onExcluir() })
                }
            }
            Column(Modifier.padding(end = 12.dp)) {
                Spacer(Modifier.height(8.dp))
                BarraDesgaste(desgaste.fracao, desgaste.status)
                Spacer(Modifier.height(8.dp))
                Text(
                    "${formatarKm(desgaste.kmRodados)} de ${formatarKm(peca.vidaUtilKm.toDouble())} (${formatarPercentual(desgaste.fracao)})",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    textoRestante(desgaste.kmRestantes) + " · instalada em ${formatarData(peca.dataInstalacao)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                FilledTonalButton(onClick = onTrocar, modifier = Modifier.align(Alignment.End)) { Text("Trocar peça") }
            }
        }
    }
}

private fun textoRestante(kmRestantes: Double): String =
    if (kmRestantes >= 0) "Faltam ${formatarKm(kmRestantes)}" else "Passou ${formatarKm(abs(kmRestantes))} do limite"

@Composable
fun TextoVazio(texto: String) {
    Text(
        texto,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(24.dp),
    )
}
