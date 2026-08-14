package com.technest.smartled.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BrightnessTest {

    @Test
    fun `valid brightness values`() {
        assertEquals(0, Brightness.Off.value)
        assertEquals(255, Brightness.Full.value)
        assertEquals(128, Brightness.Default.value)
    }

    @Test
    fun `toPercent calculation`() {
        assertEquals(0, Brightness.Off.toPercent())
        assertEquals(100, Brightness.Full.toPercent())
        assertEquals(50, Brightness(128).toPercent())
    }

    @Test
    fun `fromPercent calculation`() {
        assertEquals(0, Brightness.fromPercent(0).value)
        assertEquals(255, Brightness.fromPercent(100).value)
        assertEquals(128, Brightness.fromPercent(50).value)
    }

    @Test
    fun `invalid brightness throws`() {
        assertFailsWith<IllegalArgumentException> { Brightness(-1) }
        assertFailsWith<IllegalArgumentException> { Brightness(256) }
    }

    @Test
    fun `invalid percent throws`() {
        assertFailsWith<IllegalArgumentException> { Brightness.fromPercent(-1) }
        assertFailsWith<IllegalArgumentException> { Brightness.fromPercent(101) }
    }
}
