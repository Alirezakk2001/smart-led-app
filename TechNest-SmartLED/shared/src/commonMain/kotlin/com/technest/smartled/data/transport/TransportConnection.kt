package com.technest.smartled.data.transport

/** Platform-provided transport pair used by repositories. */
data class TransportConnection(
    val transport: Transport,
    val scanner: TransportScanner,
)

expect fun createDefaultTransportConnection(): TransportConnection
