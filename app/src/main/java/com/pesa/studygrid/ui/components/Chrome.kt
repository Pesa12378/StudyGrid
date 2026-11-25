package com.pesa.studygrid.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pesa.studygrid.ui.theme.LocalAero

private val TAB_HEIGHT = 34.dp
private val BEZEL = 11.dp

/**
 * Thick polished-metal frame drawn over a panel: metal ring, dark outer edge, inner groove,
 * inset shadow and a specular streak on the top edge.
 */
@Composable
fun Modifier.chromeBezel(cornerRadius: Dp): Modifier {
    val chrome = LocalAero.current.chrome
    return drawWithContent {
        val bw = BEZEL.toPx()
        val r = cornerRadius.toPx()
        val w = size.width
        val h = size.height
        // Inset shadow just inside the ring, so the panel reads as recessed.
        drawRect(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.22f), Color.Transparent), startY = bw, endY = bw + 14.dp.toPx()))
        drawContent()
        // Two-tone metal: outer band lit from above, inner band with the gradient reversed (reads as a bevel step).
        val outer = bw * 0.6f
        val inner = bw - outer
        drawRoundRect(
            Brush.verticalGradient(chrome),
            topLeft = Offset(outer / 2, outer / 2),
            size = Size(w - outer, h - outer),
            cornerRadius = CornerRadius(r - outer / 2),
            style = Stroke(outer),
        )
        drawRoundRect(
            Brush.verticalGradient(chrome.reversed()),
            topLeft = Offset(outer + inner / 2, outer + inner / 2),
            size = Size(w - 2 * outer - inner, h - 2 * outer - inner),
            cornerRadius = CornerRadius(r - outer - inner / 2),
            style = Stroke(inner),
        )
        drawRoundRect(
            Color.Black.copy(alpha = 0.30f),
            topLeft = Offset(outer, outer),
            size = Size(w - 2 * outer, h - 2 * outer),
            cornerRadius = CornerRadius(r - outer),
            style = Stroke(0.75.dp.toPx()),
        )
        // Screws on the outer band, clear of the rounded corners and the title plate.
        val screwR = outer * 0.36f
        val inset = r + 6.dp.toPx()
        for (s in listOf(Offset(inset, outer / 2), Offset(w - inset, outer / 2), Offset(inset, h - outer / 2), Offset(w - inset, h - outer / 2))) {
            drawCircle(Brush.linearGradient(chrome.reversed(), start = s - Offset(screwR, screwR), end = s + Offset(screwR, screwR)), screwR, s)
            drawCircle(Color.Black.copy(alpha = 0.5f), screwR, s, style = Stroke(0.75.dp.toPx()))
            drawLine(Color.Black.copy(alpha = 0.55f), s - Offset(screwR * 0.6f, -screwR * 0.6f), s + Offset(screwR * 0.6f, -screwR * 0.6f), 0.9.dp.toPx())
        }
        drawRoundRect(Color.Black.copy(alpha = 0.40f), cornerRadius = CornerRadius(r), style = Stroke(1.dp.toPx()))
        drawRoundRect(
            Color.Black.copy(alpha = 0.45f),
            topLeft = Offset(bw, bw),
            size = Size(w - 2 * bw, h - 2 * bw),
            cornerRadius = CornerRadius(r - bw),
            style = Stroke(1.dp.toPx()),
        )
        drawRoundRect(
            Color.White.copy(alpha = 0.35f),
            topLeft = Offset(bw + 1.dp.toPx(), bw + 1.dp.toPx()),
            size = Size(w - 2 * bw - 2.dp.toPx(), h - 2 * bw - 2.dp.toPx()),
            cornerRadius = CornerRadius(r - bw),
            style = Stroke(0.75.dp.toPx()),
        )
        drawRoundRect(
            Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.95f), Color.Transparent), startX = r, endX = w - r),
            topLeft = Offset(r, 1.5.dp.toPx()),
            size = Size(w - 2 * r, 1.5.dp.toPx()),
            cornerRadius = CornerRadius(1.dp.toPx()),
        )
    }
}

/** Raised metal plate with slanted sides, carrying a title (sits on top of a [ChromeCard]'s bezel). */
@Composable
fun ChromeTab(title: String, modifier: Modifier = Modifier) {
    val a = LocalAero.current
    Box(
        modifier
            .height(TAB_HEIGHT)
            .drawBehind {
                val slant = 14.dp.toPx()
                val plate = Path().apply {
                    moveTo(0f, size.height)
                    lineTo(slant, 0f)
                    lineTo(size.width - slant, 0f)
                    lineTo(size.width, size.height)
                    close()
                }
                drawPath(plate, Brush.verticalGradient(a.chrome))
                drawPath(plate, Color.Black.copy(alpha = 0.40f), style = Stroke(1.dp.toPx()))
                drawLine(Color.White.copy(alpha = 0.9f), Offset(slant + 2.dp.toPx(), 1.5.dp.toPx()), Offset(size.width - slant - 2.dp.toPx(), 1.5.dp.toPx()), 1.dp.toPx())
            }
            .padding(horizontal = 30.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            title.uppercase(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(
                color = a.tabText,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.5.sp,
                shadow = Shadow(Color.White.copy(alpha = 0.8f), Offset(0f, 1.5f), 0f), // embossed
            ),
        )
    }
}

/** Round chrome-ring button with a glossy gel center. */
@Composable
fun ChromeRoundButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = 38.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val a = LocalAero.current
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .drawBehind {
                val r = this.size.minDimension / 2
                val c = center
                drawCircle(Brush.linearGradient(a.chrome, start = Offset.Zero, end = Offset(this.size.width, this.size.height)), r, c)
                drawCircle(Color.Black.copy(alpha = 0.45f), r, c, style = Stroke(1.dp.toPx()))
                val inner = r * 0.74f
                drawCircle(Brush.verticalGradient(listOf(a.accent, a.accentDeep), startY = c.y - inner, endY = c.y + inner), inner, c)
                drawCircle(Color.Black.copy(alpha = 0.5f), inner, c, style = Stroke(1.dp.toPx()))
                drawOval(
                    Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.7f), Color.White.copy(alpha = 0.05f)), startY = c.y - inner, endY = c.y),
                    topLeft = Offset(c.x - inner * 0.7f, c.y - inner * 0.92f),
                    size = Size(inner * 1.4f, inner * 0.9f),
                )
            }
            .semantics { this.contentDescription = contentDescription }
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/** Card in a media-player-skin style: liquid-glass panel, metal bezel, title plate on top, chrome edit button. */
@Composable
fun ChromeCard(
    title: String,
    modifier: Modifier = Modifier,
    corner: @Composable BoxScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier) {
        Column(
            Modifier
                .padding(top = TAB_HEIGHT / 2)
                .fillMaxWidth()
                .glass(cornerRadius = 24.dp)
                .chromeBezel(24.dp)
                .padding(start = 22.dp, end = 22.dp, top = TAB_HEIGHT / 2 + 16.dp, bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
        ChromeTab(title, Modifier.align(Alignment.TopCenter).widthIn(max = 260.dp))
        Box(Modifier.align(Alignment.TopEnd).padding(top = TAB_HEIGHT / 2 + 16.dp, end = 18.dp), content = corner)
    }
}
