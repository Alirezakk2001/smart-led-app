package com.technest.smartled.core.model

data class Device(
    val id: String,
    val name: String,
    val address: String,
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val capabilities: DeviceCapabilities = DeviceCapabilities(),
    val configuration: DeviceConfiguration = DeviceConfiguration(),
    val state: DeviceState = DeviceState(),
)

enum class ConnectionState {
    Disconnected,
    Scanning,
    Connecting,
    Connected,
    Disconnecting,
}
