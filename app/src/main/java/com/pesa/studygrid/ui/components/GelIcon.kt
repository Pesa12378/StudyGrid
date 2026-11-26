package com.pesa.studygrid.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp
import com.pesa.studygrid.ui.theme.LocalAero

enum class GelGlyph { Flame, Trophy, Calendar, Sun, Moon }

/** 24dp glyph in the gel style of the app buttons: theme-tinted gradient fill, dark rim, glossy upper half. */
@Composable
fun GelIcon(glyph: GelGlyph, modifier: Modifier = Modifier) {
    val a = LocalAero.current
    Box(
        modifier.size(24.dp).drawBehind {
            val u = size.minDimension / 24f
            fun Path.p(x: Float, y: Float, first: Boolean = false) = if (first) moveTo(x * u, y * u) else lineTo(x * u, y * u)
            val shape = Path()
            when (glyph) {
                GelGlyph.Flame -> shape.apply {
                    // Tall flame, tip curling right, small notch on the left shoulder.
                    p(14f, 0.8f, true); cubicTo(14.3f * u, 5f * u, 19f * u, 8f * u, 19f * u, 14.5f * u)
                    cubicTo(19f * u, 19f * u, 15.8f * u, 22.8f * u, 12f * u, 22.8f * u)
                    cubicTo(8.2f * u, 22.8f * u, 5f * u, 19.5f * u, 5f * u, 15f * u)
                    cubicTo(5f * u, 12f * u, 6.3f * u, 9.8f * u, 8.3f * u, 7.5f * u)
                    cubicTo(8.4f * u, 9.6f * u, 9.2f * u, 10.8f * u, 10.6f * u, 11.2f * u)
                    cubicTo(9.6f * u, 7f * u, 11f * u, 3f * u, 14f * u, 0.8f * u); close()
                }
                GelGlyph.Trophy -> shape.apply {
                    p(6f, 2.5f, true); p(18f, 2.5f); p(18f, 9f); cubicTo(18f * u, 12.5f * u, 15f * u, 14f * u, 13f * u, 14.5f * u)
                    p(13f, 18f); p(16.5f, 18f); p(16.5f, 21.5f); p(7.5f, 21.5f); p(7.5f, 18f); p(11f, 18f); p(11f, 14.5f)
                    cubicTo(9f * u, 14f * u, 6f * u, 12.5f * u, 6f * u, 9f * u); close()
                    // handles
                    addRoundRect(androidx.compose.ui.geometry.RoundRect(2.5f * u, 4.5f * u, 6f * u, 7f * u, CornerRadius(u)))
                    addRoundRect(androidx.compose.ui.geometry.RoundRect(18f * u, 4.5f * u, 21.5f * u, 7f * u, CornerRadius(u)))
                }
                GelGlyph.Calendar -> shape.addRoundRect(androidx.compose.ui.geometry.RoundRect(3f * u, 4f * u, 21f * u, 21.5f * u, CornerRadius(3.5f * u)))
                GelGlyph.Sun -> shape.addOval(androidx.compose.ui.geometry.Rect(6f * u, 6f * u, 18f * u, 18f * u))
                GelGlyph.Moon -> shape.apply {
                    p(15f, 2.5f, true); cubicTo(8f * u, 3f * u, 4f * u, 8f * u, 4f * u, 13f * u)
                    cubicTo(4f * u, 18.5f * u, 8.5f * u, 22f * u, 13f * u, 22f * u)
                    cubicTo(17f * u, 22f * u, 20f * u, 20f * u, 21f * u, 17f * u)
                    cubicTo(14f * u, 18f * u, 10f * u, 11f * u, 15f * u, 2.5f * u); close()
                }
            }
            drawPath(shape, Brush.verticalGradient(if (glyph == GelGlyph.Flame) listOf(Color(0xFFFF5A1F), Color(0xFFFFB300)) else listOf(a.accent, a.accentDeep), 0f, size.height))
            // Specular gloss on the upper half, clipped to the glyph.
            clipPath(shape) {
                drawRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.65f), Color.White.copy(alpha = 0.02f)), 0f, size.height * 0.55f), size = Size(size.width, size.height * 0.55f))
            }
            drawPath(shape, Color.Black.copy(alpha = 0.5f), style = Stroke(1.dp.toPx()))
            if (glyph == GelGlyph.Flame) drawPath(Path().apply {
                p(12f, 22f, true); cubicTo(9.6f * u, 22f * u, 8.2f * u, 20.2f * u, 8.2f * u, 18f * u)
                cubicTo(8.2f * u, 15.8f * u, 10f * u, 14.8f * u, 11f * u, 12.8f * u)
                cubicTo(12f * u, 14.5f * u, 15.8f * u, 15.8f * u, 15.8f * u, 18.3f * u)
                cubicTo(15.8f * u, 20.4f * u, 14.2f * u, 22f * u, 12f * u, 22f * u); close()
            }, Brush.verticalGradient(listOf(Color(0xFFFFF3B0), Color(0xFFFFD54F)), 12f * u, 22f * u))
            when (glyph) {
                GelGlyph.Calendar -> {
                    drawRect(Color.Black.copy(alpha = 0.35f), Offset(3f * u, 9f * u), Size(18f * u, 1.2f * u))
                    for (r in 0..1) for (c in 0..2) drawCircle(Color.White.copy(alpha = 0.85f), 1.1f * u, Offset((8f + c * 4f) * u, (13.5f + r * 4f) * u))
                }
                GelGlyph.Sun -> for (i in 0 until 8) {
                    val ang = Math.toRadians(i * 45.0)
                    val dx = Math.cos(ang).toFloat(); val dy = Math.sin(ang).toFloat()
                    val c = Offset(12f * u, 12f * u)
                    drawLine(a.accentDeep, c + Offset(dx, dy) * 8.2f * u, c + Offset(dx, dy) * 11f * u, 2f * u, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                }
                else -> {}
            }
        },
    ) {}
}
