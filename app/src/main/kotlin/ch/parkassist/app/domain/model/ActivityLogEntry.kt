package ch.parkassist.app.domain.model

import java.time.Instant

data class ActivityLogEntry(
    val id: Long = 0,
    val sessionId: Long,
    val timestamp: Instant,
    val event: String,
    val detail: String = "",
)
