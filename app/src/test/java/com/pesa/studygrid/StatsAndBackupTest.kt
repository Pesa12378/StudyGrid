package com.pesa.studygrid

import com.pesa.studygrid.data.BackupJson
import com.pesa.studygrid.data.DailyLogEntity
import com.pesa.studygrid.data.GoalEntity
import com.pesa.studygrid.ui.GoalStats
import com.pesa.studygrid.ui.MonthStat
import com.pesa.studygrid.ui.computeStats
import com.pesa.studygrid.ui.monthlyStats
import com.pesa.studygrid.ui.newlyReachedBadge
import java.time.YearMonth
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

    private fun logs(vararg e: Pair<LocalDate, Int>) =
        e.associate { (d, i) -> d to DailyLogEntity(1, d.toEpochDay(), i, "") }

    @Test fun monthlyEmptyIsSixZeroMonthsOldestFirst() {
        val r = monthlyStats(emptyMap(), today)
        assertEquals(6, r.size)
        assertEquals(YearMonth.of(2026, 6), r.first().month)
        assertEquals(YearMonth.of(2026, 11), r.last().month)
        assertEquals(MonthStat(YearMonth.of(2026, 8), 0, 0f), r[2])
    }

    @Test fun monthlyIgnoresIntensityZeroAndAverages() {
        val r = monthlyStats(logs(LocalDate.of(2026, 11, 1) to 1, LocalDate.of(2026, 11, 2) to 4, LocalDate.of(2026, 11, 3) to 0), today)
        assertEquals(MonthStat(YearMonth.of(2026, 11), 2, 2.5f), r.last())
    }

    @Test fun monthlyBoundariesAndOutOfRange() {
        val r = monthlyStats(
            logs(
                LocalDate.of(2026, 10, 31) to 2, LocalDate.of(2026, 11, 1) to 3,
                LocalDate.of(2026, 5, 31) to 4, // before the 6-month window
            ),
            today,
        )
        assertEquals(1, r[4].activeDays)
        assertEquals(2f, r[4].avgLevel)
        assertEquals(1, r[5].activeDays)
        assertEquals(3f, r[5].avgLevel)
        assertEquals(0, r.sumOf { it.activeDays } - 2)
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

    @Test fun oldBackupWithoutPositionKeepsArrayOrder() {
        val json = """{"version":1,"goals":[{"id":5,"title":"A","color":0},{"id":2,"title":"B","color":0},{"id":9,"title":"C","color":0}],"logs":[]}"""
        assertEquals(listOf(0, 1, 2), BackupJson.decode(json).first.map { it.position })
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

    private fun cur(n: Int) = GoalStats(n, n, n, 0)

    @Test fun badgeAtSeven() = assertEquals(7, newlyReachedBadge(cur(6), cur(7)))
    @Test fun noBadgeAfterSeven() = assertEquals(null, newlyReachedBadge(cur(7), cur(8)))
    @Test fun badgeAtThirty() = assertEquals(30, newlyReachedBadge(cur(29), cur(30)))
    @Test fun noBadgeAtZero() = assertEquals(null, newlyReachedBadge(cur(0), cur(0)))
    // A broken streak rebuilt to 7 re-announces the badge (documented, harmless).
    @Test fun badgeAgainAfterReset() = assertEquals(7, newlyReachedBadge(GoalStats(0, 9, 9, 0), GoalStats(7, 9, 16, 0)))

    @Test fun rejectsGarbage() {
        assertThrows(Exception::class.java) { BackupJson.decode("{\"hola\": 1}") }
    }
}
