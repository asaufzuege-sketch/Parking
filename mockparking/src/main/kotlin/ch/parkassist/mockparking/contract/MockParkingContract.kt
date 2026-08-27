package ch.parkassist.mockparking.contract

/** Intent contract exposed by Mock Parking to ParkingAssistant. */
object MockParkingContract {
    const val ACTION_START  = "ch.parkassist.mockparking.ACTION_START_PARKING"
    const val ACTION_EXTEND = "ch.parkassist.mockparking.ACTION_EXTEND_PARKING"
    const val ACTION_STOP   = "ch.parkassist.mockparking.ACTION_STOP_PARKING"

    const val EXTRA_ZONE             = "zone"
    const val EXTRA_PLATE            = "plate"
    const val EXTRA_DURATION_MINUTES = "durationMinutes"
    const val EXTRA_SESSION_ID       = "sessionId"

    const val RESULT_STATUS    = "status"
    const val STATUS_CONFIRMED = "CONFIRMED"
    const val STATUS_DENIED    = "DENIED"
    const val STATUS_ERROR     = "ERROR"
}
