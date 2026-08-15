package com.technest.smartled.ui.theme

import androidx.compose.ui.graphics.Color
import com.technest.smartled.core.model.EffectId
import com.technest.smartled.core.model.LedLineState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

data class DynamicAccent(
    val accent: Color = LedColors.AccentCyan,
    val accentSoft: Color = LedColors.AccentCyan.copy(alpha = 0.15f),
    val accentVivid: Color = LedColors.AccentCyan,
    val glow: Color = LedColors.AccentCyan.copy(alpha = 0.08f),
    val ambient: Color = LedColors.AccentCyan.copy(alpha = 0.03f),
    val sourceColor: Color = LedColors.AccentCyan,
    val isActive: Boolean = false,
)

object ColorEngine {

    fun compute(
        lines: List<LedLineState>,
        power: Boolean,
        elapsedSeconds: Float = 0f,
    ): DynamicAccent {
        val activeLines = lines.filter { it.enabled && power }
        if (activeLines.isEmpty()) {
            return DynamicAccent(isActive = false)
        }

        val representativeColors = activeLines.map { line ->
            effectAwareColor(line, elapsedSeconds)
        }

        val blended = blendColors(representativeColors)

        val avgBrightness = activeLines.map { it.brightness.value }.average().toFloat() / 255f
        val dimmed = Color(
            red = blended.red * avgBrightness,
            green = blended.green * avgBrightness,
            blue = blended.blue * avgBrightness,
            alpha = 1f,
        )

        return buildPalette(dimmed)
    }

    private fun effectAwareColor(line: LedLineState, elapsedSeconds: Float): Color {
        val baseColor = Color(
            line.color.red / 255f,
            line.color.green / 255f,
            line.color.blue / 255f,
        )

        val speedFactor = 0.5f + (line.speed / 255f) * 1.5f

        return when (line.effectId) {
            EffectId.Static -> baseColor

            EffectId.Rainbow -> {
                val hueShift = (elapsedSeconds * 0.15f * speedFactor) % 1f
                val r = sin(hueShift * 2f * PI.toFloat()).let { it * it }
                val g = sin((hueShift + 1f / 3f) * 2f * PI.toFloat()).let { it * it }
                val b = sin((hueShift + 2f / 3f) * 2f * PI.toFloat()).let { it * it }
                Color(r, g, b, 1f)
            }

            EffectId.Breathing -> {
                val pulse = (sin(elapsedSeconds * 2f * speedFactor) + 1f) / 2f
                val intensity = 0.5f + pulse * 0.5f
                Color(
                    red = baseColor.red * intensity,
                    green = baseColor.green * intensity,
                    blue = baseColor.blue * intensity,
                )
            }

            EffectId.Fire -> {
                val flicker = 0.85f + (sin(elapsedSeconds * 5f * speedFactor) * 0.15f)
                Color(
                    red = baseColor.red.coerceAtLeast(0.6f) * flicker,
                    green = baseColor.green.coerceAtMost(0.4f) * flicker,
                    blue = baseColor.blue.coerceAtMost(0.2f) * flicker,
                )
            }

            EffectId.Twinkle -> baseColor

            EffectId.Meteor -> {
                Color(
                    red = baseColor.red.coerceAtLeast(0.5f),
                    green = baseColor.green.coerceAtMost(0.5f),
                    blue = baseColor.blue.coerceAtMost(0.3f),
                )
            }

            EffectId.Wave -> {
                val emphasis = 0.5f + (sin(elapsedSeconds * 2f * speedFactor) + 1f) / 4f
                Color(
                    red = baseColor.red * 0.5f,
                    green = (baseColor.green + 1f) / 2f * emphasis,
                    blue = (baseColor.blue + 1f) / 2f * emphasis,
                )
            }

            EffectId.Police -> {
                val phase = (sin(elapsedSeconds * 4f * speedFactor) + 1f) / 2f
                Color(red = phase, green = 0f, blue = 1f - phase)
            }

            EffectId.Gradient -> {
                val drift = (elapsedSeconds * 0.08f * speedFactor) % 1f
                val dr = sin(drift * 2f * PI.toFloat()).let { it * 0.3f }
                val dg = cos(drift * 2f * PI.toFloat()).let { it * 0.3f }
                Color(
                    red = (baseColor.red + dr).coerceIn(0f, 1f),
                    green = (baseColor.green + dg).coerceIn(0f, 1f),
                    blue = (baseColor.blue - dr * 0.5f).coerceIn(0f, 1f),
                )
            }
        }
    }

    private fun blendColors(colors: List<Color>): Color {
        if (colors.isEmpty()) return Color.Black
        if (colors.size == 1) return colors.first()

        val linearColors = colors.map { color ->
            Triple(
                srgbToLinear(color.red),
                srgbToLinear(color.green),
                srgbToLinear(color.blue),
            )
        }

        val avg = Triple(
            linearColors.map { it.first }.average().toFloat(),
            linearColors.map { it.second }.average().toFloat(),
            linearColors.map { it.third }.average().toFloat(),
        )

        return Color(
            red = linearToSrgb(avg.first),
            green = linearToSrgb(avg.second),
            blue = linearToSrgb(avg.third),
        )
    }

    private fun buildPalette(source: Color): DynamicAccent {
        val accent = boostSaturation(source, 0.3f)

        return DynamicAccent(
            accent = accent,
            accentSoft = accent.copy(alpha = 0.15f),
            accentVivid = accent.copy(alpha = 0.9f),
            glow = accent.copy(alpha = 0.08f),
            ambient = accent.copy(alpha = 0.03f),
            sourceColor = source,
            isActive = true,
        )
    }

    private fun boostSaturation(color: Color, amount: Float): Color {
        val gray = color.red * 0.299f + color.green * 0.587f + color.blue * 0.114f
        return Color(
            red = (color.red + (color.red - gray) * amount).coerceIn(0f, 1f),
            green = (color.green + (color.green - gray) * amount).coerceIn(0f, 1f),
            blue = (color.blue + (color.blue - gray) * amount).coerceIn(0f, 1f),
        )
    }

    private fun srgbToLinear(c: Float): Float {
        return if (c <= 0.04045f) c / 12.92f
        else ((c + 0.055f) / 1.055f).pow(2.4f)
    }

    private fun linearToSrgb(c: Float): Float {
        return if (c <= 0.0031308f) c * 12.92f
        else (1.055f * c.pow(1f / 2.4f) - 0.055f)
    }

    private fun Float.pow(exp: Float): Float {
        return if (this == 0f) 0f else this.toDouble().pow(exp.toDouble()).toFloat()
    }
}
