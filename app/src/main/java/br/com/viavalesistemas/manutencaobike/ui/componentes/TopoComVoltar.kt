package br.com.viavalesistemas.manutencaobike.ui.componentes

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextOverflow
import br.com.viavalesistemas.manutencaobike.ui.theme.marca

/** Barra superior na cor escura da marca, igual em todas as telas internas. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopoComVoltar(titulo: String, onVoltar: () -> Unit, acoes: @Composable RowScope.() -> Unit = {}) {
    val marca = MaterialTheme.marca
    TopAppBar(
        title = { Text(titulo, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = {
            IconButton(onClick = onVoltar) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
            }
        },
        actions = acoes,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = marca.cabecalho,
            titleContentColor = marca.sobreCabecalho,
            navigationIconContentColor = marca.sobreCabecalho,
            actionIconContentColor = marca.sobreCabecalho,
        ),
    )
}
