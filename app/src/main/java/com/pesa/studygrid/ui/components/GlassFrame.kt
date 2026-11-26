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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarData
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pesa.studygrid.ui.theme.LocalAero

private val TAB_HEIGHT = 34.dp

/**
 * Thin clear-glass edge drawn over a panel: rim brighter at the top, glossy band on the upper third,
 * soft outer shadow. The body is whatever the caller draws (usually [glass]).
 */
@Composable
fun Modifier.glassRim(cornerRadius: Dp): Modifier {
    val dark = LocalAero.current.dark
    return drawWithContent {
        drawContent()
        val r = cornerRadius.toPx()
        val sw = 1.25.dp.toPx()
        drawRoundRect(
            Brush.verticalGradient(listOf(Color.White.copy(alpha = if (dark) 0.5f else 0.7f), Color.White.copy(alpha = 0.15f))),
            topLeft = Offset(sw / 2, sw / 2),
            size = Size(size.width - sw, size.height - sw),
            cornerRadius = CornerRadius(r - sw / 2),
            style = Stroke(sw),
        )
        val gloss = size.height * 0.35f
        clipPath(Path().apply { addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(r))) }) {
            drawRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.25f), Color.Transparent), 0f, gloss), size = Size(size.width, gloss))
        }
    }
}

/** Glass capsule carrying a title (sits on top of a [GlassCard]'s edge). */
@Composable
fun GlassTab(title: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .height(TAB_HEIGHT)
            .glass(cornerRadius = TAB_HEIGHT / 2)
            .glassRim(TAB_HEIGHT / 2)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            title.uppercase(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.5.sp,
                shadow = Shadow(Color.Black.copy(alpha = 0.3f), Offset(0f, 1.5f), 3f), // legibility over photos
            ),
        )
    }
}

/**
 * Soap-bubble body for pills and circles: mostly clear, light [tint] wash, colored glow at the bottom,
 * thin bright rim with a faint iridescent film, specular highlight upper-left.
 */
internal fun DrawScope.drawBubble(tint: Color, dark: Boolean, tintAlpha: Float = 0.22f) {
    val w = size.width
    val h = size.height
    val round = CornerRadius(h / 2)
    clipPath(Path().apply { addRoundRect(RoundRect(0f, 0f, w, h, round)) }) {
        drawRect(Color.White.copy(alpha = if (dark) 0.10f else 0.26f))
        drawRect(tint.copy(alpha = tintAlpha))
        drawRect(Brush.radialGradient(listOf(tint.copy(alpha = 0.45f), Color.Transparent), Offset(w / 2, h), h * 1.1f))
    }
    val sw = 1.dp.toPx()
    drawRoundRect(Brush.sweepGradient(FILM, Offset(w / 2, h / 2)), Offset(sw, sw), Size(w - 2 * sw, h - 2 * sw), CornerRadius(h / 2 - sw), style = Stroke(sw * 1.3f))
    drawRoundRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.95f), Color.White.copy(alpha = 0.3f))), Offset(0.5.dp.toPx(), 0.5.dp.toPx()), Size(w - 1.dp.toPx(), h - 1.dp.toPx()), CornerRadius(h / 2 - 0.5.dp.toPx()), style = Stroke(0.75.dp.toPx()))
    drawOval(
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.8f), Color.White.copy(alpha = 0.05f)), startY = h * 0.05f, endY = h * 0.5f),
        topLeft = Offset(h * 0.28f, h * 0.07f),
        size = Size(minOf(w * 0.45f, h * 1.1f), h * 0.36f),
    )
}

/** Transparent glass bubble: clear body, upper-left specular, bright rim, faint accent glow at the bottom. */
@Composable
fun GlassRoundButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = 38.dp,
    tintAlpha: Float = 0.12f,
    content: @Composable BoxScope.() -> Unit,
) {
    val a = LocalAero.current
    // Outer box is the >=48dp touch target; the drawn button stays [size].
    Box(
        modifier
            .size(maxOf(size, 48.dp))
            .clip(CircleShape)
            .semantics { this.contentDescription = contentDescription }
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(size)
                .clip(CircleShape)
                .drawBehind { drawBubble(a.accent, a.dark, tintAlpha = tintAlpha) },
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}

/** Card with a clear-glass panel, thin glass edge, glass title capsule on top and a corner slot for buttons. */
@Composable
fun GlassCard(
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
                .glassRim(24.dp)
                .padding(start = 22.dp, end = 22.dp, top = TAB_HEIGHT / 2 + 16.dp, bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
        GlassTab(title, Modifier.align(Alignment.TopCenter).widthIn(max = 260.dp).semantics { heading() })
        Box(Modifier.align(Alignment.TopEnd).padding(top = TAB_HEIGHT / 2 + 16.dp, end = 18.dp), content = corner)
    }
}

/** Aviso (snackbar) con el estilo de cristal de la app. */
@Composable
fun GlassSnackbar(data: SnackbarData) {
    val aero = LocalAero.current
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(aero.dialogBg)
            .glassRim(20.dp)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(data.visuals.message, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
        data.visuals.actionLabel?.let { BubbleButton(it, aero.accent, onClick = { data.performAction() }) }
    }
}
