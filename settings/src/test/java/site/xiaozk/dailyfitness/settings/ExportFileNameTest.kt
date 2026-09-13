package site.xiaozk.dailyfitness.settings

import kotlinx.datetime.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The export file name must be stable, sortable and unique per second so repeated
 * exports never overwrite each other.
 */
class ExportFileNameTest {

    @Test
    fun `contains the timestamp down to the second`() {
        assertEquals(
            "dailyfitness-export-2024-05-06-081530.json",
            exportFileName(LocalDateTime(2024, 5, 6, 8, 15, 30)),
        )
    }

    @Test
    fun `pads single digit date and time parts`() {
        assertEquals(
            "dailyfitness-export-2024-01-02-030405.json",
            exportFileName(LocalDateTime(2024, 1, 2, 3, 4, 5)),
        )
    }
}
