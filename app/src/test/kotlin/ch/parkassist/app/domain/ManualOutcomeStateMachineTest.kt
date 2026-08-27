package ch.parkassist.app.domain

import ch.parkassist.app.domain.model.ParkingSession
import ch.parkassist.app.domain.model.Provider
import ch.parkassist.app.domain.state.ManualOutcome
import ch.parkassist.app.domain.state.ParkingEvent
import ch.parkassist.app.domain.state.ParkingState
import ch.parkassist.app.domain.state.ParkingStateMachine
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ManualOutcomeStateMachineTest {

    private val session = ParkingSession(
        provider = Provider.PARKINGPAY,
        zone = "ZONE-A",
        licensePlate = "ZH 1234",
        ticketDurationMinutes = 60,
        maxExtensions = 0,
        startTime = Instant.now(),
        confirmedByUser = true,
    )

    @Test
    fun `AwaitingUser + ManualOutcomeConfirmed results_in_Active`() {
        val state = ParkingState.AwaitingUser(session)
        val newState = ParkingStateMachine.transition(
            state,
            ParkingEvent.ManualOutcomeReported(ManualOutcome.CONFIRMED)
        )
        assertTrue(newState is ParkingState.Active)
    }

    @Test
    fun `AwaitingUser + ManualOutcomeNotCompleted results_in_Cancelled`() {
        val state = ParkingState.AwaitingUser(session)
        val newState = ParkingStateMachine.transition(
            state,
            ParkingEvent.ManualOutcomeReported(ManualOutcome.NOT_COMPLETED)
        )
        assertTrue(newState is ParkingState.Cancelled)
    }

    @Test
    fun `AwaitingUser + ManualOutcomeUnclear results_in_Error`() {
        val state = ParkingState.AwaitingUser(session)
        val newState = ParkingStateMachine.transition(
            state,
            ParkingEvent.ManualOutcomeReported(ManualOutcome.UNCLEAR)
        )
        assertTrue(newState is ParkingState.Error)
    }
}
