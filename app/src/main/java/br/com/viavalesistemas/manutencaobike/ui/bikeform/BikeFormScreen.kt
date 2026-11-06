package br.com.viavalesistemas.manutencaobike.ui.bikeform

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.viavalesistemas.manutencaobike.ui.componentes.ColetarEfeitos
import br.com.viavalesistemas.manutencaobike.ui.componentes.LocalSnackbarHostState
import br.com.viavalesistemas.manutencaobike.ui.componentes.TopoComVoltar
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun BikeFormScreen(onVoltar: () -> Unit, viewModel: BikeFormViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbarHostState.current
    val escopo = rememberCoroutineScope()
    ColetarEfeitos(viewModel.efeitos) { efeito ->
        when (efeito) {
            BikeFormEfeito.Voltar -> onVoltar()
            is BikeFormEfeito.Mensagem -> snackbar.showSnackbar(efeito.texto)
        }
    }

    val galeria = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(viewModel::onFotoEscolhida)
    }
    // Sobrevive à recriação da Activity enquanto o app de câmera está aberto.
    var uriCaptura by rememberSaveable { mutableStateOf<String?>(null) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { tirou ->
        val uri = uriCaptura
        if (tirou && uri != null) viewModel.onFotoEscolhida(uri.toUri())
        uriCaptura = null
    }

    Scaffold(
        topBar = { TopoComVoltar(if (uiState.editando) "Editar bike" else "Nova bike", onVoltar) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            SeletorFoto(
                foto = uiState.foto,
                processando = uiState.processandoFoto,
                onGaleria = {
                    galeria.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                onCamera = {
                    val uri = viewModel.uriParaCamera()
                    uriCaptura = uri.toString()
                    try {
                        camera.launch(uri)
                    } catch (e: ActivityNotFoundException) {
                        uriCaptura = null
                        escopo.launch { snackbar.showSnackbar("Nenhum app de câmera encontrado.") }
                    }
                },
                onRemover = viewModel::onRemoverFoto,
            )
            OutlinedTextField(
                value = uiState.nome,
                onValueChange = viewModel::onNomeChange,
                label = { Text("Nome / apelido *") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = uiState.marca,
                onValueChange = viewModel::onMarcaChange,
                label = { Text("Marca") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = uiState.modelo,
                onValueChange = viewModel::onModeloChange,
                label = { Text("Modelo") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = uiState.km,
                onValueChange = viewModel::onKmChange,
                label = { Text("Quilometragem atual (km)") },
                enabled = uiState.kmEditavel,
                supportingText = if (!uiState.kmEditavel) {
                    { Text("Atualizada automaticamente pelo Strava") }
                } else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            LinhaAtiva(ativa = uiState.ativa, onChange = viewModel::onAtivaChange)
            Button(
                onClick = viewModel::onSalvar,
                enabled = uiState.podeSalvar,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) { Text("Salvar") }
        }
    }
}

@Composable
private fun SeletorFoto(
    foto: File?,
    processando: Boolean,
    onGaleria: () -> Unit,
    onCamera: () -> Unit,
    onRemover: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable(enabled = !processando, onClickLabel = "Escolher foto", onClick = onGaleria),
        ) {
            if (foto != null) {
                AsyncImage(
                    model = foto,
                    contentDescription = "Foto da bike",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.AutoMirrored.Filled.DirectionsBike,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Adicione uma foto da sua bike",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (processando) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)),
                ) { CircularProgressIndicator(color = Color.White) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onGaleria, enabled = !processando, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Galeria")
            }
            OutlinedButton(onClick = onCamera, enabled = !processando, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Câmera")
            }
            if (foto != null) {
                IconButton(onClick = onRemover, enabled = !processando) {
                    Icon(Icons.Default.Delete, contentDescription = "Remover foto")
                }
            }
        }
    }
}

@Composable
private fun LinhaAtiva(ativa: Boolean, onChange: (Boolean) -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        onClick = { onChange(!ativa) },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Bike ativa", style = MaterialTheme.typography.titleSmall)
                Text(
                    if (ativa) "Aparece na lista principal e gera alertas de manutenção"
                    else "Fica separada na lista e não gera alertas",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            Switch(checked = ativa, onCheckedChange = null)
        }
    }
}
