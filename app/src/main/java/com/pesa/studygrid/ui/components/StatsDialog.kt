package com.pesa.studygrid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.content.FileProvider
import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import com.pesa.studygrid.data.DailyLogEntity
import com.pesa.studygrid.ui.GoalStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pesa.studygrid.ui.MonthStat
import com.pesa.studygrid.ui.theme.LocalAero
import java.time.format.TextStyle
import java.util.Locale

private val ES = Locale("es")
private const val BAR_AREA = 90 // dp

@Composable
fun StatsDialog(
    goalTitle: String,
    color: Color,
    stats: List<MonthStat>,
    logs: Map<LocalDate, DailyLogEntity>,
    goalStats: GoalStats,
    onDismiss: () -> Unit,
) {
    val a = LocalAero.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val layer = rememberGraphicsLayer()
    val max = maxOf(1, stats.maxOf { it.activeDays })
    GlassDialog(onDismiss) {
        // Share image: composed for real but 1dp-clipped so it stays invisible; the layer records its full size.
        Box(Modifier.size(1.dp).clipToBounds().clearAndSetSemantics {}) {
            Box(Modifier.wrapContentSize(Alignment.TopStart, unbounded = true)) {
                Column(
                    Modifier
                        .drawWithContent { layer.record { this@drawWithContent.drawContent() }; drawLayer(layer) }
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(goalTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    ContributionGraph(logs, {}, goalColor = color, scroll = false)
                    LevelLegend(color)
                    Text(
                        "Racha actual ${goalStats.current} · Mejor racha ${goalStats.best} · ${goalStats.total} días activos",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        Text(goalTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text("Días activos por mes", style = MaterialTheme.typography.labelMedium, color = a.muted)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            stats.forEach { s ->
                val label = s.month.month.getDisplayName(TextStyle.SHORT, ES).trimEnd('.')
                val avg = "%.1f".format(ES, s.avgLevel)
                Column(
                    Modifier.weight(1f).semantics(mergeDescendants = true) {
                        contentDescription = "$label ${s.month.year}: ${s.activeDays} días activos, nivel promedio $avg"
                    },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("${s.activeDays}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Box(Modifier.fillMaxWidth().height(BAR_AREA.dp), contentAlignment = Alignment.BottomCenter) {
                        Box(
                            Modifier
                                .fillMaxWidth(0.6f)
                                // Minimum 2dp so empty months still show a baseline.
                                .height(maxOf(2f, BAR_AREA * s.activeDays / max.toFloat()).dp)
                                .background(color, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)),
                        )
                    }
                    Text(label, style = MaterialTheme.typography.labelSmall, color = a.muted)
                    Text(
                        if (s.activeDays == 0) "" else "prom. $avg",
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            BubbleButton("Compartir", color, {
                scope.launch {
                    val ok = runCatching {
                        val bmp = layer.toImageBitmap().asAndroidBitmap()
                        val uri = withContext(Dispatchers.IO) {
                            val dir = File(context.cacheDir, "shared").apply { mkdirs() }
                            val f = File(dir, "studygrid-" + goalTitle.replace(Regex("[^\\p{L}\\p{N}]+"), "_").take(40) + ".png")
                            f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
                            FileProvider.getUriForFile(context, context.packageName + ".fileprovider", f)
                        }
                        val send = Intent(Intent.ACTION_SEND).setType("image/png")
                            .putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        context.startActivity(Intent.createChooser(send, "Compartir mapa de calor"))
                    }.isSuccess
                    if (!ok) Toast.makeText(context, "No se pudo compartir la imagen.", Toast.LENGTH_SHORT).show()
                }
            }, Modifier.semantics { contentDescription = "Compartir mapa de calor de $goalTitle como imagen" })
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    }
}
