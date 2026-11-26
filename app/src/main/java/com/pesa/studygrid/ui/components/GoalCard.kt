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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.graphics.lerp
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pesa.studygrid.R
import com.pesa.studygrid.data.MAX_LEVEL
import com.pesa.studygrid.ui.GoalUi
import com.pesa.studygrid.ui.STREAK_BADGES
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.pesa.studygrid.ui.theme.LocalAero
import java.time.LocalDate

@Composable
fun GoalCard(
    ui: GoalUi,
    onBumpToday: () -> Unit,
    onDayClick: (LocalDate) -> Unit,
    onEdit: () -> Unit,
    onStats: () -> Unit,
) {
    val a = LocalAero.current
    val view = LocalView.current
    val color = Color(ui.goal.color)
    val todayLevel = ui.logs[LocalDate.now()]?.intensity ?: 0

    GlassCard(
        title = ui.goal.title,
        corner = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassRoundButton(onClick = onStats, contentDescription = "Estadísticas por mes", size = 36.dp) {
                    // Mini bar chart; material-icons-core has no chart icon.
                    Row(Modifier.size(17.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
                        listOf(0.5f, 1f, 0.7f).forEach { Box(Modifier.width(4.dp).fillMaxHeight(it).background(a.ink)) }
                    }
                }
                GlassRoundButton(onClick = onEdit, contentDescription = "Editar meta", size = 36.dp) {
                    Icon(Icons.Filled.Edit, contentDescription = null, tint = a.ink, modifier = Modifier.size(17.dp))
                }
            }
        },
    ) {
        // End padding leaves room for the two glass buttons in the corner.
        Row(Modifier.fillMaxWidth().heightIn(min = 36.dp).padding(end = 112.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(12.dp).clip(CircleShape).background(color).border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape))
            Spacer(Modifier.width(10.dp))
            Text(
                ui.goal.description.ifBlank { "Meta de estudio" },
                style = MaterialTheme.typography.bodyMedium,
                color = a.muted,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Stat(GelGlyph.Flame, ui.stats.current, "Racha actual")
                Stat(GelGlyph.Trophy, ui.stats.best, "Mejor racha")
                Stat(GelGlyph.Calendar, ui.stats.total, "Días activos")
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)) {
                STREAK_BADGES.forEach { Medallion(it, ui.stats.best >= it) }
            }
        }

        if (ui.goal.weeklyTarget > 0) WeeklyProgress(ui.stats.week, ui.goal.weeklyTarget, color)

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
            Column {
                ContributionGraph(ui.logs, onDayClick, goalColor = color)
                Spacer(Modifier.height(8.dp))
                LevelLegend(color)
            }
        }

        // "+1 Hoy" sits on the left so the bottom-right FAB never covers it while scrolling.
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BubbleButton("+1 Hoy", color, onClick = {
                // At max the ViewModel shows the "ya está al máximo" snackbar.
                view.performHapticFeedback(if (todayLevel >= MAX_LEVEL) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.CONFIRM)
                onBumpToday()
            })
            Text(
                "Hoy: nivel $todayLevel/$MAX_LEVEL",
                style = MaterialTheme.typography.labelLarge,
                color = a.muted,
                modifier = Modifier.clip(CircleShape).drawBehind { drawBubble(a.accent, a.dark, 0.12f) }.padding(horizontal = 14.dp, vertical = 6.dp),
            )
        }
    }
}

/** Gel bar inside a recessed track: active days this week vs the weekly target. */
@Composable
private fun WeeklyProgress(done: Int, target: Int, color: Color) {
    val a = LocalAero.current
    val track = RoundedCornerShape(50)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Esta semana", style = MaterialTheme.typography.labelMedium, color = a.muted, modifier = Modifier.weight(1f))
            Text(
                if (done >= target) "¡Cumplida! $done/$target" else "$done/$target días",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(track)
                .background(a.graphPanel)
                .border(1.dp, Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.35f), Color.White.copy(alpha = 0.6f))), track),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(minOf(1f, done / target.toFloat()))
                    .fillMaxHeight()
                    .clip(track)
                    .background(Brush.verticalGradient(listOf(lerp(color, Color.White, 0.4f), color, lerp(color, Color.Black, 0.2f)))),
            )
        }
    }
}

/** Streak badge: amber-tinted glass when earned, clear glass otherwise. */
@Composable
private fun Medallion(days: Int, earned: Boolean) {
    val a = LocalAero.current
    val amber = Color(0xFFFFB300)
    val fill = if (earned) Brush.verticalGradient(listOf(amber.copy(alpha = 0.55f), amber.copy(alpha = 0.25f))) else Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.25f), Color.White.copy(alpha = 0.06f)))
    val rim = if (earned) Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.9f), amber.copy(alpha = 0.8f))) else Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.6f), a.muted.copy(alpha = 0.35f)))
    Box(
        Modifier
            .size(width = 34.dp, height = 22.dp)
            .clip(CircleShape)
            .background(fill)
            .border(1.dp, rim, CircleShape)
            .semantics { contentDescription = "Logro racha de $days días: ${if (earned) "obtenido" else "pendiente"}" },
        contentAlignment = Alignment.Center,
    ) {
        Text("$days", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = if (earned) (if (a.dark) Color(0xFFFFE082) else Color(0xFF6B4500)) else a.muted)
    }
}

@Composable
private fun Stat(icon: GelGlyph, value: Int, label: String) {
    Column(Modifier.semantics(mergeDescendants = true) { contentDescription = "$label: $value" }, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GelIcon(icon)
            Spacer(Modifier.width(6.dp))
            Text("$value", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = LocalAero.current.muted)
    }
}
