package ch.parkassist.app.domain.state

import ch.parkassist.app.domain.model.ParkingSession
import java.time.Instant

/**
 * Deterministic state machine for parking sessions.
 * All transitions are pure functions – no side effects.
 */
object ParkingStateMachine {

    fun transition(current: ParkingState, event: ParkingEvent): ParkingState {
        return when {
            event is ParkingEvent.Reset -> ParkingState.Idle

            current is ParkingState.Idle && event is ParkingEvent.Start -> {
                val now = Instant.now()
                if (event.session.startTime.isAfter(now)) {
                    ParkingState.Scheduled(event.session, event.session.startTime)
                } else {
                    ParkingState.LaunchingProvider(event.session)
                }
            }

            current is ParkingState.Scheduled && event is ParkingEvent.ProviderLaunched ->
                ParkingState.LaunchingProvider(current.session)

            current is ParkingState.LaunchingProvider && event is ParkingEvent.ProviderConfirmed ->
                ParkingState.AwaitingUser(current.session)

            current is ParkingState.AwaitingUser && event is ParkingEvent.TicketActive -> {
                val expires = Instant.now()
                    .plusSeconds((current.session.ticketDurationMinutes * 60).toLong())
                ParkingState.Active(current.session, expires)
            }

            current is ParkingState.Active && event is ParkingEvent.ExtensionRequired -> {
                ParkingState.ExtensionDue(current.session, current.expiresAt)
            }

            current is ParkingState.Active && event is ParkingEvent.StopRequested ->
                ParkingState.Cancelled(current.session, Instant.now())

            current is ParkingState.Active && event is ParkingEvent.SessionExpired ->
                ParkingState.Completed(current.session, Instant.now())

            current is ParkingState.ExtensionDue && event is ParkingEvent.ExtendConfirmed -> {
                val updated = current.session.copy(extensionsUsed = current.session.extensionsUsed + 1)
                val expires = Instant.now()
                    .plusSeconds((updated.ticketDurationMinutes * 60).toLong())
                ParkingState.Active(updated, expires)
            }

            current is ParkingState.ExtensionDue && event is ParkingEvent.StopRequested ->
                ParkingState.Completed(current.session, Instant.now())

            event is ParkingEvent.ErrorOccurred -> {
                val session = when (current) {
                    is ParkingState.Idle -> null
                    is ParkingState.Scheduled -> current.session
                    is ParkingState.LaunchingProvider -> current.session
                    is ParkingState.AwaitingUser -> current.session
                    is ParkingState.Active -> current.session
                    is ParkingState.ExtensionDue -> current.session
                    is ParkingState.Completed -> current.session
                    is ParkingState.Cancelled -> current.session
                    is ParkingState.Error -> current.session
                }
                ParkingState.Error(session, event.message)
            }

            else -> current // No valid transition; stay in current state
        }
    }
}
