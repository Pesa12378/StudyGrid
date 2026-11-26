package com.pesa.studygrid.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.lifecycleScope
import com.pesa.studygrid.data.AppDatabase
import com.pesa.studygrid.data.GoalEntity
import com.pesa.studygrid.ui.components.BubbleButton
import com.pesa.studygrid.ui.components.GlassDialog
import com.pesa.studygrid.ui.theme.LocalAero
import com.pesa.studygrid.ui.theme.StudyGridTheme
import kotlinx.coroutines.launch

/** Shown when a widget is placed or reconfigured: picks which goal that widget displays. */
class WidgetConfigActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        setResult(Activity.RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return finish()
        val dark = getSharedPreferences("settings", MODE_PRIVATE).getBoolean("dark", true)
        setContent {
            var goals by remember { mutableStateOf<List<GoalEntity>?>(null) }
            LaunchedEffect(Unit) { goals = AppDatabase.get(applicationContext).dao().allGoals() }
            StudyGridTheme(dark) {
                GlassDialog(onDismiss = { finish() }) {
                    val list = goals ?: return@GlassDialog
                    Text("Elegir meta", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    if (list.isEmpty()) {
                        Text("Primero crea una meta en la app Habitum Aqua.", style = MaterialTheme.typography.bodyMedium)
                        BubbleButton("Cerrar", LocalAero.current.accent, onClick = { finish() })
                    }
                    list.forEach { goal ->
                        Row(
                            Modifier.fillMaxWidth().clickable { choose(widgetId, goal.id) }.padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Row(Modifier.size(16.dp).background(Color(goal.color), CircleShape)) {}
                            Text(goal.title, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }
    }

    private fun choose(widgetId: Int, goalId: Long) {
        widgetPrefs(this).edit().putLong(widgetGoalKey(widgetId), goalId).apply()
        lifecycleScope.launch {
            // The pref is already saved; a failed refresh must not cancel the placement.
            try {
                val glanceId = GlanceAppWidgetManager(applicationContext).getGlanceIdBy(widgetId)
                StudyGridWidget().update(applicationContext, glanceId)
            } catch (e: Exception) {
                android.util.Log.w("WidgetConfig", "widget update failed", e)
            }
            setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
            finish()
        }
    }
}
