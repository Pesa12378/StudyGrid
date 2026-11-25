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
import java.time.LocalDate

data class GoalStats(val current: Int, val best: Int, val total: Int)

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
    return GoalStats(current, best, active.size)
}

class StudyViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = StudyRepository(AppDatabase.get(app).dao())
    private val resolver = app.contentResolver

    private val _messages = Channel<String>(Channel.BUFFERED)
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

    fun bumpToday(goalId: Long) = viewModelScope.launch { repo.bump(goalId, LocalDate.now()) }

    fun saveLog(goalId: Long, date: LocalDate, intensity: Int, note: String) =
        viewModelScope.launch { repo.saveLog(goalId, date, intensity, note) }

    fun saveGoal(goal: GoalEntity) = viewModelScope.launch { repo.saveGoal(goal) }

    fun deleteGoal(goal: GoalEntity) = viewModelScope.launch { repo.deleteGoal(goal) }

    fun export(uri: Uri) = viewModelScope.launch(Dispatchers.IO) {
        runCatching {
            val json = repo.exportJson()
            resolver.openOutputStream(uri, "wt")!!.use { it.write(json.toByteArray()) }
        }.onSuccess { _messages.send("Respaldo exportado") }
            .onFailure { _messages.send("No se pudo exportar: ${it.message}") }
    }

    fun import(uri: Uri) = viewModelScope.launch(Dispatchers.IO) {
        runCatching {
            val text = resolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() }
            repo.importJson(text)
        }.onSuccess { _messages.send("Respaldo importado") }
            .onFailure { _messages.send("Archivo inválido, no se cambió nada: ${it.message}") }
    }
}
