package com.pesa.studygrid

import com.pesa.studygrid.data.BackupJson
import com.pesa.studygrid.data.DailyLogEntity
import com.pesa.studygrid.data.GoalEntity
import com.pesa.studygrid.ui.GoalStats
import com.pesa.studygrid.ui.computeStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.LocalDate

class StatsAndBackupTest {
    // Wednesday; its week starts Monday 2026-11-23.
    private val today = LocalDate.of(2026, 11, 25)
    private fun days(vararg back: Long) = back.map { today.minusDays(it) }.toSet()

    @Test fun empty() = assertEquals(GoalStats(0, 0, 0, 0), computeStats(emptySet(), today))

    @Test fun streakIncludingToday() = assertEquals(GoalStats(3, 3, 3, 3), computeStats(days(0, 1, 2), today))

    @Test fun streakEndingYesterdayStillCounts() = assertEquals(GoalStats(2, 2, 2, 2), computeStats(days(1, 2), today))

    @Test fun gapOfTwoDaysBreaksCurrent() = assertEquals(GoalStats(0, 2, 2, 1), computeStats(days(2, 3), today))

    @Test fun bestIsHistorical() =
        assertEquals(GoalStats(1, 4, 5, 1), computeStats(days(0, 10, 11, 12, 13), today))

    @Test fun weekCountsOnlyMondayToToday() {
        // Sunday 22 belongs to last week; Monday 23 and Wednesday 25 to this one.
        assertEquals(2, computeStats(days(0, 2, 3), today).week)
    }

    @Test fun streakAcrossMonthAndYear() {
        val newYear = LocalDate.of(2027, 1, 1)
        val active = setOf(LocalDate.of(2026, 12, 30), LocalDate.of(2026, 12, 31), newYear)
        assertEquals(GoalStats(3, 3, 3, 3), computeStats(active, newYear))
    }

    @Test fun backupRoundTrip() {
        val goals = listOf(GoalEntity(1, "Roadmap Data Engineer", "SQL + Kimball", 0xFF2BD9FE.toInt(), 5), GoalEntity(7, "Inglés", "", 0xFF7AE582.toInt()))
        val logs = listOf(DailyLogEntity(1, today.toEpochDay(), 3, "Capítulo 3 \"Kimball\""), DailyLogEntity(7, today.minusDays(1).toEpochDay(), 0, "solo nota"))
        assertEquals(goals to logs, BackupJson.decode(BackupJson.encode(goals, logs)))
    }

    @Test fun oldBackupWithoutWeeklyTargetStillImports() {
        val json = """{"version":1,"goals":[{"id":1,"title":"A","description":"","color":0}],"logs":[]}"""
        assertEquals(0, BackupJson.decode(json).first.single().weeklyTarget)
    }

    @Test fun rejectsBadWeeklyTarget() {
        val json = BackupJson.encode(listOf(GoalEntity(1, "A", "", 0, 9)), emptyList())
        assertThrows(IllegalArgumentException::class.java) { BackupJson.decode(json) }
    }

    @Test fun rejectsLogOfMissingGoal() {
        val json = BackupJson.encode(listOf(GoalEntity(1, "A", "", 0)), listOf(DailyLogEntity(2, 0, 1)))
        assertThrows(IllegalArgumentException::class.java) { BackupJson.decode(json) }
    }

    @Test fun rejectsOutOfRangeIntensity() {
        val json = BackupJson.encode(listOf(GoalEntity(1, "A", "", 0)), listOf(DailyLogEntity(1, 0, 9)))
        assertThrows(IllegalArgumentException::class.java) { BackupJson.decode(json) }
    }

    @Test fun rejectsGarbage() {
        assertThrows(Exception::class.java) { BackupJson.decode("{\"hola\": 1}") }
    }
}
