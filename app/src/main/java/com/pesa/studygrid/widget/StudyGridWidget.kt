package com.pesa.studygrid.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.pesa.studygrid.MainActivity
import com.pesa.studygrid.R
import com.pesa.studygrid.data.AppDatabase
import com.pesa.studygrid.data.GoalEntity
import com.pesa.studygrid.ui.GoalStats
import com.pesa.studygrid.ui.components.levelColor
import com.pesa.studygrid.ui.components.readableTextOn
import com.pesa.studygrid.ui.computeStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

private val OUTER = 10.dp
private val PANEL_PAD = 8.dp
private val GAP = 10.dp
private val INFO_WIDTH = 118.dp
private val MAX_CELL = 26.dp
private val GoalIdKey = ActionParameters.Key<Long>("goalId")
private val WIDGET_BACKGROUNDS = intArrayOf(
    R.drawable.widget_bg, R.drawable.widget_hills, R.drawable.widget_meadow_crest,
    R.drawable.widget_sea, R.drawable.widget_pool, R.drawable.widget_ripples,
)

/** Day/night look, mirroring the app's Aero themes (widgets can't blur or animate, so glass is faked with drawables). */
private class WidgetTheme(val scrim: Color, val panel: Int, val title: Color, val muted: Color, val emptyCell: Color)

private val Day = WidgetTheme(
    scrim = Color(0xFF03AAB1).copy(alpha = 0.14f),
    panel = R.drawable.widget_panel_day,
    title = Color(0xFF0B3B3A),
    muted = Color(0xFF1A5C5A),
    emptyCell = Color(0xFF0E504F).copy(alpha = 0.12f),
)
private val Night = WidgetTheme(
    scrim = Color(0xFF041322).copy(alpha = 0.58f),
    panel = R.drawable.widget_panel_night,
    title = Color.White,
    muted = Color(0xFF9CC0DD),
    emptyCell = Color.White.copy(alpha = 0.08f),
)

private class WidgetData(val goal: GoalEntity, val levels: Map<LocalDate, Int>, val stats: GoalStats, val button: Bitmap)

private val BUTTON_W = 98.dp
private val BUTTON_H = 40.dp

/**
 * Widgets can't draw gradients in a dynamic color, so the gel "+1" button is pre-rendered per goal color:
 * light-to-deep body, specular highlight, inner bottom glow, light rim and a soft drop shadow (inside a 4dp margin).
 */
private fun gelButtonBitmap(context: Context, color: Color): Bitmap {
    val d = context.resources.displayMetrics.density
    val margin = 4 * d
    val bmp = Bitmap.createBitmap((BUTTON_W.value * d).toInt(), (BUTTON_H.value * d).toInt(), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val body = RectF(margin, margin, bmp.width - margin, bmp.height - margin)
    val r = body.height() / 2
    val aa = Paint.ANTI_ALIAS_FLAG

    canvas.drawRoundRect(body, r, r, Paint(aa).apply {
        shader = LinearGradient(
            0f, body.top, 0f, body.bottom,
            intArrayOf(lerp(color, Color.White, 0.35f).toArgb(), color.toArgb(), lerp(color, Color.Black, 0.22f).toArgb()),
            floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP,
        )
        setShadowLayer(3 * d, 0f, 1.5f * d, 0x59000000)
    })
    val glow = RectF(body.left + body.width() * 0.2f, body.bottom - body.height() * 0.4f, body.right - body.width() * 0.2f, body.bottom - 2 * d)
    canvas.drawOval(glow, Paint(aa).apply {
        shader = RadialGradient(glow.centerX(), glow.centerY(), glow.width() / 2, 0x4DFFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP)
    })
    val hl = RectF(body.left + 3 * d, body.top + 1.5f * d, body.right - 3 * d, body.top + body.height() * 0.5f)
    canvas.drawRoundRect(hl, hl.height() / 2, hl.height() / 2, Paint(aa).apply {
        shader = LinearGradient(0f, hl.top, 0f, hl.bottom, 0xA6FFFFFF.toInt(), 0x0AFFFFFF, Shader.TileMode.CLAMP)
    })
    canvas.drawRoundRect(body, r, r, Paint(aa).apply {
        style = Paint.Style.STROKE
        strokeWidth = d
        this.color = 0x8CFFFFFF.toInt()
    })
    return bmp
}

// shortcut: shows the first goal only; add a configure activity to pick a goal per widget if needed.
class StudyGridWidget : GlanceAppWidget() {
    // Exact: recompose with the widget's real size so cells fill the space on any launcher/phone.
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val today = LocalDate.now()
        // Same preference the app's 🌙/☀️ toggle writes.
        val dark = context.getSharedPreferences("settings", Context.MODE_PRIVATE).getBoolean("dark", true)
        val data = withContext(Dispatchers.IO) {
            val dao = AppDatabase.get(context).dao()
            val goal = dao.allGoals().firstOrNull() ?: return@withContext null
            val logs = dao.logsOf(goal.id).filter { it.intensity > 0 }
            WidgetData(
                goal,
                logs.associate { LocalDate.ofEpochDay(it.epochDay) to it.intensity },
                computeStats(logs.map { LocalDate.ofEpochDay(it.epochDay) }.toSet(), today),
                gelButtonBitmap(context, Color(goal.color)),
            )
        }
        provideContent { Content(data, today, if (dark) Night else Day) }
    }
}

@androidx.compose.runtime.Composable
private fun Content(data: WidgetData?, today: LocalDate, t: WidgetTheme) {
    // Layers: photo, theme tint, bubbles, glass panels, glass rim on top (non-clickable, so taps pass through).
    Box(GlanceModifier.fillMaxSize().cornerRadius(24.dp).clickable(actionStartActivity<MainActivity>())) {
        // Widgets can't animate: rotate the photo once a day instead.
        Image(ImageProvider(WIDGET_BACKGROUNDS[today.dayOfYear % WIDGET_BACKGROUNDS.size]), null, GlanceModifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(GlanceModifier.fillMaxSize().background(ColorProvider(t.scrim))) {}
        Image(ImageProvider(R.drawable.widget_bubbles), null, GlanceModifier.fillMaxSize(), contentScale = ContentScale.Crop)

        if (data == null) {
            Box(GlanceModifier.fillMaxSize().padding(OUTER), contentAlignment = Alignment.Center) {
                Box(GlanceModifier.background(ImageProvider(t.panel)).padding(14.dp)) {
                    Text("Crea una meta en StudyGrid", style = TextStyle(color = ColorProvider(t.title), fontSize = 14.sp, fontWeight = FontWeight.Bold))
                }
            }
        } else {
            Panels(data, today, t)
        }

        Image(ImageProvider(R.drawable.widget_rim), null, GlanceModifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
    }
}

@androidx.compose.runtime.Composable
private fun Panels(data: WidgetData, today: LocalDate, t: WidgetTheme) {
    val base = Color(data.goal.color)
    // Biggest cell that fits 7 rows in the height; as many weeks as fit beside the info panel.
    val size = LocalSize.current
    val cell = minOf((size.height - OUTER * 2 - PANEL_PAD * 2) / 7, MAX_CELL)
    val gridWidth = size.width - OUTER * 2 - GAP - INFO_WIDTH - PANEL_PAD * 2
    val weeks = (gridWidth / cell).toInt().coerceIn(4, 26)
    val firstDay = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(weeks - 1L)

    Row(GlanceModifier.fillMaxSize().padding(OUTER), verticalAlignment = Alignment.CenterVertically) {
        Row(GlanceModifier.background(ImageProvider(t.panel)).padding(PANEL_PAD)) {
            for (w in 0 until weeks) Column {
                for (d in 0 until 7) {
                    val day = firstDay.plusDays(w * 7L + d)
                    // Future days stay blank (transparent).
                    val color = when {
                        day.isAfter(today) -> Color.Transparent
                        else -> data.levels[day]?.let { levelColor(it) } ?: t.emptyCell
                    }
                    Box(GlanceModifier.size(cell).padding(cell * 0.1f)) {
                        Box(GlanceModifier.fillMaxSize().background(ColorProvider(color)).cornerRadius(cell * 0.18f)) {}
                    }
                }
            }
        }
        Spacer(GlanceModifier.width(GAP))
        Column(GlanceModifier.width(INFO_WIDTH).fillMaxHeight().background(ImageProvider(t.panel)).padding(10.dp)) {
            Text(
                data.goal.title.uppercase(), maxLines = 2,
                style = TextStyle(color = ColorProvider(t.title), fontWeight = FontWeight.Bold, fontSize = 14.sp),
            )
            Text("🔥 Racha: ${data.stats.current}", style = TextStyle(color = ColorProvider(t.muted), fontSize = 12.sp))
            Spacer(GlanceModifier.defaultWeight())
            Box(
                GlanceModifier.size(BUTTON_W, BUTTON_H)
                    .clickable(actionRunCallback<BumpTodayAction>(actionParametersOf(GoalIdKey to data.goal.id))),
                contentAlignment = Alignment.Center,
            ) {
                Image(ImageProvider(data.button), "+1 Hoy", GlanceModifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
                Text("+1 Hoy", style = TextStyle(color = ColorProvider(readableTextOn(base)), fontWeight = FontWeight.Bold, fontSize = 14.sp))
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
