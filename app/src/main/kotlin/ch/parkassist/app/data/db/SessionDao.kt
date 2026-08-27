package ch.parkassist.app.data.db

import androidx.room.*
import ch.parkassist.app.domain.state.ParkingStateNames
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY id DESC LIMIT 1")
    fun observeLatest(): Flow<SessionEntity?>

    @Query(
        "SELECT * FROM sessions " +
            "WHERE state NOT IN ('" + ParkingStateNames.COMPLETED + "', '" + ParkingStateNames.CANCELLED + "') " +
            "ORDER BY id DESC LIMIT 1"
    )
    suspend fun getLatestRestorable(): SessionEntity?

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getById(id: Long): SessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SessionEntity): Long

    @Query("UPDATE sessions SET state = :state, extensionsUsed = :extensions WHERE id = :id")
    suspend fun updateStateAndExtensions(id: Long, state: String, extensions: Int)

    @Query("SELECT * FROM sessions ORDER BY id DESC")
    suspend fun getAll(): List<SessionEntity>
}
