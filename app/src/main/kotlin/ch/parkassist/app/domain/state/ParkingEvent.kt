package ch.parkassist.app.domain.state

import ch.parkassist.app.domain.model.ParkingSession

sealed class ParkingEvent {
    data class Start(val session: ParkingSession) : ParkingEvent()
    object ProviderLaunched : ParkingEvent()
    object ProviderConfirmed : ParkingEvent()
    object ProviderDenied : ParkingEvent()
    object ProviderCancelled : ParkingEvent()
    object TicketActive : ParkingEvent()
    object ExtensionRequired : ParkingEvent()
    object ExtendConfirmed : ParkingEvent()
    object StopRequested : ParkingEvent()
    object SessionExpired : ParkingEvent()
    /** User manually reports the outcome after returning from a manual provider. */
    data class ManualOutcomeReported(val outcome: ManualOutcome) : ParkingEvent()
    data class ErrorOccurred(val message: String) : ParkingEvent()
    object Reset : ParkingEvent()
}
