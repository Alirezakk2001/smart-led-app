package com.technest.smartled.core.model

data class EffectParameter(
    val name: String,
    val value: Int,
    val min: Int = 0,
    val max: Int = 255,
)
