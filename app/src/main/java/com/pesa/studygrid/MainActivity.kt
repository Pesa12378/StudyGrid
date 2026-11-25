package com.pesa.studygrid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.runtime.rememberCoroutineScope
import androidx.glance.appwidget.updateAll
import com.pesa.studygrid.reminder.Reminder
import com.pesa.studygrid.widget.StudyGridWidget
import kotlinx.coroutines.launch
import com.pesa.studygrid.ui.screens.MainScreen
import com.pesa.studygrid.ui.theme.StudyGridTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Re-arm the daily reminder (alarms are lost on force-stop); no-op when it's off.
        Reminder.schedule(applicationContext)
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        setContent {
            var dark by remember { mutableStateOf(prefs.getBoolean("dark", true)) }
            val scope = rememberCoroutineScope()
            val view = LocalView.current
            SideEffect {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }
            StudyGridTheme(dark) {
                MainScreen(dark, onToggleDark = {
                    dark = !dark
                    prefs.edit().putBoolean("dark", dark).apply()
                    // The widget reads the same preference; redraw it in the new theme.
                    scope.launch { StudyGridWidget().updateAll(applicationContext) }
                })
            }
        }
    }
}
