package site.xiaozk.dailyfitness.settings.ui

import kotlin.time.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.io.Buffer
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.files.Path
import kotlinx.io.writeString
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import site.xiaozk.dailyfitness.repository.ISettingRepository

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
    fun `writes a timestamped json file into the picked directory`() = runTest(dispatcher) {
        val repository = FakeSettingRepository()
        val provider = FakeDirectoryProvider { name ->
            ExportTarget(displayPath = "/tmp/exports/$name", sink = Buffer())
        }
        val viewModel = ExportDataViewModel(repository, provider, Clock.System)

        viewModel.export()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.exporting)
        assertFalse(state.failed)
        val fileName = requireNotNull(provider.lastFileName)
        assertTrue(fileName.startsWith("dailyfitness-export-"))
        assertTrue(fileName.endsWith(".json"))
        assertEquals("/tmp/exports/$fileName", state.exportedPath)
        assertTrue(repository.exported)
    }

    @Test
    fun `stays idle when the user cancels the picker`() = runTest(dispatcher) {
        val repository = FakeSettingRepository()
        val viewModel = ExportDataViewModel(
            settingRepository = repository,
            directoryProvider = FakeDirectoryProvider { null },
            clock = Clock.System,
        )

        viewModel.export()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.exporting)
        assertFalse(state.failed)
        assertNull(state.exportedPath)
        assertFalse(repository.exported)
    }

    @Test
    fun `reports failure when the write throws`() = runTest(dispatcher) {
        val repository = FakeSettingRepository(fail = true)
        val viewModel = ExportDataViewModel(
            settingRepository = repository,
            directoryProvider = FakeDirectoryProvider { name ->
                ExportTarget(displayPath = "/tmp/exports/$name", sink = Buffer())
            },
            clock = Clock.System,
        )

        viewModel.export()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.exporting)
        assertTrue(state.failed)
        assertNull(state.exportedPath)
    }

    private class FakeDirectoryProvider(
        private val buildTarget: (String) -> ExportTarget?,
    ) : ExportDirectoryProvider {
        var lastFileName: String? = null

        override suspend fun createExportTarget(fileName: String): ExportTarget? {
            lastFileName = fileName
            return buildTarget(fileName)
        }
    }

    private class FakeSettingRepository(private val fail: Boolean = false) : ISettingRepository {
        var exported: Boolean = false

        override suspend fun exportAllDataTo(path: Path) = Unit

        override suspend fun exportAllDataTo(sink: Sink) {
            if (fail) error("boom")
            sink.writeString("{}")
            exported = true
        }

        override suspend fun importAllDataFrom(path: Path) = Unit

        override suspend fun importAllDataFrom(source: Source) = Unit
    }
}
