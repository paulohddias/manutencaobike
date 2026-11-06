package br.com.viavalesistemas.manutencaobike.ui.componentes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import br.com.viavalesistemas.manutencaobike.model.StatusDesgaste
import br.com.viavalesistemas.manutencaobike.ui.formatarPercentual
import br.com.viavalesistemas.manutencaobike.ui.theme.marca

@Composable
fun corDoStatus(status: StatusDesgaste): Color = when (status) {
    StatusDesgaste.OK -> MaterialTheme.marca.ok
    StatusDesgaste.ATENCAO -> MaterialTheme.marca.atencao
    StatusDesgaste.TROCAR -> MaterialTheme.marca.trocar
}

@Composable
fun BarraDesgaste(fracao: Double, status: StatusDesgaste, modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        progress = { fracao.toFloat().coerceIn(0f, 1f) },
        color = corDoStatus(status),
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
        strokeCap = StrokeCap.Round,
        gapSize = 0.dp,
        drawStopIndicator = {},
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp),
    )
}

/** Anel de desgaste com o percentual no centro. */
@Composable
fun AnelDesgaste(fracao: Double, status: StatusDesgaste, modifier: Modifier = Modifier, tamanho: Dp = 56.dp) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(tamanho)
            .semantics { contentDescription = "Desgaste ${formatarPercentual(fracao)}" },
    ) {
        CircularProgressIndicator(
            progress = { fracao.toFloat().coerceIn(0f, 1f) },
            color = corDoStatus(status),
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeWidth = 6.dp,
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp,
            modifier = Modifier.size(tamanho),
        )
        Text(formatarPercentual(fracao), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun RotuloStatus(status: StatusDesgaste, modifier: Modifier = Modifier, textoTrocar: String = "Trocar") {
    val texto = when (status) {
        StatusDesgaste.OK -> "OK"
        StatusDesgaste.ATENCAO -> "Atenção"
        StatusDesgaste.TROCAR -> textoTrocar
    }
    Pilula(texto = texto, cor = corDoStatus(status), modifier = modifier)
}

@Composable
fun Pilula(texto: String, cor: Color, modifier: Modifier = Modifier) {
    Surface(
        color = cor.copy(alpha = 0.16f),
        contentColor = cor,
        shape = RoundedCornerShape(50),
        modifier = modifier,
    ) {
        Text(
            text = texto,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}
