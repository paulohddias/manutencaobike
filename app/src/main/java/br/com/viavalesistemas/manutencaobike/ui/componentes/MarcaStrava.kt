package br.com.viavalesistemas.manutencaobike.ui.componentes

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import br.com.viavalesistemas.manutencaobike.R

/*
 * Artes oficiais do Strava (developers.strava.com/guidelines). As diretrizes proíbem alterar as
 * imagens e exigem que o botão de login seja o "Connect with Strava" oficial.
 */

@Composable
fun BotaoConectarStrava(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.strava_botao_conectar),
        contentDescription = "Conectar com o Strava",
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(6.dp))
            .clickable(role = Role.Button, onClick = onClick),
    )
}

/** Atribuição "Powered by Strava"; [sobreFundoEscuro] escolhe a versão branca ou a laranja/preta. */
@Composable
fun PoweredByStrava(sobreFundoEscuro: Boolean, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(
            if (sobreFundoEscuro) R.drawable.strava_powered_by_branco else R.drawable.strava_powered_by_laranja,
        ),
        contentDescription = "Powered by Strava",
        modifier = modifier.height(14.dp).sizeIn(maxWidth = 160.dp),
    )
}
