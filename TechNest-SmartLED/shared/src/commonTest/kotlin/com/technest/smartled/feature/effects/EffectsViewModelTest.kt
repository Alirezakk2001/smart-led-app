package com.technest.smartled.feature.effects

import com.technest.smartled.core.model.DeviceConfiguration
import com.technest.smartled.core.model.EffectId
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
class EffectsViewModelTest {

    @Test
    fun `initial state lists all effects`() = runTest(StandardTestDispatcher()) {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value

        assertEquals(EffectId.entries.size, state.effects.size)
        assertNull(state.selectedEffectId)
        assertFalse(state.isConnected)
    }

    @Test
    fun `effects have correct names and descriptions`() = runTest(StandardTestDispatcher()) {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value

        val staticEffect = state.effects.find { it.effectId == EffectId.Static }
        assertNotNull(staticEffect)
        assertEquals("Static", staticEffect.name)
        assertEquals("Solid color display", staticEffect.description)

        val rainbow = state.effects.find { it.effectId == EffectId.Rainbow }
        assertNotNull(rainbow)
        assertEquals("Rainbow", rainbow.name)
    }

    @Test
    fun `selectEffect updates selected effect and parameters`() = runTest(StandardTestDispatcher()) {
        val viewModel = createViewModel()

        viewModel.selectEffect(EffectId.Rainbow)
        val state = viewModel.uiState.value

        assertEquals(EffectId.Rainbow, state.selectedEffectId)
        assertTrue(state.editingParameters.isNotEmpty())
        assertEquals("Speed", state.editingParameters[0].name)
    }

    @Test
    fun `selectEffect on Static returns empty parameters`() = runTest(StandardTestDispatcher()) {
        val viewModel = createViewModel()

        viewModel.selectEffect(EffectId.Static)
        val state = viewModel.uiState.value

        assertEquals(EffectId.Static, state.selectedEffectId)
        assertTrue(state.editingParameters.isEmpty())
    }

    @Test
    fun `updateParameter modifies parameter value`() = runTest(StandardTestDispatcher()) {
        val viewModel = createViewModel()
        viewModel.selectEffect(EffectId.Rainbow)

        viewModel.updateParameter(0, 200)
        val state = viewModel.uiState.value

        assertEquals(200, state.editingParameters[0].value)
    }

    @Test
    fun `updateParameter clamps to valid range`() = runTest(StandardTestDispatcher()) {
        val viewModel = createViewModel()
        viewModel.selectEffect(EffectId.Rainbow)

        viewModel.updateParameter(0, 999)
        val state = viewModel.uiState.value

        assertEquals(255, state.editingParameters[0].value)
    }

    @Test
    fun `getDefaultParameters returns correct parameters per effect`() {
        assertTrue(getDefaultParameters(EffectId.Static).isEmpty())
        assertEquals(1, getDefaultParameters(EffectId.Rainbow).size)
        assertEquals("Speed", getDefaultParameters(EffectId.Rainbow)[0].name)
        assertEquals(1, getDefaultParameters(EffectId.Breathing).size)
        assertEquals(1, getDefaultParameters(EffectId.Fire).size)
        assertEquals(1, getDefaultParameters(EffectId.Twinkle).size)
        assertEquals(1, getDefaultParameters(EffectId.Meteor).size)
        assertEquals(1, getDefaultParameters(EffectId.Wave).size)
        assertEquals(1, getDefaultParameters(EffectId.Police).size)
        assertEquals(1, getDefaultParameters(EffectId.Gradient).size)
    }

    @Test
    fun `getEffectDescription returns non-empty for all effects`() {
        EffectId.entries.forEach { id ->
            val desc = getEffectDescription(id)
            assertTrue(desc.isNotEmpty(), "Description for $id should not be empty")
        }
    }

    @Test
    fun `isConnected becomes true when device connects`() = runTest(StandardTestDispatcher()) {
        val transport = MockTransport()
        val repository = DeviceRepositoryImpl(transport, transport)
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val viewModel = EffectsViewModel(repository, scope = scope)

        assertFalse(viewModel.uiState.value.isConnected)

        repository.scan()
        advanceUntilIdle()
        repository.connect("00:11:22:33:44:01")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isConnected)
    }

    @Test
    fun `applyToLine without connection shows error`() = runTest(StandardTestDispatcher()) {
        val viewModel = createViewModel()
        viewModel.selectEffect(EffectId.Rainbow)

        viewModel.applyToLine(0)
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `applyToLine with connection succeeds`() = runTest(StandardTestDispatcher()) {
        val transport = MockTransport()
        val repository = DeviceRepositoryImpl(transport, transport)
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val viewModel = EffectsViewModel(repository, scope = scope)

        repository.scan()
        advanceUntilIdle()
        repository.connect("00:11:22:33:44:01")
        advanceUntilIdle()

        viewModel.selectEffect(EffectId.Rainbow)
        viewModel.applyToLine(0)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.errorMessage)
        assertNotNull(viewModel.uiState.value.appliedMessage)
    }

    @Test
    fun `applyToAllLines with connection succeeds`() = runTest(StandardTestDispatcher()) {
        val transport = MockTransport()
        val repository = DeviceRepositoryImpl(transport, transport)
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val viewModel = EffectsViewModel(repository, scope = scope)

        repository.scan()
        advanceUntilIdle()
        repository.connect("00:11:22:33:44:01")
        advanceUntilIdle()

        val config = DeviceConfiguration(lineCount = 3, ledsPerLine = listOf(60, 120, 30))
        repository.applyConfiguration(config)
        advanceUntilIdle()

        viewModel.selectEffect(EffectId.Rainbow)
        viewModel.applyToAllLines()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.errorMessage)
        assertNotNull(viewModel.uiState.value.appliedMessage)
        assertTrue(viewModel.uiState.value.appliedMessage!!.contains("all lines"))
    }

    @Test
    fun `clearError resets error message`() = runTest(StandardTestDispatcher()) {
        val viewModel = createViewModel()
        viewModel.selectEffect(EffectId.Rainbow)
        viewModel.applyToLine(0)
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.errorMessage)

        viewModel.clearError()
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `clearAppliedMessage resets applied message`() = runTest(StandardTestDispatcher()) {
        val transport = MockTransport()
        val repository = DeviceRepositoryImpl(transport, transport)
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val viewModel = EffectsViewModel(repository, scope = scope)

        repository.scan()
        advanceUntilIdle()
        repository.connect("00:11:22:33:44:01")
        advanceUntilIdle()

        viewModel.selectEffect(EffectId.Rainbow)
        viewModel.applyToLine(0)
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.appliedMessage)

        viewModel.clearAppliedMessage()
        assertNull(viewModel.uiState.value.appliedMessage)
    }

    @Test
    fun `selecting a different effect clears applied message`() = runTest(StandardTestDispatcher()) {
        val transport = MockTransport()
        val repository = DeviceRepositoryImpl(transport, transport)
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val viewModel = EffectsViewModel(repository, scope = scope)

        repository.scan()
        advanceUntilIdle()
        repository.connect("00:11:22:33:44:01")
        advanceUntilIdle()

        viewModel.selectEffect(EffectId.Rainbow)
        viewModel.applyToLine(0)
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.appliedMessage)

        viewModel.selectEffect(EffectId.Fire)
        assertNull(viewModel.uiState.value.appliedMessage)
    }

    private fun TestScope.createViewModel(): EffectsViewModel {
        val transport = MockTransport()
        val repository = DeviceRepositoryImpl(transport, transport)
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        return EffectsViewModel(repository, scope = scope)
    }
}
