package ch.parkassist.app.domain.state

object ParkingStateNames {
    const val SCHEDULED = "Scheduled"
    const val LAUNCHING_PROVIDER = "LaunchingProvider"
    const val AWAITING_USER = "AwaitingUser"
    const val ACTIVE = "Active"
    const val EXTENSION_DUE = "ExtensionDue"
    const val COMPLETED = "Completed"
    const val CANCELLED = "Cancelled"
    const val ERROR = "Error"
}
