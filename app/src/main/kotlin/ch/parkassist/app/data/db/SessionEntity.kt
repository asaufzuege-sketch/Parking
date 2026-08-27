package ch.parkassist.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import ch.parkassist.app.domain.model.Provider

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val provider: String,
    val zone: String,
    val licensePlate: String,
    val ticketDurationMinutes: Int,
    val maxExtensions: Int,
    val extensionsUsed: Int,
    val startTimeEpoch: Long,
    val confirmedByUser: Boolean,
    val state: String, // ParkingState class simple name
)
