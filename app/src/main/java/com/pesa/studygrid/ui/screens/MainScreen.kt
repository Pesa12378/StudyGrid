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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
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
import com.pesa.studygrid.ui.components.ConfirmDialog
import com.pesa.studygrid.ui.components.DeleteGoalDialog
import com.pesa.studygrid.ui.components.GoalCard
import com.pesa.studygrid.ui.components.GoalEditorDialog
import com.pesa.studygrid.ui.components.LogEditorDialog
import com.pesa.studygrid.ui.components.glass
import com.pesa.studygrid.ui.theme.Aqua
import com.pesa.studygrid.ui.theme.LocalAero
import com.pesa.studygrid.R
import com.pesa.studygrid.ui.components.LocalBackdrop
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
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
    LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it) } }

    var goalDialog by remember { mutableStateOf<GoalDialog?>(null) }
    var deleting by remember { mutableStateOf<GoalEntity?>(null) }
    var logTarget by remember { mutableStateOf<Pair<Long, LocalDate>?>(null) }
    var pendingImport by remember { mutableStateOf<Uri?>(null) }
    var menuOpen by remember { mutableStateOf(false) }

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
                        title = { Text("StudyGrid", fontWeight = FontWeight.Bold) },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                        actions = {
                            IconButton(onClick = onToggleDark) {
                                Image(
                                    painterResource(if (dark) R.drawable.emoji_sun else R.drawable.emoji_crescent_moon),
                                    contentDescription = if (dark) "Cambiar a modo claro" else "Cambiar a modo oscuro",
                                    modifier = Modifier.size(28.dp),
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
                                    DropdownMenuItem(
                                        text = { Text("Importar respaldo (.json)") },
                                        onClick = {
                                            menuOpen = false
                                            // Some file managers label .json as octet-stream or text.
                                            importLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/plain"))
                                        },
                                    )
                                }
                            }
                        },
                    )
                },
                floatingActionButton = {
                    Box(
                        Modifier
                            .size(66.dp)
                            .glass(cornerRadius = 33.dp, tint = Aqua.copy(alpha = 0.30f), bevel = 18.dp, depth = 14.dp)
                            .clickable(role = Role.Button) { goalDialog = GoalDialog.New },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Add, "Nueva meta", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(30.dp))
                    }
                },
                snackbarHost = { SnackbarHost(snackbar) },
            ) { padding ->
                val list = goals
                when {
                    list == null -> Unit
                    list.isEmpty() -> EmptyState(Modifier.padding(padding))
                    else -> LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 8.dp, bottom = padding.calculateBottomPadding() + 104.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        items(list, key = { it.goal.id }) { ui ->
                            GoalCard(
                                ui = ui,
                                onBumpToday = { vm.bumpToday(ui.goal.id) },
                                onDayClick = { date -> logTarget = ui.goal.id to date },
                                onEdit = { goalDialog = GoalDialog.Edit(ui.goal) },
                            )
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
                date = date,
                current = ui.logs[date],
                onSave = { level, note -> vm.saveLog(goalId, date, level, note); logTarget = null },
                onDismiss = { logTarget = null },
            )
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier) {
    Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(
            Modifier.glass().padding(28.dp),
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
