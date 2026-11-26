package com.pesa.studygrid.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.pesa.studygrid.reminder.Reminder
import com.pesa.studygrid.ui.theme.LocalAero

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    var on by remember { mutableStateOf(prefs.getBoolean("reminder_on", false)) }
    val minutes = prefs.getInt("reminder_minutes", 1200)
    val picker = rememberTimePickerState(minutes / 60, minutes % 60, is24Hour = true)
    var picking by remember { mutableStateOf(false) }
    var blocked by remember { mutableStateOf(false) }

    fun save() {
        prefs.edit().putBoolean("reminder_on", on).putInt("reminder_minutes", picker.hour * 60 + picker.minute).apply()
        Reminder.schedule(context)
    }
    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        save() // saved even if denied; the notification is just skipped
        if (granted) onDismiss() else blocked = true
    }

    GlassDialog(onDismiss) {
        Text("Recordatorio diario", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Activar", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            Switch(checked = on, onCheckedChange = { on = it })
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Hora", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            TextButton(onClick = { picking = !picking }) {
                Text("%02d:%02d".format(picker.hour, picker.minute), style = MaterialTheme.typography.titleLarge)
            }
        }
        if (picking) TimePicker(picker)
        if (blocked) Text("Las notificaciones están bloqueadas: actívalas en los ajustes del sistema.", style = MaterialTheme.typography.bodySmall, color = LocalAero.current.muted)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
            Spacer(Modifier.width(8.dp))
            BubbleButton("Guardar", LocalAero.current.accent, onClick = {
                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                if (on && !granted) askPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                else { save(); onDismiss() }
            })
        }
    }
}
