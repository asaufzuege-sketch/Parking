package ch.parkassist.app.data.repository

import ch.parkassist.app.data.db.*
import ch.parkassist.app.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

class ParkingRepository(
    private val sessionDao: SessionDao,
    private val logDao: LogDao,
) {

    fun observeLatestSession(): Flow<SessionEntity?> = sessionDao.observeLatest()

    suspend fun getLatestRestorableSession(): SessionEntity? = sessionDao.getLatestRestorable()

    fun observeLog(): Flow<List<ActivityLogEntry>> =
        logDao.observeAll().map { list ->
            list.map { e ->
                ActivityLogEntry(
                    id = e.id,
                    sessionId = e.sessionId,
                    timestamp = Instant.ofEpochMilli(e.timestampEpoch),
                    event = e.event,
                    detail = e.detail,
                )
            }
        }

    suspend fun saveSession(session: ParkingSession, state: String): Long {
        val entity = SessionEntity(
            id = session.id,
            provider = session.provider.name,
            zone = session.zone,
            licensePlate = session.licensePlate,
            ticketDurationMinutes = session.ticketDurationMinutes,
            maxExtensions = session.maxExtensions,
            extensionsUsed = session.extensionsUsed,
            startTimeEpoch = session.startTime.toEpochMilli(),
            confirmedByUser = session.confirmedByUser,
            state = state,
        )
        return sessionDao.upsert(entity)
    }

    suspend fun updateSessionState(id: Long, state: String, extensionsUsed: Int) {
        sessionDao.updateStateAndExtensions(id, state, extensionsUsed)
    }

    suspend fun addLog(sessionId: Long, event: String, detail: String = "") {
        logDao.insert(
            LogEntity(
                sessionId = sessionId,
                timestampEpoch = System.currentTimeMillis(),
                event = event,
                detail = detail,
            )
        )
    }

    suspend fun clearLog() = logDao.clear()
}
