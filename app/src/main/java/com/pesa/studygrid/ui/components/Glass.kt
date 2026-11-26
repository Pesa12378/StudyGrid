package com.pesa.studygrid.ui.components

import androidx.compose.foundation.layout.heightIn
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.view.WindowManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import android.provider.Settings
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.pesa.studygrid.R
import com.pesa.studygrid.ui.theme.LocalAero
import android.graphics.BitmapFactory
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import kotlin.math.sin
import kotlin.random.Random

/** The background, recorded once so each glass panel can redraw (blur + refract) the part behind it. */
val LocalBackdrop = staticCompositionLocalOf<GraphicsLayer> { error("Backdrop not provided") }

// Bends the backdrop near a rounded-rect edge like a thick lens, with a slight RGB split and a rim light.
private const val LIQUID_GLASS_AGSL = """
uniform shader content;
uniform float2 size;
uniform float radius;
uniform float bevel;
uniform float depth;

float sdRoundRect(float2 p, float2 b, float r) {
    float2 q = abs(p) - b + r;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
}

half4 main(float2 coord) {
    float2 hs = size * 0.5;
    float2 p = coord - hs;
    float d = sdRoundRect(p, hs, radius);
    float t = clamp(1.0 + d / bevel, 0.0, 1.0);
    float2 e = float2(1.0, 0.0);
    float2 n = float2(
        sdRoundRect(p + e.xy, hs, radius) - sdRoundRect(p - e.xy, hs, radius),
        sdRoundRect(p + e.yx, hs, radius) - sdRoundRect(p - e.yx, hs, radius));
    n = n / max(length(n), 0.0001);
    float bend = t * t * depth;
    float2 src = coord - n * bend;
    float split = bend * 0.18;
    half4 c = content.eval(src);
    c.r = content.eval(src - n * split).r;
    c.b = content.eval(src + n * split).b;
    float light = max(dot(n, normalize(float2(-0.6, -1.0))), 0.0);
    c.rgb += half3(pow(t, 4.0) * light * 0.45);
    return c;
}
"""

/**
 * Liquid Glass panel: blurs and refracts the [LocalBackdrop] behind it, then adds tint, gloss and a rim light.
 * Only valid on content in the same window as the backdrop (not inside Dialogs).
 */
private val GLASS_BLUR = 8.dp
private val GLASS_BEVEL = 30.dp
private val GLASS_DEPTH = 30.dp

@Composable
fun Modifier.glass(cornerRadius: Dp = 24.dp): Modifier {
    val tint = LocalAero.current.glassTint
    val backdrop = LocalBackdrop.current
    val layer = rememberGraphicsLayer()
    val shader = remember { RuntimeShader(LIQUID_GLASS_AGSL) }
    var origin by remember { mutableStateOf(Offset.Zero) }
    val shape = RoundedCornerShape(cornerRadius)
    return this
        .onGloballyPositioned { origin = it.positionInRoot() }
        .clip(shape)
        .drawWithCache {
            shader.setFloatUniform("size", size.width, size.height)
            shader.setFloatUniform("radius", cornerRadius.toPx().coerceAtMost(size.minDimension / 2))
            shader.setFloatUniform("bevel", GLASS_BEVEL.toPx())
            shader.setFloatUniform("depth", GLASS_DEPTH.toPx())
            val blurPx = GLASS_BLUR.toPx()
            val effect = RenderEffect.createChainEffect(
                RenderEffect.createRuntimeShaderEffect(shader, "content"),
                RenderEffect.createBlurEffect(blurPx, blurPx, Shader.TileMode.CLAMP),
            ).asComposeRenderEffect()
            val outline = shape.createOutline(size, layoutDirection, this)
            val clip = Path().apply { addOutline(outline) }
            val rim = Brush.linearGradient(
                0f to Color.White.copy(alpha = 0.8f),
                0.35f to Color.White.copy(alpha = 0.12f),
                1f to Color.White.copy(alpha = 0.4f),
                start = Offset.Zero,
                end = Offset(size.width, size.height),
            )
            val gloss = Brush.verticalGradient(0f to Color.White.copy(alpha = 0.14f), 0.4f to Color.Transparent)
            onDrawBehind {
                layer.clip = true
                layer.renderEffect = effect
                layer.record { translate(-origin.x, -origin.y) { drawLayer(backdrop) } }
                clipPath(clip) { drawLayer(layer) }
                drawOutline(outline, tint)
                drawOutline(outline, gloss)
                drawOutline(outline, rim, style = Stroke(1.5.dp.toPx()))
            }
        }
}

private const val BUBBLE_FRAME_SECONDS = 1f / 30f

/** x: 0..1 of width; speed: screen heights per second; r: radius as fraction of width; phase: 0..1 start offset. */
private class Bubble(val x: Float, val speed: Float, val r: Float, val phase: Float, val wobble: Float)

private val BACKGROUNDS = intArrayOf(
    R.drawable.bg_meadow, R.drawable.bg_hills, R.drawable.bg_meadow_crest,
    R.drawable.bg_sea, R.drawable.bg_pool, R.drawable.bg_ripples,
)
private const val SLIDE_SECONDS = 40f
private const val FADE_SECONDS = 3f

private class Slide(val slot: Int, val current: ImageBitmap, val next: ImageBitmap)

/** ContentScale.Crop by hand: cover the canvas, centered. */
private fun DrawScope.drawCover(img: ImageBitmap, alpha: Float) {
    val scale = maxOf(size.width / img.width, size.height / img.height)
    val dw = (img.width * scale).toInt()
    val dh = (img.height * scale).toInt()
    drawImage(
        img,
        dstOffset = IntOffset(((size.width - dw) / 2).toInt(), ((size.height - dh) / 2).toInt()),
        dstSize = IntSize(dw, dh),
        alpha = alpha,
    )
}

internal val FILM = listOf(Color(0xFFFF6EC7), Color(0xFFFFE66D), Color(0xFF6EF0FF), Color(0xFFA78BFA), Color(0xFFFF6EC7))
    .map { it.copy(alpha = 0.30f) }

/** Background photo + soap bubbles slowly rising behind the cards (detail for the glass to bend). */
@Composable
fun AeroBackground(modifier: Modifier = Modifier) {
    val a = LocalAero.current
    val bubbles = remember {
        val rnd = Random(42)
        List(18) {
            val t = rnd.nextFloat()
            Bubble(rnd.nextFloat(), 0.02f + rnd.nextFloat() * 0.04f, 0.025f + t * t * 0.11f, rnd.nextFloat(), 0.01f + rnd.nextFloat() * 0.03f)
        }
    }
    // Respect "remove animations" in system accessibility settings.
    val context = LocalContext.current
    val animate = remember { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f }
    var time by remember { mutableFloatStateOf(0f) }
    if (animate) {
        LaunchedEffect(Unit) {
            val start = withFrameNanos { it }
            while (true) withFrameNanos { now ->
                // Bubbles move < 1px per frame: redraw at ~30 fps instead of the display's 120 Hz to save battery.
                val t = (now - start) / 1_000_000_000f
                if (t - time >= BUBBLE_FRAME_SECONDS) time = t
            }
        }
    }
    // Photo slideshow: each day starts on a different photo; only the current and next are decoded (off the main thread).
    val start = remember { LocalDate.now().dayOfYear }
    val slot by remember { derivedStateOf { (time / SLIDE_SECONDS).toInt() } }
    val slide by produceState<Slide?>(null, slot) {
        val res = context.resources
        fun load(i: Int) = BitmapFactory.decodeResource(res, BACKGROUNDS[(start + i) % BACKGROUNDS.size]).asImageBitmap()
        val prev = value
        value = withContext(Dispatchers.IO) {
            Slide(slot, if (prev?.slot == slot - 1) prev.next else load(slot), load(slot + 1))
        }
    }
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        slide?.let { s ->
            drawCover(s.current, 1f)
            // Fade into the next photo during the slot's last seconds; timed against the slot this pair was loaded for,
            // so a late load never snaps back to the previous photo.
            val fade = ((time - s.slot * SLIDE_SECONDS - (SLIDE_SECONDS - FADE_SECONDS)) / FADE_SECONDS).coerceIn(0f, 1f)
            if (fade > 0f) drawCover(s.next, fade)
        }
        drawRect(a.scrim)

        for (b in bubbles) {
            val r = b.r * w
            val travel = h + 4 * r
            val y = h + 2 * r - ((b.phase * travel + time * b.speed * h) % travel)
            val x = b.x * w + sin(time * 0.7f + b.phase * 6.28f) * b.wobble * w
            soapBubble(Offset(x, y), r, time + b.phase * 10f)
        }
    }
}

private fun DrawScope.soapBubble(c: Offset, r: Float, t: Float) {
    // Body: clear center, brighter toward the rim (fresnel).
    drawCircle(
        Brush.radialGradient(
            0f to Color.Transparent,
            0.68f to Color.White.copy(alpha = 0.03f),
            0.90f to Color.White.copy(alpha = 0.14f),
            1f to Color.White.copy(alpha = 0.42f),
            center = c,
            radius = r,
        ),
        r,
        c,
    )
    // Thin-film iridescence, slowly swirling.
    rotate(t * 25f, c) {
        drawCircle(Brush.sweepGradient(FILM, c), r * 0.92f, c, style = Stroke(r * 0.13f))
    }
    drawCircle(Color.White.copy(alpha = 0.55f), r, c, style = Stroke(maxOf(1f, r * 0.025f)))
    // Main highlight: crescent on the upper-left + soft glint.
    val arcBox = r * 0.78f
    drawArc(
        Color.White.copy(alpha = 0.85f), 195f, 55f, false,
        topLeft = Offset(c.x - arcBox, c.y - arcBox), size = Size(arcBox * 2, arcBox * 2),
        style = Stroke(r * 0.09f, cap = StrokeCap.Round),
    )
    val glint = Offset(c.x - 0.36f * r, c.y - 0.44f * r)
    drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.9f), Color.Transparent), glint, r * 0.16f), r * 0.16f, glint)
    // Secondary reflection, lower-right.
    val arc2 = r * 0.8f
    drawArc(
        Color.White.copy(alpha = 0.35f), 15f, 45f, false,
        topLeft = Offset(c.x - arc2, c.y - arc2), size = Size(arc2 * 2, arc2 * 2),
        style = Stroke(r * 0.05f, cap = StrokeCap.Round),
    )
}

private val DarkText = Color(0xFF06121F)

private fun contrast(a: Color, b: Color): Float {
    val la = a.luminance() + 0.05f
    val lb = b.luminance() + 0.05f
    return maxOf(la, lb) / minOf(la, lb)
}

/** White or dark text, whichever has the higher WCAG contrast on [background]. */
internal fun readableTextOn(background: Color): Color =
    if (contrast(background, Color.White) >= contrast(background, DarkText)) Color.White else DarkText

/** Soap-bubble pill: clear body lightly tinted with [color], iridescent rim, specular and glow (see [drawBubble]). */
@Composable
fun BubbleButton(
    text: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val dark = LocalAero.current.dark
    // Dark tone of the button color on light glass, pale tone on dark glass; both keep the color identity.
    val textColor = if (dark) lerp(color, Color.White, 0.75f) else lerp(color, Color.Black, 0.6f)
    // Outer box keeps the 48dp touch target; the drawn pill is a slim 40dp.
    Box(
        modifier
            .heightIn(min = 48.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(RoundedCornerShape(50))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.heightIn(min = 40.dp).clip(RoundedCornerShape(50)).drawBehind { drawBubble(color, dark) }.padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text, color = textColor, fontWeight = FontWeight.Medium, fontSize = 15.sp)
        }
    }
}

/** Dialog whose window blurs the app behind it (cross-window blur; dialogs can't sample the backdrop layer). */
@Composable
fun GlassDialog(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect {
            window?.run {
                addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                attributes = attributes.apply { blurBehindRadius = 48 }
                setDimAmount(0.25f)
            }
        }
        val a = LocalAero.current
        val shape = RoundedCornerShape(28.dp)
        Column(
            Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .clip(shape)
                .background(a.dialogBg)
                .glassRim(28.dp)
                .padding(30.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content,
        )
    }
}
