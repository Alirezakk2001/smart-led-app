package com.technest.smartled.feature.dashboard

import com.technest.smartled.core.model.Brightness
import com.technest.smartled.core.model.EffectId
import com.technest.smartled.core.model.RgbColor
import com.technest.smartled.data.repository.DeviceRepository
import com.technest.smartled.data.repository.DeviceRepositoryImpl
import com.technest.smartled.data.transport.MockTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    @Test
    fun `initial state is disconnected`() = runTest(StandardTestDispatcher()) {
        val viewModel = createViewModel()

        val state = viewModel.uiState.value
        assertFalse(state.isConnected)
        assertFalse(state.power)
        assertTrue(state.lines.isEmpty())
        assertEquals("", state.deviceName)
        assertNull(state.selectedLineIndex)
    }

    @Test
    fun `state updates when device connects`() = runTest(StandardTestDispatcher()) {
        val (viewModel, _) = createConnectedViewModel()
        assertTrue(viewModel.uiState.value.isConnected)
        assertEquals("LED Controller 1", viewModel.uiState.value.deviceName)
        assertEquals(1, viewModel.uiState.value.lines.size)
        assertEquals(0, viewModel.uiState.value.lines[0].index)
        assertFalse(viewModel.uiState.value.power)
        assertNotNull(viewModel.uiState.value.selectedLineIndex)
    }

    @Test
    fun `togglePower updates power state`() = runTest(StandardTestDispatcher()) {
        val (viewModel, _) = createConnectedViewModel()
        assertFalse(viewModel.uiState.value.power)

        viewModel.togglePower()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.power)

        viewModel.togglePower()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.power)
    }

    @Test
    fun `setBrightness updates brightness`() = runTest(StandardTestDispatcher()) {
        val (viewModel, _) = createConnectedViewModel()
        val newBrightness = Brightness(200)

        viewModel.setBrightness(newBrightness)
        advanceUntilIdle()
        assertEquals(newBrightness, viewModel.uiState.value.brightness)
    }

    @Test
    fun `selectLine updates selected line index`() = runTest(StandardTestDispatcher()) {
        val (viewModel, _) = createConnectedViewModel()
        assertEquals(0, viewModel.uiState.value.selectedLineIndex)

        viewModel.selectLine(null)
        assertNull(viewModel.uiState.value.selectedLineIndex)

        viewModel.selectLine(0)
        assertEquals(0, viewModel.uiState.value.selectedLineIndex)
    }

    @Test
    fun `setLineColor updates line color`() = runTest(StandardTestDispatcher()) {
        val (viewModel, _) = createConnectedViewModel()
        val newColor = RgbColor(255, 0, 0)

        viewModel.setLineColor(0, newColor)
        advanceUntilIdle()
        assertEquals(newColor, viewModel.uiState.value.lines[0].color)
    }

    @Test
    fun `setLineEffect updates line effect`() = runTest(StandardTestDispatcher()) {
        val (viewModel, _) = createConnectedViewModel()

        viewModel.setLineEffect(0, EffectId.Rainbow)
        advanceUntilIdle()
        assertEquals(EffectId.Rainbow, viewModel.uiState.value.lines[0].effectId)
    }

    @Test
    fun `setLineSpeed updates line speed`() = runTest(StandardTestDispatcher()) {
        val (viewModel, _) = createConnectedViewModel()

        viewModel.setLineSpeed(0, 200)
        advanceUntilIdle()
        assertEquals(200, viewModel.uiState.value.lines[0].speed)
    }

    @Test
    fun `setLineBrightness updates line brightness`() = runTest(StandardTestDispatcher()) {
        val (viewModel, _) = createConnectedViewModel()
        val newBrightness = Brightness(50)

        viewModel.setLineBrightness(0, newBrightness)
        advanceUntilIdle()
        assertEquals(newBrightness, viewModel.uiState.value.lines[0].brightness)
    }

    @Test
    fun `clearError resets error message`() = runTest(StandardTestDispatcher()) {
        val (viewModel, _) = createConnectedViewModel()
        viewModel.clearError()
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `lineCount matches configuration`() = runTest(StandardTestDispatcher()) {
        val (viewModel, _) = createConnectedViewModel()
        assertEquals(1, viewModel.uiState.value.lineCount)
    }

    @Test
    fun `disconnection resets state`() = runTest(StandardTestDispatcher()) {
        val (viewModel, repository) = createConnectedViewModel()

        repository.disconnect()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isConnected)
        assertTrue(state.lines.isEmpty())
        assertEquals("", state.deviceName)
    }

    @Test
    fun `selectedLine returns current line state`() = runTest(StandardTestDispatcher()) {
        val (viewModel, _) = createConnectedViewModel()

        val line = viewModel.uiState.value.selectedLine
        assertNotNull(line)
        assertEquals(0, line.index)
    }

    @Test
    fun `connecting to multi-line device configures multiple lines`() = runTest(StandardTestDispatcher()) {
        val transport = MockTransport()
        val repository = DeviceRepositoryImpl(transport, transport)
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val viewModel = DashboardViewModel(repository, scope = scope)

        repository.scan()
        advanceUntilIdle()
        repository.connect("00:11:22:33:44:01")
        advanceUntilIdle()

        // Apply 3-line configuration
        val config = com.technest.smartled.core.model.DeviceConfiguration(
            lineCount = 3,
            ledsPerLine = listOf(60, 120, 30),
        )
        repository.applyConfiguration(config)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(3, state.lines.size)
        assertEquals(3, state.lineCount)
        assertEquals(0, state.lines[0].index)
        assertEquals(1, state.lines[1].index)
        assertEquals(2, state.lines[2].index)
    }

    private fun TestScope.createViewModel(): DashboardViewModel {
        val transport = MockTransport()
        val repository = DeviceRepositoryImpl(transport, transport)
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        return DashboardViewModel(repository, scope = scope)
    }

    private suspend fun TestScope.createConnectedViewModel(): Pair<DashboardViewModel, DeviceRepository> {
        val transport = MockTransport()
        val repository = DeviceRepositoryImpl(transport, transport)
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val viewModel = DashboardViewModel(repository, scope = scope)

        repository.scan()
        advanceUntilIdle()
        repository.connect("00:11:22:33:44:01")
        advanceUntilIdle()

        return Pair(viewModel, repository)
    }
}
