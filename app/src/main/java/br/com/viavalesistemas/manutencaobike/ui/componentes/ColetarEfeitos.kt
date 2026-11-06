package br.com.viavalesistemas.manutencaobike.ui.componentes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow

val LocalSnackbarHostState = staticCompositionLocalOf<SnackbarHostState> {
    error("SnackbarHostState não fornecido")
}

@Composable
fun <T> ColetarEfeitos(efeitos: Flow<T>, onEfeito: suspend (T) -> Unit) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val onEfeitoAtual by rememberUpdatedState(onEfeito)
    LaunchedEffect(efeitos, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            efeitos.collect { onEfeitoAtual(it) }
        }
    }
}
