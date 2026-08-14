package com.technest.smartled.core.model

data class LedLineState(
    val index: Int,
    val enabled: Boolean = true,
    val color: RgbColor = RgbColor.White,
    val brightness: Brightness = Brightness.Default,
    val effectId: EffectId = EffectId.Static,
    val speed: Int = 128,
    val effectParameters: List<EffectParameter> = emptyList(),
)
