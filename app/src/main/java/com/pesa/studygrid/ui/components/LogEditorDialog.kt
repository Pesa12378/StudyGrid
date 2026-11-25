package com.pesa.studygrid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pesa.studygrid.data.DailyLogEntity
import com.pesa.studygrid.data.MAX_LEVEL
import com.pesa.studygrid.ui.theme.LocalAero
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val LEVEL_NAMES = listOf("Nada", "Bajo", "Medio", "Alto", "Intenso")
private val DATE_FORMAT = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", Spanish)

/** Edits one day. ‹ › move to the previous/next day (never past today) to fix a tap on the wrong tiny cell. */
@Composable
fun LogEditorDialog(
    goalTitle: String,
    color: Color,
    initialDate: LocalDate,
    logs: Map<LocalDate, DailyLogEntity>,
    onSave: (date: LocalDate, intensity: Int, note: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val a = LocalAero.current
    val today = LocalDate.now()
    var date by remember { mutableStateOf(initialDate) }
    // Moving to another day loads that day's values (unsaved edits of the previous day are dropped).
    var level by remember(date) { mutableIntStateOf(logs[date]?.intensity ?: 0) }
    var note by remember(date) { mutableStateOf(logs[date]?.note ?: "") }

    GlassDialog(onDismiss) {
        Text(goalTitle, style = MaterialTheme.typography.labelLarge, color = a.muted)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { date = date.minusDays(1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Día anterior")
            }
            Text(
                DATE_FORMAT.format(date).replaceFirstChar { it.titlecase(Spanish) },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { date = date.plusDays(1) }, enabled = date.isBefore(today)) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Día siguiente")
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            for (l in 0..MAX_LEVEL) {
                val selected = l == level
                val shape = RoundedCornerShape(12.dp)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .size(46.dp)
                            .clip(shape)
                            .background(if (l == 0) a.emptyCell else levelColor(l))
                            .border(
                                if (selected) 2.5.dp else 1.dp,
                                if (selected) MaterialTheme.colorScheme.onSurface else a.emptyCellBorder,
                                shape,
                            )
                            .semantics { contentDescription = "Nivel $l, ${LEVEL_NAMES[l]}" }
                            .selectable(selected = selected, role = Role.RadioButton) { level = l },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("$l", fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(LEVEL_NAMES[l], style = MaterialTheme.typography.labelSmall, color = if (selected) MaterialTheme.colorScheme.onSurface else a.muted)
                }
            }
        }

        OutlinedTextField(
            value = note,
            onValueChange = { note = it.take(280) },
            label = { Text("Nota del día") },
            placeholder = { Text("Capítulo 3 Kimball + ejercicios Window Functions") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 4,
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
            Spacer(Modifier.width(8.dp))
            GelButton("Guardar", color, onClick = { onSave(date, level, note) })
        }
    }
}
