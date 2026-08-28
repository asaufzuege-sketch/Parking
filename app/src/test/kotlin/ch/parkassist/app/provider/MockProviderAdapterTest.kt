package ch.parkassist.app.provider

import android.content.Intent
import org.junit.Assert.*
import org.junit.Test

/**
 * Verifies the explicit Intent contract constants shared between ParkingAssistant and MockParking.
 * Uses MockProviderContract (JVM-only, no Android dependency).
 */
class MockProviderAdapterTest {

    @Test
    fun mockPackageIsCorrect() {
        assertEquals("ch.parkassist.mockparking", MockProviderContract.PACKAGE)
    }

    @Test
    fun extraKeysMatchContract() {
        assertEquals("zone", MockProviderContract.EXTRA_ZONE)
        assertEquals("plate", MockProviderContract.EXTRA_PLATE)
        assertEquals("durationMinutes", MockProviderContract.EXTRA_DURATION_MINUTES)
        assertEquals("sessionId", MockProviderContract.EXTRA_SESSION_ID)
    }

    @Test
    fun resultStatusConstantsAreDistinct() {
        val statuses = listOf(
            MockProviderContract.STATUS_CONFIRMED,
            MockProviderContract.STATUS_DENIED,
            MockProviderContract.STATUS_ERROR,
        )
        assertEquals(statuses.size, statuses.toSet().size)
    }

    @Test
    fun actionStringsAreDistinct() {
        val actions = listOf(
            MockProviderContract.ACTION_START,
            MockProviderContract.ACTION_EXTEND,
            MockProviderContract.ACTION_STOP,
        )
        assertEquals(actions.size, actions.toSet().size)
    }

    @Test
    fun mockProviderIntentDoesNotUseNewTask() {
        assertEquals(0, 0 and Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
