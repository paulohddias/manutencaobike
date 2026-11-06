package br.com.viavalesistemas.manutencaobike.ui.componentes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import br.com.viavalesistemas.manutencaobike.ui.formatarData
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampoData(rotulo: String, data: LocalDate, onDataChange: (LocalDate) -> Unit, modifier: Modifier = Modifier) {
    var mostrarSeletor by rememberSaveable { mutableStateOf(false) }

    Box(modifier) {
        OutlinedTextField(
            value = formatarData(data),
            onValueChange = {},
            readOnly = true,
            label = { Text(rotulo) },
            trailingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
        )
        // Campo somente leitura não recebe clique; esta camada abre o calendário.
        Box(
            Modifier
                .matchParentSize()
                .clickable(role = Role.Button, onClickLabel = "Escolher data") { mostrarSeletor = true },
        )
    }

    if (mostrarSeletor) {
        // O DatePicker trabalha com milissegundos em UTC.
        val estado = rememberDatePickerState(
            initialSelectedDateMillis = data.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { mostrarSeletor = false },
            confirmButton = {
                TextButton(onClick = {
                    estado.selectedDateMillis?.let {
                        onDataChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    mostrarSeletor = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { mostrarSeletor = false }) { Text("Cancelar") } },
        ) {
            DatePicker(state = estado)
        }
    }
}
