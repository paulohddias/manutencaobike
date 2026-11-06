package br.com.viavalesistemas.manutencaobike.ui.navegacao

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import br.com.viavalesistemas.manutencaobike.ui.bikedetalhe.BikeDetalheScreen
import br.com.viavalesistemas.manutencaobike.ui.bikeform.BikeFormScreen
import br.com.viavalesistemas.manutencaobike.ui.bikes.BikesScreen
import br.com.viavalesistemas.manutencaobike.ui.catalogo.CatalogoScreen
import br.com.viavalesistemas.manutencaobike.ui.componentes.ColetarEfeitos
import br.com.viavalesistemas.manutencaobike.ui.componentes.LocalSnackbarHostState
import br.com.viavalesistemas.manutencaobike.ui.pecaform.PecaFormScreen
import br.com.viavalesistemas.manutencaobike.ui.servicoform.ServicoFormScreen
import kotlinx.coroutines.flow.Flow

@Composable
fun AppNavHost(mensagensGlobais: Flow<String>) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    ColetarEfeitos(mensagensGlobais) { snackbarHostState.showSnackbar(it) }

    val voltar: () -> Unit = { navController.navigateUp() }

    CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
        NavHost(navController = navController, startDestination = RotaBikes) {
            composable<RotaBikes> {
                BikesScreen(
                    onAbrirBike = { navController.navigate(RotaBikeDetalhe(it)) },
                    onAdicionarBike = { navController.navigate(RotaBikeForm()) },
                    onAbrirCatalogo = { navController.navigate(RotaCatalogo) },
                )
            }
            composable<RotaBikeForm> { BikeFormScreen(onVoltar = voltar) }
            composable<RotaBikeDetalhe> {
                BikeDetalheScreen(
                    onVoltar = voltar,
                    onEditarBike = { navController.navigate(RotaBikeForm(it)) },
                    onInstalarPeca = { bikeId, tipoPecaId -> navController.navigate(RotaPecaForm(bikeId, tipoPecaId)) },
                    onRegistrarServico = { navController.navigate(RotaServicoForm(it)) },
                )
            }
            composable<RotaPecaForm> { PecaFormScreen(onVoltar = voltar) }
            composable<RotaServicoForm> { ServicoFormScreen(onVoltar = voltar) }
            composable<RotaCatalogo> { CatalogoScreen(onVoltar = voltar) }
        }
    }
}
