package com.technest.smartled.data.transport

actual fun createDefaultTransportConnection(): TransportConnection {
    val transport = UnsupportedTransport("desktop")
    return TransportConnection(transport = transport, scanner = transport)
}
