package br.com.viavalesistemas.manutencaobike.ui.componentes

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import br.com.viavalesistemas.manutencaobike.model.StatusDesgaste

private val CorAtencao = Color(0xFFE6A100)

@Composable
fun corDoStatus(status: StatusDesgaste): Color = when (status) {
    StatusDesgaste.OK -> MaterialTheme.colorScheme.primary
    StatusDesgaste.ATENCAO -> CorAtencao
    StatusDesgaste.TROCAR -> MaterialTheme.colorScheme.error
}

@Composable
fun BarraDesgaste(fracao: Double, status: StatusDesgaste, modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        progress = { fracao.toFloat().coerceIn(0f, 1f) },
        color = corDoStatus(status),
        strokeCap = StrokeCap.Round,
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp),
    )
}

@Composable
fun RotuloStatus(status: StatusDesgaste, textoTrocar: String = "Trocar", modifier: Modifier = Modifier) {
    val texto = when (status) {
        StatusDesgaste.OK -> "OK"
        StatusDesgaste.ATENCAO -> "Atenção"
        StatusDesgaste.TROCAR -> textoTrocar
    }
    val cor = corDoStatus(status)
    Surface(
        color = cor.copy(alpha = 0.15f),
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
