package com.technest.smartled.data.transport

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.MutableSharedFlow

class MockTransport : Transport, TransportScanner {

    private val _events = MutableSharedFlow<TransportEvent>(extraBufferCapacity = 8)
    override val events: Flow<TransportEvent> = _events.asSharedFlow()

    private var connectedAddress: String? = null

    override suspend fun connect(address: String): Result<Unit> {
        delay(300) // simulate connection delay
        connectedAddress = address
        _events.tryEmit(TransportEvent.Connected)
        return Result.success(Unit)
    }

    override suspend fun disconnect() {
        val wasConnected = connectedAddress != null
        connectedAddress = null
        if (wasConnected) {
            _events.tryEmit(TransportEvent.Disconnected)
        }
    }

    override suspend fun send(data: ByteArray): Result<Unit> {
        if (connectedAddress == null) {
            return Result.failure(IllegalStateException("Not connected"))
        }
        delay(50) // simulate send delay
        return Result.success(Unit)
    }

    override fun isConnected(): Boolean = connectedAddress != null

    override suspend fun startScan(): Flow<ScannedDevice> = flow {
        val mockDevices = listOf(
            ScannedDevice(address = "00:11:22:33:44:01", name = "LED Controller 1", rssi = -55),
            ScannedDevice(address = "00:11:22:33:44:02", name = "LED Controller 2", rssi = -62),
            ScannedDevice(address = "00:11:22:33:44:03", name = "Living Room LEDs", rssi = -48),
        )
        for (device in mockDevices) {
            delay(500) // simulate discovery interval
            emit(device)
        }
    }

    override suspend fun stopScan() {
        // no-op in mock
    }
}
