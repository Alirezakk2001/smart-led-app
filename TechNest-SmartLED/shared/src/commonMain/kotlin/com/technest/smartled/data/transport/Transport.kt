package com.technest.smartled.data.transport

import kotlinx.coroutines.flow.Flow

sealed interface TransportEvent {
    data object Connected : TransportEvent
    data object Disconnected : TransportEvent
    data class MessageReceived(val data: ByteArray) : TransportEvent
    data class Error(val reason: String) : TransportEvent
}

interface Transport {
    val events: Flow<TransportEvent>

    suspend fun connect(address: String): Result<Unit>
    suspend fun disconnect()
    suspend fun send(data: ByteArray): Result<Unit>
    fun isConnected(): Boolean
}
