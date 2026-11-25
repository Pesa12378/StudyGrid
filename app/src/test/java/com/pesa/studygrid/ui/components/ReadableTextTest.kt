package com.pesa.studygrid.ui.components

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class ReadableTextTest {
    private val dark = Color(0xFF06121F)

    // Goal palette: all light enough that dark text wins (white was 2.3:1 on coral, 2.6:1 on sky).
    @Test fun coralGetsDarkText() = assertEquals(dark, readableTextOn(Color(0xFFFF8A65)))
    @Test fun skyGetsDarkText() = assertEquals(dark, readableTextOn(Color(0xFF4FA3FF)))
    @Test fun aquaGetsDarkText() = assertEquals(dark, readableTextOn(Color(0xFF2BD9FE)))

    @Test fun deepTealGetsWhiteText() = assertEquals(Color.White, readableTextOn(Color(0xFF026B72)))
}
