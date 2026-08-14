package com.technest.smartled.feature.setup

import com.technest.smartled.core.model.DeviceConfiguration
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
class SetupViewModelTest {

    @Test
    fun `initial state has defaults`() = runTest(StandardTestDispatcher()) {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value

        assertFalse(state.isConnected)
        assertNull(state.device)
        assertEquals(1, state.lineCount)
        assertEquals(listOf(60), state.ledsPerLine)
        assertFalse(state.isComplete)
    }

    @Test
    fun `state updates when device connects`() = runTest(StandardTestDispatcher()) {
        val (viewModel, _) = createConnectedViewModel()

        assertTrue(viewModel.uiState.value.isConnected)
        assertNotNull(viewModel.uiState.value.device)
        assertEquals("LED Controller 1", viewModel.uiState.value.device?.name)
        assertEquals(1, viewModel.uiState.value.lineCount)
        assertEquals(listOf(60), viewModel.uiState.value.ledsPerLine)
    }

    @Test
    fun `setLineCount adjusts ledsPerLine list size`() = runTest(StandardTestDispatcher()) {
        val (viewModel, _) = createConnectedViewModel()

        viewModel.setLineCount(3)
        val state = viewModel.uiState.value
        assertEquals(3, state.lineCount)
        assertEquals(3, state.ledsPerLine.size)
        assertEquals(60, state.ledsPerLine[0])
        assertEquals(60, state.ledsPerLine[1])
        assertEquals(60, state.ledsPerLine[2])
    }

    @Test
    fun `setLineCount clamps to minimum 1`() = runTest(StandardTestDispatcher()) {
        val (viewModel, _) = createConnectedViewModel()

        viewModel.setLineCount(0)
        assertEquals(1, viewModel.uiState.value.lineCount)
    }

    @Test
    fun `setLineCount clamps to device capabilities max`() = runTest(StandardTestDispatcher()) {
        val (viewModel, _) = createConnectedViewModel()

        // MockTransport gives maxLines = 4
        viewModel.setLineCount(10)
        assertEquals(4, viewModel.uiState.value.lineCount)
    }

    @Test
    fun `setLineCount reduces ledsPerLine when decreasing`() = runTest(StandardTestDispatcher()) {
        val (viewModel, _) = createConnectedViewModel()
        viewModel.setLineCount(3)
        assertEquals(3, viewModel.uiState.value.ledsPerLine.size)

        viewModel.setLineCount(1)
        val state = viewModel.uiState.value
        assertEquals(1, state.lineCount)
        assertEquals(1, state.ledsPerLine.size)
    }

    @Test
    fun `setLedsForLine updates specific line`() = runTest(StandardTestDispatcher()) {
        val (viewModel, _) = createConnectedViewModel()
        viewModel.setLineCount(3)

        viewModel.setLedsForLine(0, 100)
        assertEquals(100, viewModel.uiState.value.ledsPerLine[0])
        assertEquals(60, viewModel.uiState.value.ledsPerLine[1])
        assertEquals(60, viewModel.uiState.value.ledsPerLine[2])
    }

    @Test
    fun `setLedsForLine clamps to valid range`() = runTest(StandardTestDispatcher()) {
        val (viewModel, _) = createConnectedViewModel()

        viewModel.setLedsForLine(0, 0)
        assertEquals(1, viewModel.uiState.value.ledsPerLine[0])

        viewModel.setLedsForLine(0, 999)
        assertEquals(512, viewModel.uiState.value.ledsPerLine[0])
    }

    @Test
    fun `applyConfiguration without connection shows error`() = runTest(StandardTestDispatcher()) {
        val viewModel = createViewModel()

        viewModel.applyConfiguration()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isComplete)
    }

    @Test
    fun `applyConfiguration with connection succeeds`() = runTest(StandardTestDispatcher()) {
        val (viewModel, _) = createConnectedViewModel()

        viewModel.setLineCount(2)
        viewModel.setLedsForLine(1, 120)

        viewModel.applyConfiguration()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.isComplete)
    }

    @Test
    fun `applyConfiguration updates device configuration`() = runTest(StandardTestDispatcher()) {
        val (viewModel, repository) = createConnectedViewModel()

        viewModel.setLineCount(2)
        viewModel.setLedsForLine(0, 50)
        viewModel.setLedsForLine(1, 100)

        viewModel.applyConfiguration()
        advanceUntilIdle()

        val device = repository.observeConnectedDevice().value
        assertNotNull(device)
        assertEquals(2, device.configuration.lineCount)
        assertEquals(listOf(50, 100), device.configuration.ledsPerLine)
    }

    @Test
    fun `clearError resets error message`() = runTest(StandardTestDispatcher()) {
        val viewModel = createViewModel()

        viewModel.applyConfiguration()
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.errorMessage)

        viewModel.clearError()
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `reset clears state to defaults`() = runTest(StandardTestDispatcher()) {
        val (viewModel, _) = createConnectedViewModel()

        viewModel.setLineCount(3)
        viewModel.setLedsForLine(0, 100)
        assertTrue(viewModel.uiState.value.isConnected)
        assertEquals(3, viewModel.uiState.value.lineCount)

        viewModel.reset()
        val state = viewModel.uiState.value
        assertFalse(state.isConnected)
        assertNull(state.device)
        assertEquals(1, state.lineCount)
        assertEquals(listOf(60), state.ledsPerLine)
        assertFalse(state.isComplete)
    }

    @Test
    fun `disconnection resets connected state`() = runTest(StandardTestDispatcher()) {
        val (viewModel, repository) = createConnectedViewModel()

        repository.disconnect()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isConnected)
        assertNull(state.device)
    }

    @Test
    fun `can configure multiple lines`() = runTest(StandardTestDispatcher()) {
        val (viewModel, _) = createConnectedViewModel()

        viewModel.setLineCount(4)
        var state = viewModel.uiState.value
        assertEquals(4, state.lineCount)
        assertEquals(4, state.ledsPerLine.size)

        viewModel.setLedsForLine(0, 30)
        viewModel.setLedsForLine(1, 60)
        viewModel.setLedsForLine(2, 90)
        viewModel.setLedsForLine(3, 120)

        state = viewModel.uiState.value
        assertEquals(listOf(30, 60, 90, 120), state.ledsPerLine)
    }

    @Test
    fun `setLineCount respects maxLines from capabilities`() = runTest(StandardTestDispatcher()) {
        val (viewModel, _) = createConnectedViewModel()

        // MockTransport sets maxLines = 4, so 5 should clamp to 4
        viewModel.setLineCount(5)
        assertEquals(4, viewModel.uiState.value.lineCount)
    }

    private fun TestScope.createViewModel(): SetupViewModel {
        val transport = MockTransport()
        val repository = DeviceRepositoryImpl(transport, transport)
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        return SetupViewModel(repository, scope = scope)
    }

    private suspend fun TestScope.createConnectedViewModel(): Pair<SetupViewModel, DeviceRepositoryImpl> {
        val transport = MockTransport()
        val repository = DeviceRepositoryImpl(transport, transport)
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val viewModel = SetupViewModel(repository, scope = scope)

        repository.scan()
        advanceUntilIdle()
        repository.connect("00:11:22:33:44:01")
        advanceUntilIdle()

        return Pair(viewModel, repository)
    }
}
