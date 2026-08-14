package com.technest.smartled.data.repository

import com.technest.smartled.core.model.*

sealed interface ConnectionResult {
    data class Success(val device: Device) : ConnectionResult
    data class Failure(val reason: String) : ConnectionResult
}

sealed interface CommandResult {
    data object Success : CommandResult
    data class Failure(val reason: String) : CommandResult
}

interface DeviceRepository {
    fun observeConnectedDevice(): kotlinx.coroutines.flow.StateFlow<Device?>

    suspend fun scan(): List<Device>
    suspend fun connect(deviceId: String): ConnectionResult
    suspend fun disconnect()

    suspend fun readCapabilities(): DeviceCapabilities?
    suspend fun readConfiguration(): DeviceConfiguration?

    suspend fun setPower(on: Boolean): CommandResult
    suspend fun setBrightness(brightness: Brightness): CommandResult
    suspend fun setLineEnabled(lineIndex: Int, enabled: Boolean): CommandResult
    suspend fun setLineColor(lineIndex: Int, color: RgbColor): CommandResult
    suspend fun setLineEffect(lineIndex: Int, effectId: EffectId): CommandResult
    suspend fun setLineSpeed(lineIndex: Int, speed: Int): CommandResult
    suspend fun setLineBrightness(lineIndex: Int, brightness: Brightness): CommandResult

    suspend fun applyConfiguration(configuration: DeviceConfiguration): CommandResult
}
