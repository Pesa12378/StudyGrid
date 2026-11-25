package com.pesa.studygrid.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
abstract class StudyDao {
    @Query("SELECT * FROM goals ORDER BY id")
    abstract fun goals(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM daily_logs")
    abstract fun logs(): Flow<List<DailyLogEntity>>

    @Query("SELECT * FROM goals ORDER BY id")
    abstract suspend fun allGoals(): List<GoalEntity>

    @Query("SELECT * FROM daily_logs ORDER BY goalId, epochDay")
    abstract suspend fun allLogs(): List<DailyLogEntity>

    @Query("SELECT * FROM daily_logs WHERE goalId = :goalId AND epochDay = :epochDay")
    abstract suspend fun log(goalId: Long, epochDay: Long): DailyLogEntity?

    @Query("SELECT * FROM daily_logs WHERE goalId = :goalId")
    abstract suspend fun logsOf(goalId: Long): List<DailyLogEntity>

    // Upsert, not REPLACE: REPLACE deletes the row first and would cascade-delete the goal's logs.
    @Upsert
    abstract suspend fun upsertGoal(goal: GoalEntity)

    @Delete
    abstract suspend fun deleteGoal(goal: GoalEntity)

    @Upsert
    abstract suspend fun upsertLog(log: DailyLogEntity)

    @Query("DELETE FROM daily_logs WHERE goalId = :goalId AND epochDay = :epochDay")
    abstract suspend fun deleteLog(goalId: Long, epochDay: Long)

    @Query("DELETE FROM daily_logs")
    protected abstract suspend fun deleteAllLogs()

    @Query("DELETE FROM goals")
    protected abstract suspend fun deleteAllGoals()

    @Insert
    protected abstract suspend fun insertGoals(goals: List<GoalEntity>)

    @Insert
    protected abstract suspend fun insertLogs(logs: List<DailyLogEntity>)

    /** +1 on a day, capped at [MAX_LEVEL]. */
    @Transaction
    open suspend fun bump(goalId: Long, epochDay: Long) {
        val current = log(goalId, epochDay)
        upsertLog(
            current?.copy(intensity = minOf(current.intensity + 1, MAX_LEVEL))
                ?: DailyLogEntity(goalId, epochDay, 1),
        )
    }

    @Upsert
    protected abstract suspend fun upsertLogs(logs: List<DailyLogEntity>)

    /** Undo of a goal deletion: puts the goal back with its whole history. */
    @Transaction
    open suspend fun restoreGoal(goal: GoalEntity, logs: List<DailyLogEntity>) {
        upsertGoal(goal)
        upsertLogs(logs)
    }

    /** All-or-nothing: if any insert fails, the previous data stays untouched. */
    @Transaction
    open suspend fun replaceAll(goals: List<GoalEntity>, logs: List<DailyLogEntity>) {
        deleteAllLogs()
        deleteAllGoals()
        insertGoals(goals)
        insertLogs(logs)
    }
}
