package ch.parkassist.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activity_log")
data class LogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val timestampEpoch: Long,
    val event: String,
    val detail: String,
)
