package site.xiaozk.dailyfitness.settings

import kotlinx.datetime.LocalDateTime
import kotlinx.io.files.Path
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The export file name must be stable, sortable and unique per second so repeated
 * exports never overwrite each other.
 */
class ExportFilePathTest {

    @Test
    fun `appends a timestamped json file name to the directory`() {
        val path = exportFilePath(
            directory = Path("/storage/emulated/0/Download"),
            timestamp = LocalDateTime(2024, 5, 6, 8, 15, 30),
        )

        assertEquals(
            "/storage/emulated/0/Download/dailyfitness-export-2024-05-06-081530.json",
            path.toString(),
        )
    }

    @Test
    fun `pads single digit date and time parts`() {
        val path = exportFilePath(
            directory = Path("/tmp"),
            timestamp = LocalDateTime(2024, 1, 2, 3, 4, 5),
        )

        assertEquals("/tmp/dailyfitness-export-2024-01-02-030405.json", path.toString())
    }
}
