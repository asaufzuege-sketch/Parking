package ch.parkassist.app.ui

import ch.parkassist.app.data.db.SessionEntity
import ch.parkassist.app.domain.model.Provider
import ch.parkassist.app.domain.state.ParkingState
import ch.parkassist.app.domain.state.ParkingStateNames
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ParkingStateRestoreMapperTest {

    @Test
    fun `restoring LaunchingProvider maps to AwaitingUser`() {
        val entity = sessionEntity(state = ParkingStateNames.LAUNCHING_PROVIDER)
        val restored = entity.toRestoredParkingState(entity.toParkingSession())
        assertTrue(restored is ParkingState.AwaitingUser)
    }

    @Test
    fun `restoring Active maps to Active`() {
        val entity = sessionEntity(state = ParkingStateNames.ACTIVE)
        val restored = entity.toRestoredParkingState(entity.toParkingSession())
        assertTrue(restored is ParkingState.Active)
        val active = restored as ParkingState.Active
        assertEquals(
            entity.toParkingSession().startTime.plusSeconds((entity.ticketDurationMinutes * 60).toLong()),
            active.expiresAt
        )
    }

    @Test
    fun `restoring Active with used extensions keeps accumulated expiry`() {
        val entity = sessionEntity(state = ParkingStateNames.ACTIVE, extensionsUsed = 2)
        val restored = entity.toRestoredParkingState(entity.toParkingSession())
        val active = restored as ParkingState.Active
        assertEquals(
            entity.toParkingSession().startTime.plusSeconds((entity.ticketDurationMinutes * 3 * 60L)),
            active.expiresAt
        )
    }

    @Test
    fun `provider fallback maps unknown provider to mock`() {
        val entity = sessionEntity(provider = "UNKNOWN")
        assertEquals(Provider.MOCK, entity.toParkingSession().provider)
    }

    @Test
    fun `legacy EasyPark provider maps to Parkingpay`() {
        val entity = sessionEntity(provider = "EASYPARK")
        assertEquals(Provider.PARKINGPAY, entity.toParkingSession().provider)
    }

    private fun sessionEntity(
        state: String = ParkingStateNames.ACTIVE,
        provider: String = Provider.MOCK.name,
        extensionsUsed: Int = 0,
    ) = SessionEntity(
        id = 9,
        provider = provider,
        zone = "8000",
        licensePlate = "ZH123456",
        ticketDurationMinutes = 30,
        maxExtensions = 1,
        extensionsUsed = extensionsUsed,
        startTimeEpoch = Instant.now().toEpochMilli(),
        confirmedByUser = true,
        state = state,
    )
}
