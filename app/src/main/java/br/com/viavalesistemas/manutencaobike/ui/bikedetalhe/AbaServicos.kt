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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.viavalesistemas.manutencaobike.model.Servico
import br.com.viavalesistemas.manutencaobike.model.SituacaoServico
import br.com.viavalesistemas.manutencaobike.ui.componentes.BarraDesgaste
import br.com.viavalesistemas.manutencaobike.ui.componentes.Confirmacao
import br.com.viavalesistemas.manutencaobike.ui.componentes.RotuloStatus
import br.com.viavalesistemas.manutencaobike.ui.formatarData
import br.com.viavalesistemas.manutencaobike.ui.formatarKm
import br.com.viavalesistemas.manutencaobike.ui.formatarMoeda

@Composable
fun AbaServicos(
    uiState: BikeDetalheUiState,
    actions: BikeDetalheActions,
    kmAtual: Double,
    onPedirConfirmacao: (Confirmacao) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (uiState.situacoesServico.isNotEmpty()) {
            item { TituloSecao("Próximos serviços") }
            items(uiState.situacoesServico, key = { "situacao-${it.tipo.id}" }) { CartaoSituacao(it, kmAtual) }
        }
        item { TituloSecao("Serviços realizados") }
        if (uiState.servicos.isEmpty()) {
            item { TextoVazio("Nenhum serviço registrado ainda.") }
        }
        items(uiState.servicos, key = { "servico-${it.id}" }) { servico ->
            ItemServico(
                servico = servico,
                onExcluir = {
                    onPedirConfirmacao(
                        Confirmacao(
                            titulo = "Excluir serviço?",
                            texto = "${servico.tipoNome} de ${formatarData(servico.data)} será removido.",
                            acao = "Excluir",
                            onConfirmar = { actions.onExcluirServico(servico) },
                        ),
                    )
                },
            )
        }
    }
}

@Composable
private fun CartaoSituacao(situacao: SituacaoServico, kmAtual: Double) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(situacao.tipo.nome, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (situacao.ultimo != null) RotuloStatus(situacao.status, textoTrocar = "Vencido")
            }
            val ultimo = situacao.ultimo
            if (ultimo == null) {
                Text(
                    "Sem registro. Registre o último ${situacao.tipo.nome.lowercase()} para receber lembretes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }
            Spacer(Modifier.height(8.dp))
            situacao.fracao?.let { BarraDesgaste(it, situacao.status) }
            Spacer(Modifier.height(8.dp))
            Text(
                "Último: ${formatarData(ultimo.data)} com ${formatarKm(ultimo.km)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(textoProximo(situacao, kmAtual), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun textoProximo(situacao: SituacaoServico, kmAtual: Double): String {
    val partes = buildList {
        situacao.proximoKm?.let { proximo ->
            val faltam = proximo - kmAtual
            add(if (faltam >= 0) "em ${formatarKm(faltam)}" else "passou ${formatarKm(-faltam)}")
        }
        situacao.proximaData?.let { add("até ${formatarData(it)}") }
    }
    return "Próximo: " + partes.joinToString(" ou ")
}

@Composable
private fun ItemServico(servico: Servico, onExcluir: () -> Unit) {
    val detalhes = buildList {
        add(formatarData(servico.data))
        add(formatarKm(servico.km))
        servico.custoCentavos?.let { add(formatarMoeda(it)) }
    }.joinToString(" · ")
    ElevatedCard(Modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = { Text(servico.tipoNome) },
            supportingContent = {
                Column {
                    Text(detalhes)
                    servico.observacoes?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                }
            },
            trailingContent = {
                IconButton(onClick = onExcluir) { Icon(Icons.Default.Delete, contentDescription = "Excluir serviço") }
            },
        )
    }
}

@Composable
fun TituloSecao(texto: String) {
    Text(
        texto,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp),
    )
}
