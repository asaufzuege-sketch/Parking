package ch.parkassist.app.domain.state

import ch.parkassist.app.domain.model.ParkingSession
import java.time.Instant

sealed class ParkingState {
    /** No active session. */
    object Idle : ParkingState()

    /** Session configured and waiting for scheduled start time. */
    data class Scheduled(val session: ParkingSession, val scheduledAt: Instant) : ParkingState()

    /** App/deep-link to provider is being launched. */
    data class LaunchingProvider(val session: ParkingSession) : ParkingState()

    /** Provider app launched; waiting for completion result. */
    data class AwaitingUser(val session: ParkingSession) : ParkingState()

    /** Parking ticket is active. */
    data class Active(val session: ParkingSession, val expiresAt: Instant) : ParkingState()

    /** Ticket about to expire; extension decision pending. */
    data class ExtensionDue(val session: ParkingSession, val expiresAt: Instant) : ParkingState()

    /** Session completed normally. */
    data class Completed(val session: ParkingSession, val endedAt: Instant) : ParkingState()

    /** Session cancelled by user. */
    data class Cancelled(val session: ParkingSession, val cancelledAt: Instant) : ParkingState()

    /** An error occurred. */
    data class Error(val session: ParkingSession?, val message: String) : ParkingState()
}
