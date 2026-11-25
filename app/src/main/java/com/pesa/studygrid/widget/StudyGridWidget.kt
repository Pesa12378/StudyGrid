package com.pesa.studygrid.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.pesa.studygrid.MainActivity
import com.pesa.studygrid.data.AppDatabase
import com.pesa.studygrid.data.GoalEntity
import com.pesa.studygrid.ui.GoalStats
import com.pesa.studygrid.ui.components.levelColor
import com.pesa.studygrid.ui.computeStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

private val Navy = Color(0xFF0E2236)
private val Muted = Color(0xFF9CC0DD)
private val Dark = Color(0xFF06121F)
private val EmptyCell = Color.White.copy(alpha = 0.07f)
private const val WEEKS = 7
private val GoalIdKey = ActionParameters.Key<Long>("goalId")

private class WidgetData(val goal: GoalEntity, val levels: Map<LocalDate, Int>, val stats: GoalStats)

// shortcut: shows the first goal only; add a configure activity to pick a goal per widget if needed.
class StudyGridWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val today = LocalDate.now()
        val data = withContext(Dispatchers.IO) {
            val dao = AppDatabase.get(context).dao()
            val goal = dao.allGoals().firstOrNull() ?: return@withContext null
            val logs = dao.logsOf(goal.id).filter { it.intensity > 0 }
            WidgetData(
                goal,
                logs.associate { LocalDate.ofEpochDay(it.epochDay) to it.intensity },
                computeStats(logs.map { LocalDate.ofEpochDay(it.epochDay) }.toSet(), today),
            )
        }
        provideContent { Content(data, today) }
    }
}

@androidx.compose.runtime.Composable
private fun Content(data: WidgetData?, today: LocalDate) {
    val root = GlanceModifier.fillMaxSize().background(ColorProvider(Navy)).cornerRadius(20.dp)
        .padding(12.dp).clickable(actionStartActivity<MainActivity>())
    if (data == null) {
        Box(root, contentAlignment = Alignment.Center) {
            Text("Crea una meta en StudyGrid", style = TextStyle(color = ColorProvider(Muted), fontSize = 14.sp))
        }
        return
    }
    val base = Color(data.goal.color)
    val firstDay = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(WEEKS - 1L)
    Row(root, verticalAlignment = Alignment.CenterVertically) {
        Row {
            for (w in 0 until WEEKS) Column {
                for (d in 0 until 7) {
                    val day = firstDay.plusDays(w * 7L + d)
                    // Future days stay blank (transparent).
                    val color = when {
                        day.isAfter(today) -> Color.Transparent
                        else -> data.levels[day]?.let { levelColor(base, it) } ?: EmptyCell
                    }
                    Box(GlanceModifier.size(12.dp).padding(1.dp)) {
                        Box(GlanceModifier.fillMaxSize().background(ColorProvider(color)).cornerRadius(2.dp)) {}
                    }
                }
            }
        }
        Spacer(GlanceModifier.width(12.dp))
        Column {
            Text(
                data.goal.title, maxLines = 2,
                style = TextStyle(color = ColorProvider(Color.White), fontWeight = FontWeight.Bold, fontSize = 15.sp),
            )
            Text("Racha: ${data.stats.current}", style = TextStyle(color = ColorProvider(Muted), fontSize = 12.sp))
            Spacer(GlanceModifier.height(8.dp))
            Box(
                GlanceModifier.background(ColorProvider(base)).cornerRadius(16.dp)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .clickable(actionRunCallback<BumpTodayAction>(actionParametersOf(GoalIdKey to data.goal.id))),
            ) {
                Text("+1 Hoy", style = TextStyle(color = ColorProvider(Dark), fontWeight = FontWeight.Bold, fontSize = 13.sp))
            }
        }
    }
}

class BumpTodayAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val goalId = parameters[GoalIdKey] ?: return
        withContext(Dispatchers.IO) { AppDatabase.get(context).dao().bump(goalId, LocalDate.now().toEpochDay()) }
        StudyGridWidget().update(context, glanceId)
    }
}

class StudyGridWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = StudyGridWidget()
}
