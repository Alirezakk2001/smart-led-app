package com.technest.smartled.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RgbColorTest {

    @Test
    fun `valid RGB values`() {
        val color = RgbColor(100, 150, 200)
        assertEquals(100, color.red)
        assertEquals(150, color.green)
        assertEquals(200, color.blue)
    }

    @Test
    fun `black has zero values`() {
        assertEquals(RgbColor(0, 0, 0), RgbColor.Black)
    }

    @Test
    fun `white has max values`() {
        assertEquals(RgbColor(255, 255, 255), RgbColor.White)
    }

    @Test
    fun `invalid red throws`() {
        assertFailsWith<IllegalArgumentException> { RgbColor(-1, 0, 0) }
        assertFailsWith<IllegalArgumentException> { RgbColor(256, 0, 0) }
    }

    @Test
    fun `invalid green throws`() {
        assertFailsWith<IllegalArgumentException> { RgbColor(0, -1, 0) }
        assertFailsWith<IllegalArgumentException> { RgbColor(0, 256, 0) }
    }

    @Test
    fun `invalid blue throws`() {
        assertFailsWith<IllegalArgumentException> { RgbColor(0, 0, -1) }
        assertFailsWith<IllegalArgumentException> { RgbColor(0, 0, 256) }
    }
}
