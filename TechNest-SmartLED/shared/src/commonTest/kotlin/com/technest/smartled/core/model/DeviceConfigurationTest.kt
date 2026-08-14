package com.technest.smartled.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DeviceConfigurationTest {

    @Test
    fun `default configuration`() {
        val config = DeviceConfiguration()
        assertEquals(1, config.lineCount)
        assertEquals(listOf(60), config.ledsPerLine)
    }

    @Test
    fun `multi line configuration`() {
        val config = DeviceConfiguration(lineCount = 3, ledsPerLine = listOf(60, 120, 30))
        assertEquals(3, config.lineCount)
        assertEquals(listOf(60, 120, 30), config.ledsPerLine)
    }

    @Test
    fun `mismatched line count throws`() {
        assertFailsWith<IllegalArgumentException> {
            DeviceConfiguration(lineCount = 3, ledsPerLine = listOf(60, 120))
        }
    }

    @Test
    fun `zero line count throws`() {
        assertFailsWith<IllegalArgumentException> {
            DeviceConfiguration(lineCount = 0)
        }
    }

    @Test
    fun `zero LED count throws`() {
        assertFailsWith<IllegalArgumentException> {
            DeviceConfiguration(lineCount = 2, ledsPerLine = listOf(60, 0))
        }
    }
}
