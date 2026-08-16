package com.technest.smartled.data.transport

actual fun createDefaultTransportConnection(): TransportConnection {
    val transport = UnsupportedTransport("iOS")
    return TransportConnection(transport = transport, scanner = transport)
}
