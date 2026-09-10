package site.xiaozk.dailyfitness.settings.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import site.xiaozk.dailyfitness.repository.IAiCoachConfigStore
import site.xiaozk.dailyfitness.repository.model.AiCoachConfig
import site.xiaozk.dailyfitness.repository.model.AiCoachModel

/**
 * M3.1: the settings ViewModel reads the persisted config and writes edits back
 * through the `:repository` store (which the `AiCoachConfigProvider` observes).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AiCoachSettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads the persisted config`() = runTest(dispatcher) {
        val store = FakeConfigStore(
            AiCoachConfig(apiKey = "old-key", model = AiCoachModel.DeepSeekV4Pro, baseUrl = "https://x")
        )
        val viewModel = AiCoachSettingsViewModel(store)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state.loaded)
        assertEquals("old-key", state.apiKey)
        assertEquals(AiCoachModel.DeepSeekV4Pro, state.model)
        assertEquals("https://x", state.baseUrl)
        assertFalse(state.saved)
    }

    @Test
    fun `save writes the edited config back to the store`() = runTest(dispatcher) {
        val store = FakeConfigStore(AiCoachConfig(apiKey = "old-key"))
        val viewModel = AiCoachSettingsViewModel(store)
        advanceUntilIdle()

        viewModel.onApiKeyChange("  new-key  ")
        viewModel.onModelChange(AiCoachModel.DeepSeekV4Pro)
        viewModel.onBaseUrlChange("https://custom.example.com")
        viewModel.save()
        advanceUntilIdle()

        assertEquals(
            AiCoachConfig(
                apiKey = "new-key",
                model = AiCoachModel.DeepSeekV4Pro,
                baseUrl = "https://custom.example.com",
            ),
            store.saved,
        )
        assertTrue(viewModel.state.value.saved)
    }

    @Test
    fun `save is ignored while the key is blank`() = runTest(dispatcher) {
        val store = FakeConfigStore(AiCoachConfig(apiKey = "old-key"))
        val viewModel = AiCoachSettingsViewModel(store)
        advanceUntilIdle()

        viewModel.onApiKeyChange("   ")
        viewModel.save()
        advanceUntilIdle()

        assertFalse(viewModel.state.value.canSave)
        assertEquals(null, store.saved)
    }

    private class FakeConfigStore(initial: AiCoachConfig) : IAiCoachConfigStore {
        private val state = MutableStateFlow(initial)
        var saved: AiCoachConfig? = null

        override fun observe(): Flow<AiCoachConfig> = state

        override suspend fun save(config: AiCoachConfig) {
            saved = config
            state.value = config
        }
    }
}
