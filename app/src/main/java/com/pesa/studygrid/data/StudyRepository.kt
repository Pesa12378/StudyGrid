package com.pesa.studygrid.data

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

class StudyRepository(private val dao: StudyDao) {
    val goals = dao.goals()
    val logs = dao.logs()

    suspend fun saveGoal(goal: GoalEntity) = dao.upsertGoal(goal)
    suspend fun deleteGoal(goal: GoalEntity) = dao.deleteGoal(goal)
    suspend fun bump(goalId: Long, date: LocalDate) = dao.bump(goalId, date.toEpochDay())
    suspend fun log(goalId: Long, date: LocalDate) = dao.log(goalId, date.toEpochDay())
    suspend fun logsOf(goalId: Long) = dao.logsOf(goalId)
    suspend fun restoreGoal(goal: GoalEntity, logs: List<DailyLogEntity>) = dao.restoreGoal(goal, logs)

    /** Undo of a day change: put back exactly what was there ([previous] null = the day was empty). */
    suspend fun restoreLog(goalId: Long, date: LocalDate, previous: DailyLogEntity?) {
        if (previous == null) dao.deleteLog(goalId, date.toEpochDay()) else dao.upsertLog(previous)
    }

    suspend fun saveLog(goalId: Long, date: LocalDate, intensity: Int, note: String) {
        val day = date.toEpochDay()
        // An empty day (level 0, no note) is stored as "no row".
        if (intensity == 0 && note.isBlank()) dao.deleteLog(goalId, day)
        else dao.upsertLog(DailyLogEntity(goalId, day, intensity.coerceIn(0, MAX_LEVEL), note.trim()))
    }

    suspend fun exportJson(): String = BackupJson.encode(dao.allGoals(), dao.allLogs())

    /** Parses and validates first; only then replaces everything in one transaction. */
    suspend fun importJson(text: String) {
        val (goals, logs) = BackupJson.decode(text)
        dao.replaceAll(goals, logs)
    }
}

object BackupJson {
    private const val VERSION = 1

    fun encode(goals: List<GoalEntity>, logs: List<DailyLogEntity>): String = JSONObject()
        .put("app", "StudyGrid")
        .put("version", VERSION)
        .put("goals", JSONArray(goals.map {
            JSONObject().put("id", it.id).put("title", it.title)
                .put("description", it.description).put("color", it.color).put("weeklyTarget", it.weeklyTarget)
        }))
        .put("logs", JSONArray(logs.map {
            JSONObject().put("goalId", it.goalId).put("date", LocalDate.ofEpochDay(it.epochDay).toString())
                .put("intensity", it.intensity).put("note", it.note)
        }))
        .toString(2)

    /** @throws IllegalArgumentException or org.json.JSONException on an invalid backup. */
    fun decode(text: String): Pair<List<GoalEntity>, List<DailyLogEntity>> {
        val root = JSONObject(text)
        require(root.optInt("version") == VERSION) { "Versión de respaldo no soportada" }

        val goalsJson = root.getJSONArray("goals")
        val goals = (0 until goalsJson.length()).map { i ->
            val o = goalsJson.getJSONObject(i)
            GoalEntity(
                id = o.getLong("id"),
                title = o.getString("title").also { require(it.isNotBlank()) { "Meta sin título" } },
                description = o.optString("description"),
                color = o.getInt("color"),
                // Optional: backups made before the weekly goal existed don't have it.
                weeklyTarget = o.optInt("weeklyTarget", 0).also { require(it in 0..7) { "Meta semanal inválida" } },
            )
        }
        val ids = goals.map { it.id }.toSet()
        require(ids.size == goals.size && ids.all { it > 0 }) { "IDs de metas inválidos" }

        val logsJson = root.getJSONArray("logs")
        val logs = (0 until logsJson.length()).map { i ->
            val o = logsJson.getJSONObject(i)
            val goalId = o.getLong("goalId")
            require(goalId in ids) { "Registro de una meta inexistente" }
            val intensity = o.getInt("intensity")
            require(intensity in 0..MAX_LEVEL) { "Intensidad fuera de rango" }
            DailyLogEntity(goalId, LocalDate.parse(o.getString("date")).toEpochDay(), intensity, o.optString("note"))
        }
        require(logs.map { it.goalId to it.epochDay }.toSet().size == logs.size) { "Días duplicados" }
        return goals to logs
    }
}
