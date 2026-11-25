package com.pesa.studygrid.reminder

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class ReminderTest {
    private val zone = ZoneId.of("UTC")
    private fun at(y: Int, m: Int, d: Int, h: Int, min: Int = 0) = ZonedDateTime.of(LocalDateTime.of(y, m, d, h, min), zone)

    @Test fun beforeTimeIsToday() =
        assertEquals(at(2026, 5, 10, 20), Reminder.nextTrigger(at(2026, 5, 10, 8), 1200))

    @Test fun afterTimeIsTomorrow() =
        assertEquals(at(2026, 5, 11, 20), Reminder.nextTrigger(at(2026, 5, 10, 21), 1200))

    @Test fun exactlyAtTimeIsTomorrow() =
        assertEquals(at(2026, 5, 11, 20), Reminder.nextTrigger(at(2026, 5, 10, 20), 1200))

    @Test fun acrossMonthEnd() =
        assertEquals(at(2026, 2, 1, 7, 30), Reminder.nextTrigger(at(2026, 1, 31, 23), 450))
}
