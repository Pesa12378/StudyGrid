package com.pesa.studygrid.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.graphicsLayer
import com.pesa.studygrid.ui.GoalUi
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pesa.studygrid.data.GoalEntity
import com.pesa.studygrid.ui.StudyViewModel
import com.pesa.studygrid.ui.components.AeroBackground
import com.pesa.studygrid.ui.components.GlassRoundButton
import com.pesa.studygrid.ui.components.GlassSnackbar
import com.pesa.studygrid.ui.components.ConfirmDialog
import com.pesa.studygrid.ui.components.DeleteGoalDialog
import com.pesa.studygrid.ui.components.ReminderDialog
import com.pesa.studygrid.ui.components.GoalCard
import com.pesa.studygrid.ui.components.GoalEditorDialog
import com.pesa.studygrid.ui.components.LogEditorDialog
import com.pesa.studygrid.ui.components.StatsDialog
import com.pesa.studygrid.ui.monthlyStats
import com.pesa.studygrid.ui.components.glass
import com.pesa.studygrid.ui.components.glassRim
import com.pesa.studygrid.ui.theme.LocalAero
import com.pesa.studygrid.R
import com.pesa.studygrid.ui.components.LocalBackdrop
import androidx.compose.foundation.Image
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.pesa.studygrid.ui.components.GelGlyph
import com.pesa.studygrid.ui.components.GelIcon
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import kotlinx.coroutines.flow.collectLatest
import java.time.LocalDate

private sealed interface GoalDialog {
    data object New : GoalDialog
    data class Edit(val goal: GoalEntity) : GoalDialog
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(dark: Boolean, onToggleDark: () -> Unit, vm: StudyViewModel = viewModel()) {
    val goals by vm.goals.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        // collectLatest: a new message replaces the visible one instead of queueing (fast "+1" taps).
        vm.messages.collectLatest { m ->
            val result = snackbar.showSnackbar(
                m.text,
                actionLabel = if (m.undo != null) "Deshacer" else null,
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) vm.undo(m)
        }
    }

    var goalDialog by remember { mutableStateOf<GoalDialog?>(null) }
    var deleting by remember { mutableStateOf<GoalEntity?>(null) }
    var logTarget by remember { mutableStateOf<Pair<Long, LocalDate>?>(null) }
    var pendingImport by remember { mutableStateOf<Uri?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    var showReminder by remember { mutableStateOf(false) }
    var statsGoalId by remember { mutableStateOf<Long?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let(vm::export)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        pendingImport = uri
    }

    val backdrop = rememberGraphicsLayer()
    CompositionLocalProvider(LocalBackdrop provides backdrop) {
        Box(Modifier.fillMaxSize()) {
            AeroBackground(
                Modifier.fillMaxSize().drawWithContent {
                    backdrop.record { this@drawWithContent.drawContent() }
                    drawLayer(backdrop)
                },
            )
            Scaffold(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurface,
                topBar = {
                    TopAppBar(
                        title = { Text("StudyGrid", fontWeight = FontWeight.Bold, style = androidx.compose.material3.LocalTextStyle.current.copy(shadow = androidx.compose.ui.graphics.Shadow(Color.Black.copy(alpha = 0.35f), androidx.compose.ui.geometry.Offset(0f, 2f), 4f))) },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                        actions = {
                            IconButton(onClick = onToggleDark) {
                                GelIcon(
                                    if (dark) GelGlyph.Sun else GelGlyph.Moon,
                                    Modifier.semantics { contentDescription = if (dark) "Cambiar a modo claro" else "Cambiar a modo oscuro" },
                                )
                            }
                            Box {
                                IconButton(onClick = { menuOpen = true }) { Icon(Icons.Filled.MoreVert, "Más opciones") }
                                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                    DropdownMenuItem(
                                        text = { Text("Exportar respaldo (.json)") },
                                        onClick = {
                                            menuOpen = false
                                            exportLauncher.launch("studygrid-respaldo-${LocalDate.now()}.json")
                                        },
                                    )
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = { Text("Importar respaldo (.json)") },
                                        onClick = {
                                            menuOpen = false
                                            // Some file managers label .json as octet-stream or text.
                                            importLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/plain"))
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Recordatorio diario") },
                                        onClick = { menuOpen = false; showReminder = true },
                                    )
                                }
                            }
                        },
                    )
                },
                floatingActionButton = {
                    GlassRoundButton(onClick = { goalDialog = GoalDialog.New }, contentDescription = "Nueva meta", size = 56.dp, tintAlpha = 0.75f) {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = LocalAero.current.ink, modifier = Modifier.size(30.dp))
                    }
                },
                snackbarHost = { SnackbarHost(snackbar) { GlassSnackbar(it) } },
            ) { padding ->
                val list = goals
                // Local copy so dragging reorders instantly; re-synced whenever the DB emits.
                val local = remember { mutableStateListOf<GoalUi>() }
                LaunchedEffect(list) { local.clear(); local.addAll(list.orEmpty()) }
                val lazyState = rememberLazyListState()
                val reorderState = rememberReorderableLazyListState(lazyState) { from, to ->
                    local.add(to.index, local.removeAt(from.index))
                }
                when {
                    list == null -> Unit
                    list.isEmpty() -> EmptyState(Modifier.padding(padding))
                    // Starts below the transparent top bar so cards never scroll under the title.
                    else -> LazyColumn(
                        modifier = Modifier.padding(top = padding.calculateTopPadding()),
                        state = lazyState,
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = padding.calculateBottomPadding() + 104.dp + if (snackbar.currentSnackbarData != null) 80.dp else 0.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        items(local, key = { it.goal.id }) { ui ->
                            ReorderableItem(reorderState, key = ui.goal.id) { isDragging ->
                                Box(
                                    Modifier
                                        .graphicsLayer { if (isDragging) { scaleX = 1.02f; scaleY = 1.02f } }
                                        .longPressDraggableHandle(onDragStopped = { vm.reorder(local.map { it.goal.id }) }),
                                ) {
                                    GoalCard(
                                        ui = ui,
                                        onBumpToday = { vm.bumpToday(ui.goal.id) },
                                        onDayClick = { date -> logTarget = ui.goal.id to date },
                                        onEdit = { goalDialog = GoalDialog.Edit(ui.goal) },
                                        onStats = { statsGoalId = ui.goal.id },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    goalDialog?.let { dialog ->
        GoalEditorDialog(
            initial = (dialog as? GoalDialog.Edit)?.goal,
            onSave = { vm.saveGoal(it); goalDialog = null },
            onDelete = { deleting = (dialog as? GoalDialog.Edit)?.goal; goalDialog = null },
            onDismiss = { goalDialog = null },
        )
    }

    deleting?.let { goal ->
        DeleteGoalDialog(
            goal = goal,
            onConfirm = { vm.deleteGoal(goal); deleting = null },
            onDismiss = { deleting = null },
        )
    }

    if (showReminder) ReminderDialog(onDismiss = { showReminder = false })

    statsGoalId?.let { id ->
        val ui = goals?.find { it.goal.id == id }
        if (ui == null) statsGoalId = null
        else StatsDialog(ui.goal.title, Color(ui.goal.color), monthlyStats(ui.logs, LocalDate.now()), ui.logs, ui.stats) { statsGoalId = null }
    }

    pendingImport?.let { uri ->
        ConfirmDialog(
            title = "¿Importar respaldo?",
            text = "Esto reemplaza TODAS tus metas y registros actuales por los del archivo. Si el archivo es inválido, no se cambia nada.",
            confirmLabel = "Importar",
            onConfirm = { vm.import(uri); pendingImport = null },
            onDismiss = { pendingImport = null },
        )
    }

    logTarget?.let { (goalId, date) ->
        val ui = goals?.find { it.goal.id == goalId }
        if (ui == null) {
            logTarget = null
        } else {
            LogEditorDialog(
                goalTitle = ui.goal.title,
                color = Color(ui.goal.color),
                initialDate = date,
                logs = ui.logs,
                onSave = { day, level, note -> vm.saveLog(goalId, day, level, note) },
                onDismiss = { logTarget = null },
            )
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier) {
    Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(
            Modifier.glass().glassRim(24.dp).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Image(painterResource(R.drawable.emoji_bubbles), contentDescription = null, modifier = Modifier.size(56.dp))
            Text("Aún no tienes metas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "Toca + para crear tu primera meta de estudio.",
                style = MaterialTheme.typography.bodyMedium,
                color = LocalAero.current.muted,
                textAlign = TextAlign.Center,
            )
        }
    }
}
