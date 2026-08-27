package ch.parkassist.app.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LogDao {
    @Query("SELECT * FROM activity_log ORDER BY timestampEpoch DESC")
    fun observeAll(): Flow<List<LogEntity>>

    @Insert
    suspend fun insert(entry: LogEntity)

    @Query("DELETE FROM activity_log")
    suspend fun clear()
}
