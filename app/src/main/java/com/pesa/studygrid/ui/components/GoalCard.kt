package com.pesa.studygrid.ui.components

import android.view.HapticFeedbackConstants
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pesa.studygrid.R
import com.pesa.studygrid.data.MAX_LEVEL
import com.pesa.studygrid.ui.GoalUi
import com.pesa.studygrid.ui.theme.LocalAero
import java.time.LocalDate

@Composable
fun GoalCard(
    ui: GoalUi,
    onBumpToday: () -> Unit,
    onDayClick: (LocalDate) -> Unit,
    onEdit: () -> Unit,
) {
    val a = LocalAero.current
    val view = LocalView.current
    val color = Color(ui.goal.color)
    val todayLevel = ui.logs[LocalDate.now()]?.intensity ?: 0

    ChromeCard(
        title = ui.goal.title,
        corner = {
            ChromeRoundButton(onClick = onEdit, contentDescription = "Editar meta", size = 36.dp) {
                Icon(Icons.Filled.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
            }
        },
    ) {
        // End padding leaves room for the chrome edit button in the corner.
        Row(Modifier.fillMaxWidth().heightIn(min = 36.dp).padding(end = 44.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(12.dp).clip(CircleShape).background(color).border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape))
            Spacer(Modifier.width(10.dp))
            Text(
                ui.goal.description.ifBlank { "Meta de estudio" },
                style = MaterialTheme.typography.bodyMedium,
                color = a.muted,
            )
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Stat(R.drawable.emoji_fire, ui.stats.current, "Racha actual")
            Stat(R.drawable.emoji_trophy, ui.stats.best, "Mejor racha")
            Stat(R.drawable.emoji_calendar, ui.stats.total, "Días activos")
        }

        val panel = RoundedCornerShape(14.dp)
        Box(
            Modifier
                .fillMaxWidth()
                .clip(panel)
                .background(a.graphPanel)
                // Dark top / light bottom edge = recessed screen.
                .border(1.dp, Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.35f), Color.White.copy(alpha = 0.6f))), panel)
                .padding(10.dp),
        ) {
            ContributionGraph(ui.logs, color, onDayClick)
        }

        // "+1 Hoy" sits on the left so the bottom-right FAB never covers it while scrolling.
        Row(verticalAlignment = Alignment.CenterVertically) {
            GelButton("+1 Hoy", color, onClick = {
                if (todayLevel >= MAX_LEVEL) {
                    view.performHapticFeedback(HapticFeedbackConstants.REJECT)
                } else {
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    onBumpToday()
                }
            })
            Spacer(Modifier.weight(1f))
            Text("Hoy: nivel $todayLevel/$MAX_LEVEL", style = MaterialTheme.typography.bodyMedium, color = a.muted)
        }
    }
}

@Composable
private fun Stat(@DrawableRes icon: Int, value: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(icon), contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(6.dp))
            Text("$value", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = LocalAero.current.muted)
    }
}
