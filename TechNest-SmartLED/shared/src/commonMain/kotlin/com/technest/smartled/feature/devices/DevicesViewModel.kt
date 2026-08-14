package com.technest.smartled.feature.devices

import com.technest.smartled.core.model.ConnectionState
import com.technest.smartled.core.model.Device
import com.technest.smartled.data.repository.ConnectionResult
import com.technest.smartled.data.repository.DeviceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DevicesUiState(
    val discoveredDevices: List<Device> = emptyList(),
    val connectedDevice: Device? = null,
    val isScanning: Boolean = false,
    val isConnecting: Boolean = false,
    val errorMessage: String? = null,
)

class DevicesViewModel(
    private val repository: DeviceRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val _uiState = MutableStateFlow(DevicesUiState())
    val uiState: StateFlow<DevicesUiState> = _uiState.asStateFlow()

    init {
        scope.launch {
            repository.observeConnectedDevice().collect { device ->
                _uiState.update { it.copy(connectedDevice = device) }
            }
        }
    }

    fun startScan() {
        if (_uiState.value.isScanning) return
        _uiState.update { it.copy(isScanning = true, errorMessage = null) }
        scope.launch {
            try {
                val devices = repository.scan()
                _uiState.update {
                    it.copy(
                        discoveredDevices = devices,
                        isScanning = false,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        errorMessage = e.message ?: "Scan failed",
                    )
                }
            }
        }
    }

    fun connect(deviceId: String) {
        if (_uiState.value.isConnecting) return
        _uiState.update { it.copy(isConnecting = true, errorMessage = null) }
        scope.launch {
            try {
                val result = repository.connect(deviceId)
                _uiState.update { state ->
                    when (result) {
                        is ConnectionResult.Success -> state.copy(
                            isConnecting = false,
                            connectedDevice = result.device,
                        )
                        is ConnectionResult.Failure -> state.copy(
                            isConnecting = false,
                            errorMessage = result.reason,
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isConnecting = false,
                        errorMessage = e.message ?: "Connection failed",
                    )
                }
            }
        }
    }

    fun disconnect() {
        scope.launch {
            try {
                repository.disconnect()
                _uiState.update { it.copy(connectedDevice = null) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = e.message ?: "Disconnect failed")
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
