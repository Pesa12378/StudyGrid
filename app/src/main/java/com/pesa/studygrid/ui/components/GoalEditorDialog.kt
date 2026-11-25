package com.pesa.studygrid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pesa.studygrid.data.GoalEntity
import com.pesa.studygrid.ui.theme.GoalPalette

/** Spoken names for [GoalPalette], same order. */
private val COLOR_NAMES = listOf("aqua", "lima", "cielo", "violeta", "coral")

/** [initial] null = create a new goal. Delete is only reachable from here (edit mode), on purpose. */
@Composable
fun GoalEditorDialog(initial: GoalEntity?, onSave: (GoalEntity) -> Unit, onDelete: () -> Unit, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var color by remember { mutableIntStateOf(initial?.color ?: GoalPalette.first().toArgb()) }
    var weekly by remember { mutableIntStateOf(initial?.weeklyTarget ?: 0) }

    GlassDialog(onDismiss) {
        Text(if (initial == null) "Nueva meta" else "Editar meta", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = title,
            onValueChange = { title = it.take(60) },
            label = { Text("Título") },
            placeholder = { Text("Roadmap Data Engineer") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = description,
            onValueChange = { description = it.take(140) },
            label = { Text("Descripción (opcional)") },
            maxLines = 2,
            modifier = Modifier.fillMaxWidth(),
        )
        Text("Color", style = MaterialTheme.typography.labelLarge)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            GoalPalette.forEachIndexed { i, c ->
                val selected = c.toArgb() == color
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(c)
                        .border(if (selected) 3.dp else 1.dp, if (selected) MaterialTheme.colorScheme.onSurface else Color.White.copy(alpha = 0.4f), CircleShape)
                        .semantics { contentDescription = "Color ${COLOR_NAMES[i]}" }
                        .selectable(selected = selected, role = Role.RadioButton) { color = c.toArgb() },
                    contentAlignment = Alignment.Center,
                ) {
                    if (selected) Icon(Icons.Filled.Check, "Seleccionado", tint = Color(0xFF06121F))
                }
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Meta semanal", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            TextButton(onClick = { weekly-- }, enabled = weekly > 0) { Text("−", style = MaterialTheme.typography.titleLarge) }
            Text(
                if (weekly == 0) "Sin meta" else "$weekly ${if (weekly == 1) "día" else "días"}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(min = 68.dp),
            )
            TextButton(onClick = { weekly++ }, enabled = weekly < 7) { Text("+", style = MaterialTheme.typography.titleLarge) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            if (initial != null) {
                TextButton(onClick = onDelete) { Text("Eliminar", color = DangerRed) }
                Spacer(Modifier.weight(1f))
            }
            TextButton(onClick = onDismiss) { Text("Cancelar") }
            Spacer(Modifier.width(8.dp))
            GelButton("Guardar", Color(color), enabled = title.isNotBlank(), onClick = {
                onSave((initial ?: GoalEntity(title = "", color = color)).copy(title = title.trim(), description = description.trim(), color = color, weeklyTarget = weekly))
            })
        }
    }
}

private val DangerRed = Color(0xFFE5484D)

/** Deleting wipes the whole history, so the user must type the goal's title to confirm. */
@Composable
fun DeleteGoalDialog(goal: GoalEntity, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    var typed by remember { mutableStateOf("") }
    val matches = typed.trim().equals(goal.title.trim(), ignoreCase = true)
    GlassDialog(onDismiss) {
        Text("¿Eliminar meta?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(
            "Se borrará \"${goal.title}\" con TODO su historial. Podrás deshacerlo solo durante unos segundos.\n\nEscribe el nombre de la meta para confirmar:",
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedTextField(
            value = typed,
            onValueChange = { typed = it },
            placeholder = { Text(goal.title) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
            Spacer(Modifier.width(8.dp))
            GelButton("Eliminar", DangerRed, onClick = onConfirm, enabled = matches)
        }
    }
}

@Composable
fun ConfirmDialog(title: String, text: String, confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    GlassDialog(onDismiss) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(text, style = MaterialTheme.typography.bodyMedium)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
            Spacer(Modifier.width(8.dp))
            GelButton(confirmLabel, DangerRed, onClick = onConfirm)
        }
    }
}
