package com.technest.smartled.feature.setup

import com.technest.smartled.core.model.Device
import com.technest.smartled.core.model.DeviceCapabilities
import com.technest.smartled.core.model.DeviceConfiguration
import com.technest.smartled.data.repository.CommandResult
import com.technest.smartled.data.repository.DeviceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SetupUiState(
    val device: Device? = null,
    val isConnected: Boolean = false,
    val lineCount: Int = 1,
    val ledsPerLine: List<Int> = listOf(60),
    val capabilities: DeviceCapabilities = DeviceCapabilities(),
    val isConfiguring: Boolean = false,
    val isComplete: Boolean = false,
    val errorMessage: String? = null,
)

class SetupViewModel(
    private val repository: DeviceRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    init {
        scope.launch {
            repository.observeConnectedDevice().collect { device ->
                _uiState.update { state ->
                    if (device != null) {
                        state.copy(
                            device = device,
                            isConnected = true,
                            capabilities = device.capabilities,
                            lineCount = device.configuration.lineCount,
                            ledsPerLine = device.configuration.ledsPerLine,
                        )
                    } else {
                        state.copy(
                            device = null,
                            isConnected = false,
                        )
                    }
                }
            }
        }
    }

    fun setLineCount(count: Int) {
        val maxLines = _uiState.value.capabilities.maxLines
        val clamped = count.coerceIn(1, maxLines)
        _uiState.update { state ->
            val currentLeds = state.ledsPerLine
            val newLeds = if (clamped > currentLeds.size) {
                currentLeds + List(clamped - currentLeds.size) { 60 }
            } else if (clamped < currentLeds.size) {
                currentLeds.take(clamped)
            } else {
                currentLeds
            }
            state.copy(lineCount = clamped, ledsPerLine = newLeds)
        }
    }

    fun setLedsForLine(lineIndex: Int, count: Int) {
        val maxLeds = _uiState.value.capabilities.maxLedsPerLine
        val clamped = count.coerceIn(1, maxLeds)
        _uiState.update { state ->
            val newLeds = state.ledsPerLine.toMutableList().also {
                if (lineIndex < it.size) it[lineIndex] = clamped
            }
            state.copy(ledsPerLine = newLeds)
        }
    }

    fun applyConfiguration() {
        val state = _uiState.value
        if (!state.isConnected) {
            _uiState.update { it.copy(errorMessage = "Not connected to a device") }
            return
        }

        val config = try {
            DeviceConfiguration(
                lineCount = state.lineCount,
                ledsPerLine = state.ledsPerLine,
            )
        } catch (e: IllegalArgumentException) {
            _uiState.update { it.copy(errorMessage = e.message ?: "Invalid configuration") }
            return
        }

        _uiState.update {
            it.copy(isConfiguring = true, errorMessage = null)
        }

        scope.launch {
            val result = repository.applyConfiguration(config)
            if (result is CommandResult.Failure) {
                _uiState.update {
                    it.copy(errorMessage = result.reason, isConfiguring = false)
                }
            } else {
                _uiState.update {
                    it.copy(isConfiguring = false, isComplete = true)
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun reset() {
        _uiState.update { SetupUiState() }
    }
}
