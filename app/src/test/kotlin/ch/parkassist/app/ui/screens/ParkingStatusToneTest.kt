package ch.parkassist.app.ui.screens

import ch.parkassist.app.domain.model.ParkingSession
import ch.parkassist.app.domain.model.Provider
import ch.parkassist.app.domain.state.ParkingState
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class ParkingStatusToneTest {

    private val session = ParkingSession(
        provider = Provider.MOCK,
        zone = "8001",
        licensePlate = "ZH123456",
        ticketDurationMinutes = 60,
        maxExtensions = 1,
        startTime = Instant.parse("2026-08-27T10:00:00Z"),
        confirmedByUser = true,
    )

    @Test
    fun activeState_uses_active_tone() {
        val tone = ParkingState.Active(session, session.startTime.plusSeconds(3600)).statusTone()

        assertEquals(ParkingStatusTone.ACTIVE, tone)
    }

    @Test
    fun waitingStates_use_waiting_tone() {
        val states = listOf(
            ParkingState.Scheduled(session, session.startTime),
            ParkingState.LaunchingProvider(session),
            ParkingState.AwaitingUser(session),
            ParkingState.ExtensionDue(session, session.startTime.plusSeconds(3600)),
        )

        states.forEach { state ->
            assertEquals(ParkingStatusTone.WAITING, state.statusTone())
        }
    }

    @Test
    fun terminalStates_use_neutral_tone() {
        val states = listOf(
            ParkingState.Idle,
            ParkingState.Completed(session, session.startTime.plusSeconds(3600)),
            ParkingState.Cancelled(session, session.startTime.plusSeconds(1800)),
        )

        states.forEach { state ->
            assertEquals(ParkingStatusTone.NEUTRAL, state.statusTone())
        }
    }

    @Test
    fun errorState_uses_error_tone() {
        val tone = ParkingState.Error(session, "Anbieter meldet Fehler").statusTone()

        assertEquals(ParkingStatusTone.ERROR, tone)
    }
}
