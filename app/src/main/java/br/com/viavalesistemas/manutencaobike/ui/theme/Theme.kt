package br.com.viavalesistemas.manutencaobike.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val EsquemaClaro = lightColorScheme(
    primary = VerdeEscuro,
    onPrimary = Color.White,
    primaryContainer = VerdeContainerClaro,
    onPrimaryContainer = Color(0xFF0B3D1C),
    secondary = MarcaAzul,
    onSecondary = Color.White,
    secondaryContainer = AzulContainerClaro,
    onSecondaryContainer = Color(0xFF14245E),
    tertiary = MarcaArdosia,
    onTertiary = Color.White,
    background = Color(0xFFF3F5F8),
    onBackground = Color(0xFF17202B),
    surface = Color(0xFFF3F5F8),
    onSurface = Color(0xFF17202B),
    surfaceVariant = Color(0xFFE3E8EE),
    onSurfaceVariant = Color(0xFF526070),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color(0xFFF8FAFC),
    surfaceContainerHigh = Color(0xFFEDF1F5),
    surfaceContainerHighest = Color(0xFFE3E8EE),
    outline = Color(0xFFB4BFCB),
    outlineVariant = Color(0xFFDCE2E9),
    error = Vermelho,
)

private val EsquemaEscuro = darkColorScheme(
    primary = MarcaVerde,
    onPrimary = Color(0xFF06260F),
    primaryContainer = VerdeContainerEscuro,
    onPrimaryContainer = Color(0xFFB8F5C8),
    secondary = Color(0xFF8AA2F0),
    onSecondary = Color(0xFF0E1D55),
    secondaryContainer = AzulContainerEscuro,
    onSecondaryContainer = Color(0xFFDDE4FB),
    tertiary = Color(0xFFB9C6D4),
    onTertiary = MarcaArdosia,
    background = ArdosiaProfunda,
    onBackground = Color(0xFFE6EBF0),
    surface = ArdosiaProfunda,
    onSurface = Color(0xFFE6EBF0),
    surfaceVariant = ArdosiaClara,
    onSurfaceVariant = Color(0xFFA9B6C4),
    surfaceContainerLowest = Color(0xFF111821),
    surfaceContainerLow = ArdosiaMedia,
    surfaceContainer = ArdosiaMedia,
    surfaceContainerHigh = MarcaArdosia,
    surfaceContainerHighest = ArdosiaClara,
    outline = Color(0xFF5B6B7D),
    outlineVariant = ArdosiaClara,
    error = Color(0xFFFF6B6F),
)

private val Formas = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/** Cores da marca que não cabem no ColorScheme do Material: o cabeçalho escuro e os status de desgaste. */
@Immutable
data class CoresMarca(
    val cabecalho: Color,
    val cabecalhoFim: Color,
    val sobreCabecalho: Color,
    val sobreCabecalhoSuave: Color,
    val ok: Color,
    val atencao: Color,
    val trocar: Color,
    val strava: Color = Color(0xFFFC4C02),
)

private val MarcaClara = CoresMarca(
    cabecalho = MarcaArdosia,
    cabecalhoFim = Color(0xFF1F2A36),
    sobreCabecalho = Color.White,
    sobreCabecalhoSuave = Color(0xFFB9C6D4),
    ok = VerdeEscuro,
    atencao = Ambar,
    trocar = Vermelho,
)

private val MarcaEscura = MarcaClara.copy(
    cabecalho = MarcaArdosia,
    cabecalhoFim = ArdosiaMedia,
    ok = MarcaVerde,
    trocar = Color(0xFFFF6B6F),
)

private val LocalCoresMarca = staticCompositionLocalOf { MarcaClara }

val MaterialTheme.marca: CoresMarca
    @Composable @ReadOnlyComposable get() = LocalCoresMarca.current

// Sem cores dinâmicas (Material You): o app usa sempre a identidade da marca.
@Composable
fun ManutencaobikeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalCoresMarca provides if (darkTheme) MarcaEscura else MarcaClara) {
        MaterialTheme(
            colorScheme = if (darkTheme) EsquemaEscuro else EsquemaClaro,
            typography = Typography,
            shapes = Formas,
            content = content,
        )
    }
}
