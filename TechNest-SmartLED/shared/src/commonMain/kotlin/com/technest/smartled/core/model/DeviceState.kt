package com.technest.smartled.core.model

data class DeviceState(
    val power: Boolean = false,
    val brightness: Brightness = Brightness.Default,
    val lines: List<LedLineState> = emptyList(),
)
