package com.technest.smartled.data.transport

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flow

class UnsupportedTransport(
    private val platformName: String,
) : Transport, TransportScanner {

    private val _events = MutableSharedFlow<TransportEvent>(extraBufferCapacity = 8)
    override val events: Flow<TransportEvent> = _events.asSharedFlow()

    override suspend fun connect(address: String): Result<Unit> {
        return Result.failure(UnsupportedOperationException(message()))
    }

    override suspend fun disconnect() = Unit

    override suspend fun send(data: ByteArray): Result<Unit> {
        return Result.failure(UnsupportedOperationException(message()))
    }

    override fun isConnected(): Boolean = false

    override suspend fun startScan(): Flow<ScannedDevice> = flow {
        throw UnsupportedOperationException(message())
    }

    override suspend fun stopScan() = Unit

    private fun message(): String = "Real BLE transport is not implemented for $platformName yet"
}
