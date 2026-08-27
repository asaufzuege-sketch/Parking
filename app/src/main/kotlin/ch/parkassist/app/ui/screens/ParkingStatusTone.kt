package ch.parkassist.app.ui.screens

import ch.parkassist.app.domain.state.ParkingState

internal enum class ParkingStatusTone {
    ACTIVE,
    WAITING,
    ERROR,
    NEUTRAL,
}

internal fun ParkingState.statusTone(): ParkingStatusTone = when (this) {
    is ParkingState.Active -> ParkingStatusTone.ACTIVE
    is ParkingState.Scheduled,
    is ParkingState.LaunchingProvider,
    is ParkingState.AwaitingUser,
    is ParkingState.ExtensionDue -> ParkingStatusTone.WAITING
    is ParkingState.Error -> ParkingStatusTone.ERROR
    is ParkingState.Idle,
    is ParkingState.Completed,
    is ParkingState.Cancelled -> ParkingStatusTone.NEUTRAL
}
