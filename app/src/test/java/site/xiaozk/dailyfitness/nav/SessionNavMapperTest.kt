package site.xiaozk.dailyfitness.nav

import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import site.xiaozk.dailyfitness.session.SessionIntents

class SessionNavMapperTest {

    private val today = LocalDate(2026, 8, 30)

    @Test
    fun `add set action maps to AddWorkoutAction`() {
        assertEquals(AddWorkoutAction, sessionActionToNavKey(SessionIntents.ACTION_ADD_SET, today))
    }

    @Test
    fun `open today action maps to TrainDay with today`() {
        assertEquals(TrainDay(date = today), sessionActionToNavKey(SessionIntents.ACTION_OPEN_TODAY, today))
    }

    @Test
    fun `unknown action maps to null`() {
        assertNull(sessionActionToNavKey("unknown.action", today))
    }

    @Test
    fun `null action maps to null`() {
        assertNull(sessionActionToNavKey(null, today))
    }
}
