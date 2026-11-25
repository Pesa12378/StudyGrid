package com.pesa.studygrid.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pesa.studygrid.data.AppDatabase
import com.pesa.studygrid.data.DailyLogEntity
import com.pesa.studygrid.data.GoalEntity
import com.pesa.studygrid.data.StudyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.pesa.studygrid.data.MAX_LEVEL
import com.pesa.studygrid.widget.StudyGridWidget
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.flow.drop
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** [week] = active days in the current Monday..Sunday week (for the weekly goal). */
data class GoalStats(val current: Int, val best: Int, val total: Int, val week: Int)

/** Snackbar text; [undo] non-null shows a "Deshacer" action that runs it. */
class UiMessage(val text: String, val undo: (suspend () -> Unit)? = null)

data class GoalUi(val goal: GoalEntity, val logs: Map<LocalDate, DailyLogEntity>, val stats: GoalStats)

/** Current streak counts back from today, or from yesterday if today isn't logged yet. */
fun computeStats(active: Set<LocalDate>, today: LocalDate): GoalStats {
    var best = 0
    var run = 0
    var prev: LocalDate? = null
    for (day in active.sorted()) {
        run = if (prev != null && day == prev.plusDays(1)) run + 1 else 1
        best = maxOf(best, run)
        prev = day
    }
    var day = if (today in active) today else today.minusDays(1)
    var current = 0
    while (day in active) {
        current++
        day = day.minusDays(1)
    }
    val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val week = active.count { !it.isBefore(weekStart) && !it.isAfter(today) }
    return GoalStats(current, best, active.size, week)
}

class StudyViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = StudyRepository(AppDatabase.get(app).dao())
    private val resolver = app.contentResolver

    private val _messages = Channel<UiMessage>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    /** null while the database hasn't emitted yet (avoids flashing the empty state). */
    val goals: StateFlow<List<GoalUi>?> = combine(repo.goals, repo.logs) { goals, logs ->
        // shortcut: "today" is read when the data changes; if the app stays open past midnight, stats refresh on the next edit.
        val today = LocalDate.now()
        val byGoal = logs.groupBy { it.goalId }
        goals.map { goal ->
            val map = byGoal[goal.id].orEmpty().associateBy { LocalDate.ofEpochDay(it.epochDay) }
            GoalUi(goal, map, computeStats(map.filterValues { it.intensity > 0 }.keys, today))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        // Keep the home-screen widget in sync with every change made in the app (drop the initial load).
        viewModelScope.launch {
            combine(repo.goals, repo.logs) { _, _ -> }.drop(1).collect { StudyGridWidget().updateAll(app) }
        }
    }

    fun bumpToday(goalId: Long) = viewModelScope.launch {
        val today = LocalDate.now()
        val before = repo.log(goalId, today)
        repo.bump(goalId, today)
        val level = minOf((before?.intensity ?: 0) + 1, MAX_LEVEL)
        _messages.send(UiMessage("Hoy: nivel $level") { repo.restoreLog(goalId, today, before) })
    }

    fun saveLog(goalId: Long, date: LocalDate, intensity: Int, note: String) = viewModelScope.launch {
        val before = repo.log(goalId, date)
        repo.saveLog(goalId, date, intensity, note)
        _messages.send(UiMessage("Día guardado") { repo.restoreLog(goalId, date, before) })
    }

    fun saveGoal(goal: GoalEntity) = viewModelScope.launch { repo.saveGoal(goal) }

    fun reorder(ids: List<Long>) = viewModelScope.launch { repo.reorder(ids) }

    fun deleteGoal(goal: GoalEntity) = viewModelScope.launch {
        val logs = repo.logsOf(goal.id)
        repo.deleteGoal(goal)
        _messages.send(UiMessage("Meta \"${goal.title}\" eliminada") { repo.restoreGoal(goal, logs) })
    }

    fun undo(message: UiMessage) = viewModelScope.launch { message.undo?.invoke() }

    fun export(uri: Uri) = viewModelScope.launch(Dispatchers.IO) {
        runCatching {
            val json = repo.exportJson()
            resolver.openOutputStream(uri, "wt")!!.use { it.write(json.toByteArray()) }
        }.onSuccess { _messages.send(UiMessage("Respaldo exportado")) }
            .onFailure { _messages.send(UiMessage("No se pudo exportar: ${it.message}")) }
    }

    fun import(uri: Uri) = viewModelScope.launch(Dispatchers.IO) {
        runCatching {
            val text = resolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() }
            repo.importJson(text)
        }.onSuccess { _messages.send(UiMessage("Respaldo importado")) }
            .onFailure { _messages.send(UiMessage("Archivo inválido, no se cambió nada: ${it.message}")) }
    }
}
