package ch.parkassist.app.domain

import ch.parkassist.app.domain.model.ParkingSession
import ch.parkassist.app.domain.model.Provider
import ch.parkassist.app.domain.state.*
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class StateMachineTest {

    private val session = ParkingSession(
        provider = Provider.MOCK,
        zone = "ZONE-A",
        licensePlate = "ZH 1234",
        ticketDurationMinutes = 60,
        maxExtensions = 2,
        startTime = Instant.now(),
        confirmedByUser = true,
    )

    @Test
    fun `idle + start now results_in_LaunchingProvider`() {
        val newState = ParkingStateMachine.transition(
            ParkingState.Idle,
            ParkingEvent.Start(session.copy(startTime = Instant.now().minusSeconds(1)))
        )
        assertTrue(newState is ParkingState.LaunchingProvider)
    }

    @Test
    fun `idle + start future results_in_Scheduled`() {
        val future = Instant.now().plusSeconds(3600)
        val newState = ParkingStateMachine.transition(
            ParkingState.Idle,
            ParkingEvent.Start(session.copy(startTime = future))
        )
        assertTrue(newState is ParkingState.Scheduled)
    }

    @Test
    fun `LaunchingProvider + ProviderLaunched results_in_AwaitingUser`() {
        val state = ParkingState.LaunchingProvider(session)
        val newState = ParkingStateMachine.transition(state, ParkingEvent.ProviderLaunched)
        assertTrue(newState is ParkingState.AwaitingUser)
    }

    @Test
    fun `AwaitingUser + ProviderConfirmed results_in_Active`() {
        val state = ParkingState.AwaitingUser(session)
        val newState = ParkingStateMachine.transition(state, ParkingEvent.ProviderConfirmed)
        assertTrue(newState is ParkingState.Active)
    }

    @Test
    fun `AwaitingUser + ProviderDenied results_in_Cancelled`() {
        val state = ParkingState.AwaitingUser(session)
        val newState = ParkingStateMachine.transition(state, ParkingEvent.ProviderDenied)
        assertTrue(newState is ParkingState.Cancelled)
    }

    @Test
    fun `AwaitingUser + ProviderCancelled results_in_Cancelled`() {
        val state = ParkingState.AwaitingUser(session)
        val newState = ParkingStateMachine.transition(state, ParkingEvent.ProviderCancelled)
        assertTrue(newState is ParkingState.Cancelled)
    }

    @Test
    fun `Active + ExtensionRequired results_in_ExtensionDue`() {
        val expires = Instant.now().plusSeconds(60)
        val state = ParkingState.Active(session, expires)
        val newState = ParkingStateMachine.transition(state, ParkingEvent.ExtensionRequired)
        assertTrue(newState is ParkingState.ExtensionDue)
    }

    @Test
    fun `ExtensionDue + ExtendConfirmed results_in_Active with incremented extensions`() {
        val expires = Instant.now().plusSeconds(60)
        val state = ParkingState.ExtensionDue(session, expires)
        val newState = ParkingStateMachine.transition(state, ParkingEvent.ExtendConfirmed)
        assertTrue(newState is ParkingState.Active)
        assertEquals(1, (newState as ParkingState.Active).session.extensionsUsed)
    }

    @Test
    fun `Active + StopRequested results_in_Cancelled`() {
        val expires = Instant.now().plusSeconds(60)
        val state = ParkingState.Active(session, expires)
        val newState = ParkingStateMachine.transition(state, ParkingEvent.StopRequested)
        assertTrue(newState is ParkingState.Cancelled)
    }

    @Test
    fun `Active + SessionExpired results_in_Completed`() {
        val expires = Instant.now().minusSeconds(1)
        val state = ParkingState.Active(session, expires)
        val newState = ParkingStateMachine.transition(state, ParkingEvent.SessionExpired)
        assertTrue(newState is ParkingState.Completed)
    }

    @Test
    fun `any state + ErrorOccurred results_in_Error`() {
        val state = ParkingState.Active(session, Instant.now())
        val newState = ParkingStateMachine.transition(state, ParkingEvent.ErrorOccurred("Test error"))
        assertTrue(newState is ParkingState.Error)
        assertEquals("Test error", (newState as ParkingState.Error).message)
    }

    @Test
    fun `Error + Reset results_in_Idle`() {
        val state = ParkingState.Error(session, "some error")
        val newState = ParkingStateMachine.transition(state, ParkingEvent.Reset)
        assertTrue(newState is ParkingState.Idle)
    }

    @Test
    fun `unknown transition stays in current state`() {
        val state = ParkingState.Scheduled(session, Instant.now())
        val newState = ParkingStateMachine.transition(state, ParkingEvent.TicketActive)
        assertEquals(state, newState)
    }
}
