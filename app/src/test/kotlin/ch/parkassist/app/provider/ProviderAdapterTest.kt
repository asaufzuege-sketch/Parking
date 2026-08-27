package ch.parkassist.app.provider

import ch.parkassist.app.domain.model.ParkingSession
import ch.parkassist.app.domain.model.Provider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ProviderAdapterTest {

    @Test
    fun mockAdapterSupportsAutomation() {
        assertTrue(MockParkingAdapter.capabilities.supportsAutomation)
        assertFalse(MockParkingAdapter.capabilities.requiresManualHandoff)
    }

    @Test
    fun parkingpayAdapterDoesNotSupportAutomation() {
        assertFalse(ParkingpayManualAdapter.capabilities.supportsAutomation)
        assertTrue(ParkingpayManualAdapter.capabilities.requiresManualHandoff)
    }

    @Test
    fun twintAdapterDoesNotSupportAutomation() {
        assertFalse(TwintManualAdapter.capabilities.supportsAutomation)
        assertTrue(TwintManualAdapter.capabilities.requiresManualHandoff)
    }

    @Test
    fun registryAutomationOnlyForMock() {
        assertTrue(ProviderRegistry.supportsAutomation(Provider.MOCK))
        assertFalse(ProviderRegistry.supportsAutomation(Provider.PARKINGPAY))
        assertFalse(ProviderRegistry.supportsAutomation(Provider.TWINT))
    }

    @Test
    fun requireMockAutomationPassesForMock() {
        requireMockAutomation(Provider.MOCK)
    }

    @Test(expected = IllegalStateException::class)
    fun requireMockAutomationFailsForParkingpay() {
        requireMockAutomation(Provider.PARKINGPAY)
    }

    @Test(expected = IllegalStateException::class)
    fun requireMockAutomationFailsForTwint() {
        requireMockAutomation(Provider.TWINT)
    }

    @Test
    fun mockAutomationConfigValidation() {
        val config = MockAutomationConfig(repetitions = 3, intervalSeconds = 10)
        assertEquals(3, config.repetitions)
    }

    @Test(expected = IllegalArgumentException::class)
    fun mockAutomationConfigRejectsExcessiveRepetitions() {
        MockAutomationConfig(repetitions = MockAutomationConfig.MAX_REPETITIONS + 1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun mockAutomationConfigRejectsZeroRepetitions() {
        MockAutomationConfig(repetitions = 0)
    }

    @Test
    fun dryRunDescriptionContainsNoSensitiveData() {
        val session = ParkingSession(
            provider = Provider.MOCK,
            zone = "8001",
            licensePlate = "ZH 99999",
            ticketDurationMinutes = 30,
            maxExtensions = 0,
            startTime = Instant.now(),
        )
        val desc = MockParkingAdapter.dryRunDescription(session, ProviderAction.START)
        assertTrue(desc.contains("[DRY-RUN]"))
        assertFalse(desc.contains("99999"))
    }

    @Test
    fun logMetadataDoesNotContainPlate() {
        val session = ParkingSession(
            provider = Provider.MOCK,
            zone = "8001",
            licensePlate = "ZH 99999",
            ticketDurationMinutes = 30,
            maxExtensions = 0,
            startTime = Instant.now(),
        )
        val meta = MockParkingAdapter.logMetadata(session)
        assertFalse(meta.values.any { it.contains("99999") })
    }
}
