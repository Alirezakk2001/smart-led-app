package com.technest.smartled.core.model

data class DeviceCapabilities(
    val maxLines: Int = 1,
    val maxLedsPerLine: Int = 512,
    val supportedEffects: List<EffectId> = EffectId.entries,
    val supportsScheduler: Boolean = false,
    val supportsMusicReactive: Boolean = false,
    val protocolVersion: Int = 1,
    val firmwareVersion: String = "0.0.0",
    val hardwareVersion: String = "0.0.0",
)
