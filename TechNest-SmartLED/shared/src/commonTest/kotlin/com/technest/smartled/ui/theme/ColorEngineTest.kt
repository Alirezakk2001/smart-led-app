package com.technest.smartled.ui.theme

import androidx.compose.ui.graphics.Color
import com.technest.smartled.core.model.Brightness
import com.technest.smartled.core.model.EffectId
import com.technest.smartled.core.model.LedLineState
import com.technest.smartled.core.model.RgbColor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ColorEngineTest {

    @Test
    fun `compute returns inactive when no lines`() {
        val result = ColorEngine.compute(lines = emptyList(), power = true)
        assertFalse(result.isActive)
    }

    @Test
    fun `compute returns inactive when power is off`() {
        val result = ColorEngine.compute(
            lines = listOf(LedLineState(index = 0)),
            power = false,
        )
        assertFalse(result.isActive)
    }

    @Test
    fun `compute returns inactive when no enabled lines`() {
        val result = ColorEngine.compute(
            lines = listOf(LedLineState(index = 0, enabled = false)),
            power = true,
        )
        assertFalse(result.isActive)
    }

    @Test
    fun `compute returns active for enabled line with power on`() {
        val result = ColorEngine.compute(
            lines = listOf(LedLineState(
                index = 0, enabled = true,
                color = RgbColor(255, 0, 0),
                brightness = Brightness.Full,
            )),
            power = true,
        )
        assertTrue(result.isActive)
    }

    @Test
    fun `compute returns red-tinted accent for red LED`() {
        val result = ColorEngine.compute(
            lines = listOf(LedLineState(
                index = 0, enabled = true,
                color = RgbColor(255, 0, 0),
                brightness = Brightness.Full,
            )),
            power = true,
        )
        assertTrue(result.isActive)
        assertTrue(result.accent.red > result.accent.blue)
        assertTrue(result.accent.green < result.accent.red)
    }

    @Test
    fun `compute returns blue-tinted accent for blue LED`() {
        val result = ColorEngine.compute(
            lines = listOf(LedLineState(
                index = 0, enabled = true,
                color = RgbColor(0, 0, 255),
                brightness = Brightness.Full,
            )),
            power = true,
        )
        assertTrue(result.isActive)
        assertTrue(result.accent.blue > result.accent.red)
        assertTrue(result.accent.blue > result.accent.green)
    }

    @Test
    fun `compute dims accent when brightness is low`() {
        val bright = ColorEngine.compute(
            lines = listOf(LedLineState(
                index = 0, enabled = true,
                color = RgbColor(255, 0, 0),
                brightness = Brightness.Full,
            )),
            power = true,
        )
        val dim = ColorEngine.compute(
            lines = listOf(LedLineState(
                index = 0, enabled = true,
                color = RgbColor(255, 0, 0),
                brightness = Brightness(50),
            )),
            power = true,
        )
        assertTrue(dim.accent.red < bright.accent.red)
    }

    @Test
    fun `compute blends multiple lines`() {
        val result = ColorEngine.compute(
            lines = listOf(
                LedLineState(index = 0, enabled = true, color = RgbColor(255, 0, 0), brightness = Brightness.Full),
                LedLineState(index = 1, enabled = true, color = RgbColor(0, 0, 255), brightness = Brightness.Full),
            ),
            power = true,
        )
        assertTrue(result.isActive)
        assertTrue(result.accent.red > 0f)
        assertTrue(result.accent.blue > 0f)
    }

    @Test
    fun `compute only uses enabled lines`() {
        val result = ColorEngine.compute(
            lines = listOf(
                LedLineState(index = 0, enabled = false, color = RgbColor(255, 0, 0), brightness = Brightness.Full),
                LedLineState(index = 1, enabled = true, color = RgbColor(0, 255, 0), brightness = Brightness.Full),
            ),
            power = true,
        )
        assertTrue(result.isActive)
        assertTrue(result.accent.green > result.accent.red)
    }

    @Test
    fun `compute palette includes soft and glow variants`() {
        val result = ColorEngine.compute(
            lines = listOf(LedLineState(
                index = 0, enabled = true,
                color = RgbColor(255, 0, 0),
                brightness = Brightness.Full,
            )),
            power = true,
        )
        assertTrue(result.isActive)
        assertTrue(result.accentSoft.alpha < result.accent.alpha)
        assertTrue(result.glow.alpha < result.accentSoft.alpha)
        assertTrue(result.ambient.alpha <= result.glow.alpha)
    }

    @Test
    fun `compute with Fire effect emphasizes warm reds`() {
        val result = ColorEngine.compute(
            lines = listOf(LedLineState(
                index = 0, enabled = true,
                color = RgbColor(255, 255, 255),
                brightness = Brightness.Full,
                effectId = EffectId.Fire,
            )),
            power = true,
            elapsedSeconds = 0f,
        )
        assertTrue(result.isActive)
        assertTrue(result.accent.red > result.accent.blue)
    }

    @Test
    fun `DynamicAccent default values are sensible`() {
        val default = DynamicAccent()
        assertFalse(default.isActive)
        assertEquals(LedColors.AccentCyan, default.accent)
    }
}
