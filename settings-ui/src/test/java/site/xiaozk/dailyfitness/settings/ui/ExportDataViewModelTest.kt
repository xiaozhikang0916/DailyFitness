package site.xiaozk.dailyfitness.settings.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.io.files.Path
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import site.xiaozk.dailyfitness.repository.ISettingRepository
import kotlin.time.Clock

/**
 * The export screen must use the app-injected directory provider and write a
 * correctly named file through [ISettingRepository]; both are faked here so the
 * flow is tested without Android or real files.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ExportDataViewModelTest {

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
    fun `exports a json file into the picked directory`() = runTest(dispatcher) {
        val repository = FakeSettingRepository()
        val directory = Path("/tmp/exports")
        val viewModel = ExportDataViewModel(
            settingRepository = repository,
            directoryProvider = FakeDirectoryProvider(directory),
            clock = Clock.System,
        )

        viewModel.export()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.exporting)
        assertFalse(state.failed)
        val exported = requireNotNull(repository.exportedPath)
        assertEquals(directory, exported.parent)
        assertTrue(exported.name.startsWith("dailyfitness-export-"))
        assertTrue(exported.name.endsWith(".json"))
        assertEquals(exported.toString(), state.exportedPath)
    }

    @Test
    fun `stays idle when the user cancels the picker`() = runTest(dispatcher) {
        val repository = FakeSettingRepository()
        val viewModel = ExportDataViewModel(
            settingRepository = repository,
            directoryProvider = FakeDirectoryProvider(null),
            clock = Clock.System,
        )

        viewModel.export()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.exporting)
        assertFalse(state.failed)
        assertNull(state.exportedPath)
        assertNull(repository.exportedPath)
    }

    @Test
    fun `reports failure when the write throws`() = runTest(dispatcher) {
        val repository = FakeSettingRepository(fail = true)
        val viewModel = ExportDataViewModel(
            settingRepository = repository,
            directoryProvider = FakeDirectoryProvider(Path("/tmp/exports")),
            clock = Clock.System,
        )

        viewModel.export()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.exporting)
        assertTrue(state.failed)
        assertNull(state.exportedPath)
    }

    private class FakeDirectoryProvider(private val directory: Path?) : ExportDirectoryProvider {
        override suspend fun pickExportDirectory(): Path? = directory
    }

    private class FakeSettingRepository(private val fail: Boolean = false) : ISettingRepository {
        var exportedPath: Path? = null

        override suspend fun exportAllDataTo(path: Path) {
            if (fail) error("boom")
            exportedPath = path
        }

        override suspend fun importAllDataFrom(path: Path) = Unit
    }
}
