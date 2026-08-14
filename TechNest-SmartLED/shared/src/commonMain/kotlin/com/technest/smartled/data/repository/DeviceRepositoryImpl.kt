package com.technest.smartled.data.repository

import com.technest.smartled.core.model.*
import com.technest.smartled.data.transport.ScannedDevice
import com.technest.smartled.data.transport.Transport
import com.technest.smartled.data.transport.TransportScanner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DeviceRepositoryImpl(
    private val transport: Transport,
    private val scanner: TransportScanner,
) : DeviceRepository {

    private val _connectedDevice = MutableStateFlow<Device?>(null)
    override fun observeConnectedDevice(): StateFlow<Device?> = _connectedDevice.asStateFlow()

    private var cachedDevices: List<Device> = emptyList()

    override suspend fun scan(): List<Device> {
        val scanned = mutableListOf<ScannedDevice>()
        scanner.startScan().collect { device ->
            scanned.add(device)
        }
        cachedDevices = scanned.map { scannedDevice ->
            Device(
                id = scannedDevice.address,
                name = scannedDevice.name,
                address = scannedDevice.address,
            )
        }
        return cachedDevices
    }

    override suspend fun connect(deviceId: String): ConnectionResult {
        val device = cachedDevices.find { it.id == deviceId }
            ?: return ConnectionResult.Failure("Device not found: $deviceId")

        val result = transport.connect(device.address)
        return if (result.isSuccess) {
            val connected = device.copy(connectionState = ConnectionState.Connected)
            _connectedDevice.value = connected
            ConnectionResult.Success(connected)
        } else {
            ConnectionResult.Failure(
                result.exceptionOrNull()?.message ?: "Connection failed"
            )
        }
    }

    override suspend fun disconnect() {
        transport.disconnect()
        _connectedDevice.value = null
    }

    override suspend fun readCapabilities(): DeviceCapabilities? {
        if (!transport.isConnected()) return null
        return DeviceCapabilities(
            maxLines = 4,
            maxLedsPerLine = 512,
            supportedEffects = EffectId.entries,
            protocolVersion = 1,
            firmwareVersion = "1.0.0",
        )
    }

    override suspend fun readConfiguration(): DeviceConfiguration? {
        if (!transport.isConnected()) return null
        return DeviceConfiguration()
    }

    override suspend fun setPower(on: Boolean): CommandResult {
        if (!transport.isConnected()) return CommandResult.Failure("Not connected")
        return CommandResult.Success
    }

    override suspend fun setBrightness(brightness: Brightness): CommandResult {
        if (!transport.isConnected()) return CommandResult.Failure("Not connected")
        return CommandResult.Success
    }

    override suspend fun setLineEnabled(lineIndex: Int, enabled: Boolean): CommandResult {
        if (!transport.isConnected()) return CommandResult.Failure("Not connected")
        return CommandResult.Success
    }

    override suspend fun setLineColor(lineIndex: Int, color: RgbColor): CommandResult {
        if (!transport.isConnected()) return CommandResult.Failure("Not connected")
        return CommandResult.Success
    }

    override suspend fun setLineEffect(lineIndex: Int, effectId: EffectId): CommandResult {
        if (!transport.isConnected()) return CommandResult.Failure("Not connected")
        return CommandResult.Success
    }

    override suspend fun setLineSpeed(lineIndex: Int, speed: Int): CommandResult {
        if (!transport.isConnected()) return CommandResult.Failure("Not connected")
        return CommandResult.Success
    }

    override suspend fun setLineBrightness(lineIndex: Int, brightness: Brightness): CommandResult {
        if (!transport.isConnected()) return CommandResult.Failure("Not connected")
        return CommandResult.Success
    }

    override suspend fun applyConfiguration(configuration: DeviceConfiguration): CommandResult {
        if (!transport.isConnected()) return CommandResult.Failure("Not connected")
        return CommandResult.Success
    }
}
