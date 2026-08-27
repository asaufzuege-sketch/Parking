package ch.parkassist.app.domain.model

import java.time.Instant

data class ParkingSession(
    val id: Long = 0,
    val provider: Provider,
    val zone: String,
    val licensePlate: String,
    val ticketDurationMinutes: Int,
    val maxExtensions: Int,
    val extensionsUsed: Int = 0,
    val startTime: Instant,
    val confirmedByUser: Boolean = false,
)
