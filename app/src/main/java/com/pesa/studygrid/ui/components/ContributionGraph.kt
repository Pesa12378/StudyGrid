package com.pesa.studygrid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pesa.studygrid.data.DailyLogEntity
import com.pesa.studygrid.data.MAX_LEVEL
import com.pesa.studygrid.ui.theme.LocalAero
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

val Spanish: Locale = Locale.forLanguageTag("es")

private const val WEEKS = 26
private val CELL = 24.dp
private val GAP = 4.dp
private val MONTH_ROW = 20.dp
private val CELL_SHAPE = RoundedCornerShape(5.dp)

/** Popup centered above the anchor (below if no room), clamped to the window. */
private val AboveAnchor = object : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
        val x = (anchorBounds.center.x - popupContentSize.width / 2).coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))
        val above = anchorBounds.top - popupContentSize.height
        val y = if (above >= 0) above else anchorBounds.bottom
        return IntOffset(x, y)
    }
}
private val DAY_LABELS = listOf("L", "M", "X", "J", "V", "S", "D")
private val MONTH_FORMAT = DateTimeFormatter.ofPattern("MMM", Spanish)
private val CELL_DATE = DateTimeFormatter.ofPattern("d 'de' MMMM", Spanish)

private val NOTE_DATE = DateTimeFormatter.ofPattern("d MMM", Spanish)

internal val LEVEL_NAMES = listOf("Nada", "Bajo", "Medio", "Alto", "Intenso")

private val RAMP = floatArrayOf(0f, 0.30f, 0.55f, 0.78f, 1f)

/** Single place for the heatmap ramp: level 1..4 = goal color at growing strength over the panel base (level 0 is the empty cell). */
fun levelColor(level: Int, goalColor: Color, dark: Boolean): Color {
    val base = if (dark) Color(0xFF0A1A2B) else Color(0xFFE9FBF7)
    return androidx.compose.ui.graphics.lerp(base, goalColor.copy(alpha = 1f), RAMP[level.coerceIn(0, MAX_LEVEL)])
}

/** 26 weeks x 7 days (Mon..Sun), newest week on the right; starts scrolled to the right end. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContributionGraph(
    logs: Map<LocalDate, DailyLogEntity>,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    goalColor: Color,
    scroll: Boolean = true, // false for the share image: lays out all 26 weeks
) {
    val a = LocalAero.current
    val dark = a.dark
    val todayRing = if (dark) Color.White else a.accentDeep
    val haptic = LocalHapticFeedback.current
    var noteDay by remember { mutableStateOf<LocalDate?>(null) }
    val today = LocalDate.now()
    val firstWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(WEEKS - 1L)

    Row(modifier) {
        Column(Modifier.padding(top = MONTH_ROW + GAP, end = 6.dp), verticalArrangement = Arrangement.spacedBy(GAP)) {
            DAY_LABELS.forEach {
                Box(Modifier.height(CELL), contentAlignment = Alignment.Center) { Text(it, fontSize = 11.sp, color = a.muted) }
            }
        }
        // Int.MAX_VALUE is clamped to maxValue on first layout -> opens on the current week.
        Row(if (scroll) Modifier.horizontalScroll(rememberScrollState(Int.MAX_VALUE)) else Modifier, horizontalArrangement = Arrangement.spacedBy(GAP)) {
            repeat(WEEKS) { w ->
                val weekStart = firstWeek.plusWeeks(w.toLong())
                Column(verticalArrangement = Arrangement.spacedBy(GAP)) {
                    Box(Modifier.width(CELL).height(MONTH_ROW)) {
                        // Label the week where a month begins (always >= 4 columns apart, so no overlap).
                        if (weekStart.dayOfMonth <= 7) {
                            Text(
                                MONTH_FORMAT.format(weekStart).trimEnd('.'),
                                fontSize = 11.sp,
                                color = a.muted,
                                softWrap = false,
                                modifier = Modifier.wrapContentWidth(Alignment.Start, unbounded = true),
                            )
                        }
                    }
                    for (d in 0L..6L) {
                        val date = weekStart.plusDays(d)
                        val level = logs[date]?.intensity ?: 0
                        val cell = Modifier.size(CELL).clip(CELL_SHAPE)
                        if (date.isAfter(today)) {
                            Box(cell)
                        } else {
                            val filled = if (level == 0) cell.background(a.emptyCell).border(1.dp, a.emptyCellBorder, CELL_SHAPE)
                            else cell.background(levelColor(level, goalColor, dark))
                            val note = logs[date]?.note.orEmpty()
                            val hasNote = note.isNotBlank()
                            Box(
                                (if (date == today) filled.drawWithContent {
                                    drawContent()
                                    // 2dp ring inset 1dp, so it reads no heavier than the neighbouring cells.
                                    drawRoundRect(todayRing, Offset(2.dp.toPx(), 2.dp.toPx()), Size(size.width - 4.dp.toPx(), size.height - 4.dp.toPx()), CornerRadius(4.dp.toPx()), style = Stroke(2.dp.toPx()))
                                } else filled)
                                    .semantics { contentDescription = "${CELL_DATE.format(date)}, nivel $level" + if (hasNote) ", con nota" else "" }
                                    .combinedClickable(
                                        onClick = { onDayClick(date) },
                                        onLongClickLabel = "Ver nota",
                                        onLongClick = if (hasNote) ({
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            noteDay = date
                                        }) else null,
                                    ),
                                contentAlignment = Alignment.TopEnd,
                            ) {
                                if (hasNote) {
                                    // White dot with a thin dark outline: visible on pale and saturated cells alike.
                                    Box(Modifier.padding(3.dp).size(4.dp).drawBehind {
                                        drawCircle(Color.White.copy(alpha = 0.85f))
                                        drawCircle(Color.Black.copy(alpha = 0.4f), size.minDimension / 2 - 0.25.dp.toPx(), style = Stroke(0.5.dp.toPx()))
                                    })
                                }
                                if (noteDay == date && hasNote) {
                                    val fg = readableTextOn(a.dialogBg)
                                    Popup(popupPositionProvider = AboveAnchor, onDismissRequest = { noteDay = null }) {
                                        Column(
                                            Modifier.widthIn(max = 220.dp).clip(RoundedCornerShape(12.dp))
                                                .background(a.dialogBg).padding(10.dp),
                                        ) {
                                            Text(NOTE_DATE.format(date), fontSize = 11.sp, color = fg.copy(alpha = 0.7f))
                                            Text(note, fontSize = 13.sp, color = fg)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Color key for the heatmap: one swatch per level with its name (Bajo, Medio, Alto, Intenso). */
@Composable
fun LevelLegend(goalColor: Color, modifier: Modifier = Modifier) {
    val a = LocalAero.current
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        for (level in 1..MAX_LEVEL) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(11.dp).clip(CELL_SHAPE).background(levelColor(level, goalColor, a.dark)))
                Spacer(Modifier.width(4.dp))
                Text(LEVEL_NAMES[level], fontSize = 10.sp, color = a.muted)
            }
        }
    }
}
