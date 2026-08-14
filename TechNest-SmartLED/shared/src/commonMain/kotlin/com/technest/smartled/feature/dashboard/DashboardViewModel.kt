package com.technest.smartled.feature.dashboard

import com.technest.smartled.core.model.*
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

data class DashboardUiState(
    val isConnected: Boolean = false,
    val deviceName: String = "",
    val deviceAddress: String = "",
    val power: Boolean = false,
    val brightness: Brightness = Brightness.Default,
    val lines: List<LedLineState> = emptyList(),
    val selectedLineIndex: Int? = null,
    val capabilities: DeviceCapabilities = DeviceCapabilities(),
    val configuration: DeviceConfiguration = DeviceConfiguration(),
    val isSendingCommand: Boolean = false,
    val errorMessage: String? = null,
) {
    val selectedLine: LedLineState?
        get() = if (selectedLineIndex == null) null
        else lines.getOrNull(selectedLineIndex)

    val lineCount: Int get() = configuration.lineCount
}

class DashboardViewModel(
    private val repository: DeviceRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        scope.launch {
            repository.observeConnectedDevice().collect { device ->
                _uiState.update { state ->
                    if (device != null) {
                        state.copy(
                            isConnected = true,
                            deviceName = device.name,
                            deviceAddress = device.address,
                            power = device.state.power,
                            brightness = device.state.brightness,
                            lines = device.state.lines,
                            capabilities = device.capabilities,
                            configuration = device.configuration,
                            selectedLineIndex = if (device.state.lines.isNotEmpty()) {
                                state.selectedLineIndex ?: device.state.lines.first().index
                            } else null,
                        )
                    } else {
                        DashboardUiState()
                    }
                }
            }
        }
    }

    fun selectLine(lineIndex: Int?) {
        _uiState.update { it.copy(selectedLineIndex = lineIndex) }
    }

    fun togglePower() {
        val newPower = !_uiState.value.power
        _uiState.update { it.copy(power = newPower, isSendingCommand = true) }
        scope.launch {
            val result = repository.setPower(newPower)
            if (result is CommandResult.Failure) {
                _uiState.update { it.copy(errorMessage = result.reason) }
            }
            _uiState.update { it.copy(isSendingCommand = false) }
        }
    }

    fun setBrightness(brightness: Brightness) {
        _uiState.update { it.copy(brightness = brightness, isSendingCommand = true) }
        scope.launch {
            val result = repository.setBrightness(brightness)
            if (result is CommandResult.Failure) {
                _uiState.update { it.copy(errorMessage = result.reason) }
            }
            _uiState.update { it.copy(isSendingCommand = false) }
        }
    }

    fun setLineColor(lineIndex: Int, color: RgbColor) {
        _uiState.update { state ->
            state.copy(
                lines = state.lines.map { line ->
                    if (line.index == lineIndex) line.copy(color = color) else line
                },
                isSendingCommand = true,
            )
        }
        scope.launch {
            val result = repository.setLineColor(lineIndex, color)
            if (result is CommandResult.Failure) {
                _uiState.update { it.copy(errorMessage = result.reason) }
            }
            _uiState.update { it.copy(isSendingCommand = false) }
        }
    }

    fun setLineEffect(lineIndex: Int, effectId: EffectId) {
        _uiState.update { state ->
            state.copy(
                lines = state.lines.map { line ->
                    if (line.index == lineIndex) line.copy(effectId = effectId) else line
                },
                isSendingCommand = true,
            )
        }
        scope.launch {
            val result = repository.setLineEffect(lineIndex, effectId)
            if (result is CommandResult.Failure) {
                _uiState.update { it.copy(errorMessage = result.reason) }
            }
            _uiState.update { it.copy(isSendingCommand = false) }
        }
    }

    fun setLineSpeed(lineIndex: Int, speed: Int) {
        _uiState.update { state ->
            state.copy(
                lines = state.lines.map { line ->
                    if (line.index == lineIndex) line.copy(speed = speed) else line
                },
                isSendingCommand = true,
            )
        }
        scope.launch {
            val result = repository.setLineSpeed(lineIndex, speed)
            if (result is CommandResult.Failure) {
                _uiState.update { it.copy(errorMessage = result.reason) }
            }
            _uiState.update { it.copy(isSendingCommand = false) }
        }
    }

    fun setLineBrightness(lineIndex: Int, brightness: Brightness) {
        _uiState.update { state ->
            state.copy(
                lines = state.lines.map { line ->
                    if (line.index == lineIndex) line.copy(brightness = brightness) else line
                },
                isSendingCommand = true,
            )
        }
        scope.launch {
            val result = repository.setLineBrightness(lineIndex, brightness)
            if (result is CommandResult.Failure) {
                _uiState.update { it.copy(errorMessage = result.reason) }
            }
            _uiState.update { it.copy(isSendingCommand = false) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
