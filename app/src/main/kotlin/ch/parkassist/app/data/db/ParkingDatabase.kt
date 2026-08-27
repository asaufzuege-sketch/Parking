package ch.parkassist.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [SessionEntity::class, LogEntity::class], version = 1, exportSchema = false)
abstract class ParkingDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun logDao(): LogDao

    companion object {
        @Volatile private var INSTANCE: ParkingDatabase? = null

        fun getInstance(context: Context): ParkingDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ParkingDatabase::class.java,
                    "parking_db"
                ).build().also { INSTANCE = it }
            }
    }
}
