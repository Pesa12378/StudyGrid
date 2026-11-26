package com.pesa.studygrid.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelRampTest {
    private val goal = Color(0xFF4FA3FF)

    @Test fun lightRampGetsStrongerAndDistinct() {
        val c = (1..4).map { levelColor(it, goal, dark = false) }
        assertTrue(c.toSet().size == 4)
        assertTrue(c[0].luminance() > c[3].luminance())
        assertTrue(c.zipWithNext().all { (x, y) -> x.luminance() > y.luminance() })
    }

    @Test fun darkRampIsDistinctAndLevel4IsGoalColor() {
        val c = (1..4).map { levelColor(it, goal, dark = true) }
        assertTrue(c.toSet().size == 4)
        assertTrue(c[3] == goal)
    }
}
