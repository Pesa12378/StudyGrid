package com.pesa.studygrid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
private val CELL = 14.dp
private val GAP = 3.dp
private val MONTH_ROW = 16.dp
private val CELL_SHAPE = RoundedCornerShape(3.dp)
// Level 1 at 0.30 blended into the day-mode empty cell on the light panel; 0.50 keeps it distinct, steps stay increasing.
private val LEVEL_ALPHA = floatArrayOf(0f, 0.50f, 0.68f, 0.84f, 1f)
private val DAY_LABELS = listOf("L", "M", "X", "J", "V", "S", "D")
private val MONTH_FORMAT = DateTimeFormatter.ofPattern("MMM", Spanish)
private val CELL_DATE = DateTimeFormatter.ofPattern("d 'de' MMMM", Spanish)

fun levelColor(base: Color, level: Int): Color = base.copy(alpha = LEVEL_ALPHA[level.coerceIn(0, MAX_LEVEL)])

/** 26 weeks x 7 days (Mon..Sun), newest week on the right; starts scrolled to the right end. */
@Composable
fun ContributionGraph(
    logs: Map<LocalDate, DailyLogEntity>,
    color: Color,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val a = LocalAero.current
    val todayRing = MaterialTheme.colorScheme.onSurface
    val today = LocalDate.now()
    val firstWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(WEEKS - 1L)

    Row(modifier) {
        Column(Modifier.padding(top = MONTH_ROW + GAP, end = 4.dp), verticalArrangement = Arrangement.spacedBy(GAP)) {
            DAY_LABELS.forEach {
                Box(Modifier.height(CELL), contentAlignment = Alignment.Center) { Text(it, fontSize = 9.sp, color = a.muted) }
            }
        }
        // Int.MAX_VALUE is clamped to maxValue on first layout -> opens on the current week.
        Row(Modifier.horizontalScroll(rememberScrollState(Int.MAX_VALUE)), horizontalArrangement = Arrangement.spacedBy(GAP)) {
            repeat(WEEKS) { w ->
                val weekStart = firstWeek.plusWeeks(w.toLong())
                Column(verticalArrangement = Arrangement.spacedBy(GAP)) {
                    Box(Modifier.width(CELL).height(MONTH_ROW)) {
                        // Label the week where a month begins (always >= 4 columns apart, so no overlap).
                        if (weekStart.dayOfMonth <= 7) {
                            Text(
                                MONTH_FORMAT.format(weekStart).trimEnd('.'),
                                fontSize = 9.sp,
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
                            else cell.background(levelColor(color, level))
                            Box(
                                (if (date == today) filled.border(1.5.dp, todayRing, CELL_SHAPE) else filled)
                                    .semantics { contentDescription = "${CELL_DATE.format(date)}, nivel $level" }
                                    .clickable { onDayClick(date) },
                            )
                        }
                    }
                }
            }
        }
    }
}
