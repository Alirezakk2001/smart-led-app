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
        val result = mutableListOf<ScannedDevice>()
        scanner.startScan().collect { device ->
            result.add(device)
        }
        cachedDevices = result.map { scannedDevice ->
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
            val config = DeviceConfiguration()
            val connected = device.copy(
                connectionState = ConnectionState.Connected,
                configuration = config,
                state = DeviceState(
                    lines = config.ledsPerLine.mapIndexed { index, _ ->
                        LedLineState(index = index)
                    }
                ),
            )
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
        val caps = DeviceCapabilities(
            maxLines = 4,
            maxLedsPerLine = 512,
            supportedEffects = EffectId.entries,
            protocolVersion = 1,
            firmwareVersion = "1.0.0",
        )
        _connectedDevice.value = _connectedDevice.value?.copy(capabilities = caps)
        return caps
    }

    override suspend fun readConfiguration(): DeviceConfiguration? {
        if (!transport.isConnected()) return null
        val config = DeviceConfiguration()
        _connectedDevice.value = _connectedDevice.value?.copy(configuration = config)
        return config
    }

    override suspend fun setPower(on: Boolean): CommandResult {
        if (!transport.isConnected()) return CommandResult.Failure("Not connected")
        updateDeviceState { it.copy(power = on) }
        return CommandResult.Success
    }

    override suspend fun setBrightness(brightness: Brightness): CommandResult {
        if (!transport.isConnected()) return CommandResult.Failure("Not connected")
        updateDeviceState { it.copy(brightness = brightness) }
        return CommandResult.Success
    }

    override suspend fun setLineEnabled(lineIndex: Int, enabled: Boolean): CommandResult {
        if (!transport.isConnected()) return CommandResult.Failure("Not connected")
        updateLines { lines ->
            lines.map { line ->
                if (line.index == lineIndex) line.copy(enabled = enabled) else line
            }
        }
        return CommandResult.Success
    }

    override suspend fun setLineColor(lineIndex: Int, color: RgbColor): CommandResult {
        if (!transport.isConnected()) return CommandResult.Failure("Not connected")
        updateLines { lines ->
            lines.map { line ->
                if (line.index == lineIndex) line.copy(color = color) else line
            }
        }
        return CommandResult.Success
    }

    override suspend fun setLineEffect(lineIndex: Int, effectId: EffectId): CommandResult {
        if (!transport.isConnected()) return CommandResult.Failure("Not connected")
        updateLines { lines ->
            lines.map { line ->
                if (line.index == lineIndex) line.copy(effectId = effectId) else line
            }
        }
        return CommandResult.Success
    }

    override suspend fun setLineSpeed(lineIndex: Int, speed: Int): CommandResult {
        if (!transport.isConnected()) return CommandResult.Failure("Not connected")
        updateLines { lines ->
            lines.map { line ->
                if (line.index == lineIndex) line.copy(speed = speed) else line
            }
        }
        return CommandResult.Success
    }

    override suspend fun setLineBrightness(lineIndex: Int, brightness: Brightness): CommandResult {
        if (!transport.isConnected()) return CommandResult.Failure("Not connected")
        updateLines { lines ->
            lines.map { line ->
                if (line.index == lineIndex) line.copy(brightness = brightness) else line
            }
        }
        return CommandResult.Success
    }

    override suspend fun applyConfiguration(configuration: DeviceConfiguration): CommandResult {
        if (!transport.isConnected()) return CommandResult.Failure("Not connected")
        _connectedDevice.value = _connectedDevice.value?.copy(configuration = configuration)
        updateDeviceState { state ->
            state.copy(
                lines = configuration.ledsPerLine.mapIndexed { index, _ ->
                    state.lines.getOrElse(index) { LedLineState(index = index) }
                }
            )
        }
        return CommandResult.Success
    }

    private fun updateDeviceState(transform: (DeviceState) -> DeviceState) {
        _connectedDevice.value = _connectedDevice.value?.let { device ->
            device.copy(state = transform(device.state))
        }
    }

    private fun updateLines(transform: (List<LedLineState>) -> List<LedLineState>) {
        updateDeviceState { state ->
            state.copy(lines = transform(state.lines))
        }
    }
}
