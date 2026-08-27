package ch.parkassist.app.ui

import android.app.Activity
import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ch.parkassist.app.data.db.ParkingDatabase
import ch.parkassist.app.data.db.SessionEntity
import ch.parkassist.app.data.repository.ParkingRepository
import ch.parkassist.app.domain.model.*
import ch.parkassist.app.domain.policy.PolicyResult
import ch.parkassist.app.domain.policy.PolicyValidator
import ch.parkassist.app.domain.state.*
import ch.parkassist.app.provider.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant

enum class ProviderAction {
    START,
    EXTEND,
    STOP,
}

private const val PROVIDER_ERROR_MESSAGE = "Anbieter meldet Fehler"
private const val PROVIDER_UNKNOWN_STATUS_MESSAGE = "Unbekannter Anbieterstatus"
private const val RESTORE_INCOMPLETE_PROVIDER_STATE_MESSAGE =
    "Vorheriger Anbieterzustand konnte nicht abgeschlossen werden"

data class ParkingUiState(
    val parkingState: ParkingState = ParkingState.Idle,
    val provider: Provider = Provider.MOCK,
    val zone: String = "",
    val licensePlate: String = "",
    val ticketDurationMinutes: String = "60",
    val maxExtensions: String = "0",
    val startNow: Boolean = true,
    val scheduledTime: Instant = Instant.now(),
    val userConfirmed: Boolean = false,
    val validationError: String? = null,
    val pendingLaunchIntent: Intent? = null,
    val pendingProviderAction: ProviderAction? = null,
    val stateBeforeProviderLaunch: ParkingState? = null,
    val log: List<ActivityLogEntry> = emptyList(),
)

class ParkingViewModel(application: Application) : AndroidViewModel(application) {

    private val db = ParkingDatabase.getInstance(application)
    private val repository = ParkingRepository(db.sessionDao(), db.logDao())

    private val _uiState = MutableStateFlow(ParkingUiState())
    val uiState: StateFlow<ParkingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeLog().collect { entries ->
                _uiState.update { it.copy(log = entries) }
            }
        }
        viewModelScope.launch {
            repository.getLatestRestorableSession()?.let { entity ->
                val restoredSession = entity.toParkingSession()
                _uiState.update {
                    it.copy(
                        parkingState = entity.toRestoredParkingState(restoredSession),
                        provider = restoredSession.provider,
                        zone = restoredSession.zone,
                        licensePlate = restoredSession.licensePlate,
                        ticketDurationMinutes = restoredSession.ticketDurationMinutes.toString(),
                        maxExtensions = restoredSession.maxExtensions.toString(),
                        userConfirmed = restoredSession.confirmedByUser,
                        validationError = null,
                        pendingLaunchIntent = null,
                        pendingProviderAction = null,
                        stateBeforeProviderLaunch = null,
                    )
                }
            }
        }
    }

    fun setProvider(provider: Provider) = _uiState.update { it.copy(provider = provider) }
    fun setZone(zone: String) = _uiState.update { it.copy(zone = zone) }
    fun setLicensePlate(plate: String) = _uiState.update { it.copy(licensePlate = plate) }
    fun setTicketDuration(v: String) = _uiState.update { it.copy(ticketDurationMinutes = v) }
    fun setMaxExtensions(v: String) = _uiState.update { it.copy(maxExtensions = v) }
    fun setStartNow(v: Boolean) = _uiState.update { it.copy(startNow = v) }
    fun setScheduledTime(v: Instant) = _uiState.update { it.copy(scheduledTime = v) }
    fun setUserConfirmed(v: Boolean) = _uiState.update { it.copy(userConfirmed = v) }

    fun startParking(policy: ZonePolicy = defaultPolicy()) {
        val state = _uiState.value
        val duration = state.ticketDurationMinutes.toIntOrNull()
        val extensions = state.maxExtensions.toIntOrNull()

        if (duration == null || duration <= 0) {
            _uiState.update { it.copy(validationError = "Ungültige Ticketdauer") }
            return
        }
        if (extensions == null || extensions < 0) {
            _uiState.update { it.copy(validationError = "Ungültige Verlängerungsanzahl") }
            return
        }
        if (state.zone.isBlank()) {
            _uiState.update { it.copy(validationError = "Bitte Parkzone eingeben") }
            return
        }
        if (state.licensePlate.isBlank()) {
            _uiState.update { it.copy(validationError = "Bitte Kontrollschild eingeben") }
            return
        }
        if (!state.userConfirmed) {
            _uiState.update { it.copy(validationError = "Bitte Bestätigung ankreuzen") }
            return
        }

        val session = ParkingSession(
            provider = state.provider,
            zone = state.zone,
            licensePlate = state.licensePlate,
            ticketDurationMinutes = duration,
            maxExtensions = extensions,
            startTime = if (state.startNow) Instant.now() else state.scheduledTime,
            confirmedByUser = state.userConfirmed,
        )

        val validationResult = PolicyValidator.validateStart(session, policy)
        if (validationResult is PolicyResult.Rejected) {
            _uiState.update { it.copy(validationError = validationResult.reason) }
            return
        }

        _uiState.update { it.copy(validationError = null) }

        val newState = ParkingStateMachine.transition(
            ParkingState.Idle,
            ParkingEvent.Start(session)
        )
        applyStateChange(newState)

        if (newState is ParkingState.LaunchingProvider) {
            launchProvider(session, ProviderAction.START)
        }
    }

    /**
     * Transitions to [ParkingState.ExtensionDue] after validating the policy.
     * The user must then call [confirmExtension] to launch the provider and extend.
     */
    fun requestExtension(policy: ZonePolicy = defaultPolicy()) {
        val current = _uiState.value.parkingState
        val session = when (current) {
            is ParkingState.Active -> current.session
            is ParkingState.ExtensionDue -> current.session
            else -> return
        }
        val elapsed = session.ticketDurationMinutes * (1 + session.extensionsUsed)
        val result = PolicyValidator.validateExtension(session, policy, elapsed)
        if (result is PolicyResult.Rejected) {
            applyStateChange(ParkingState.Error(session, result.reason))
            return
        }
        // Move to ExtensionDue and wait for explicit user confirmation
        val newState = ParkingStateMachine.transition(current, ParkingEvent.ExtensionRequired)
        applyStateChange(newState)
    }

    /**
     * Called by the user to explicitly confirm the extension shown in [ParkingState.ExtensionDue].
     * Launches the provider and waits for result to transition back to [ParkingState.Active].
     */
    fun confirmExtension() {
        val current = _uiState.value.parkingState
        if (current !is ParkingState.ExtensionDue) return
        launchProvider(current.session.copy(extensionsUsed = current.session.extensionsUsed + 1), ProviderAction.EXTEND)
    }

    fun stopParking() {
        val current = _uiState.value.parkingState
        val session = sessionFromState(current)
        if (session != null && (current is ParkingState.Active || current is ParkingState.ExtensionDue)) {
            launchProvider(session, ProviderAction.STOP)
            return
        }
        val newState = ParkingStateMachine.transition(current, ParkingEvent.StopRequested)
        applyStateChange(newState)
    }

    fun resetError() {
        val newState = ParkingStateMachine.transition(
            _uiState.value.parkingState, ParkingEvent.Reset
        )
        applyStateChange(newState)
    }

    fun clearLog() {
        viewModelScope.launch { repository.clearLog() }
    }

    fun clearPendingIntent() = _uiState.update { it.copy(pendingLaunchIntent = null) }

    fun handleProviderResult(resultCode: Int, status: String?) {
        val state = _uiState.value.parkingState
        val session = sessionFromState(state)
        val action = _uiState.value.pendingProviderAction
        val stateBeforeProviderLaunch = _uiState.value.stateBeforeProviderLaunch

        when {
            resultCode == Activity.RESULT_OK && status == MockProviderAdapter.STATUS_CONFIRMED -> {
                when (action) {
                    ProviderAction.STOP -> {
                        if (session != null) {
                            applyStateChange(ParkingState.Completed(session, Instant.now()))
                        } else {
                            _uiState.update { it.copy(parkingState = ParkingState.Idle) }
                        }
                    }
                    else -> {
                        applyStateChange(ParkingStateMachine.transition(state, ParkingEvent.ProviderConfirmed))
                    }
                }
            }
            resultCode == Activity.RESULT_OK && status == MockProviderAdapter.STATUS_DENIED -> {
                if (action == ProviderAction.STOP && stateBeforeProviderLaunch != null) {
                    applyStateChange(stateBeforeProviderLaunch)
                } else {
                    applyStateChange(ParkingStateMachine.transition(state, ParkingEvent.ProviderDenied))
                }
            }
            resultCode == Activity.RESULT_OK && status == MockProviderAdapter.STATUS_ERROR -> {
                applyStateChange(
                    ParkingStateMachine.transition(
                        state,
                        ParkingEvent.ErrorOccurred(PROVIDER_ERROR_MESSAGE)
                    )
                )
            }
            resultCode == Activity.RESULT_CANCELED -> {
                if (action == ProviderAction.STOP && stateBeforeProviderLaunch != null) {
                    applyStateChange(stateBeforeProviderLaunch)
                } else {
                    applyStateChange(ParkingStateMachine.transition(state, ParkingEvent.ProviderCancelled))
                }
            }
            else -> {
                applyStateChange(
                    ParkingStateMachine.transition(
                        state,
                        ParkingEvent.ErrorOccurred(PROVIDER_UNKNOWN_STATUS_MESSAGE)
                    )
                )
            }
        }

        _uiState.update { it.copy(pendingProviderAction = null, stateBeforeProviderLaunch = null) }
    }

    private fun launchProvider(session: ParkingSession, action: ProviderAction) {
        val context = getApplication<Application>()
        val previousState = _uiState.value.parkingState
        val adapter = ProviderRegistry.adapterFor(session.provider)
        val result = when (action) {
            ProviderAction.START -> adapter.buildStartIntent(context, session)
            ProviderAction.EXTEND -> adapter.buildExtendIntent(context, session)
            ProviderAction.STOP -> adapter.buildStopIntent(context, session)
        }
        when (result) {
            is LaunchResult.Success -> {
                _uiState.update {
                    it.copy(
                        pendingLaunchIntent = result.intent,
                        pendingProviderAction = action,
                        stateBeforeProviderLaunch = previousState,
                    )
                }
                val providerState = if (action == ProviderAction.START) {
                    ParkingStateMachine.transition(_uiState.value.parkingState, ParkingEvent.ProviderLaunched)
                } else {
                    ParkingState.AwaitingUser(session)
                }
                applyStateChange(providerState)
                log(session.id, "ProviderLaunched", "${session.provider.name}:${action.name}")
            }
            is LaunchResult.NotAvailable -> {
                applyStateChange(ParkingState.Error(session, result.reason))
                log(session.id, "ProviderNotAvailable", result.reason)
            }
        }
    }

    private fun applyStateChange(newState: ParkingState) {
        _uiState.update { it.copy(parkingState = newState) }
        val session = sessionFromState(newState)
        if (session != null) {
            viewModelScope.launch {
                val savedId = repository.saveSession(session, newState::class.simpleName ?: "")
                if (session.id == 0L) {
                    val updated = session.copy(id = savedId)
                    _uiState.update {
                        it.copy(parkingState = newState.withSession(updated))
                    }
                }
                log(session.id, newState::class.simpleName ?: "StateChange")
            }
        }
    }

    private fun log(sessionId: Long, event: String, detail: String = "") {
        viewModelScope.launch { repository.addLog(sessionId, event, detail) }
    }

    private fun sessionFromState(state: ParkingState): ParkingSession? = when (state) {
        is ParkingState.Idle -> null
        is ParkingState.Scheduled -> state.session
        is ParkingState.LaunchingProvider -> state.session
        is ParkingState.AwaitingUser -> state.session
        is ParkingState.Active -> state.session
        is ParkingState.ExtensionDue -> state.session
        is ParkingState.Completed -> state.session
        is ParkingState.Cancelled -> state.session
        is ParkingState.Error -> state.session
    }

    /** Replace the session reference inside a state (used after DB assigns id). */
    private fun ParkingState.withSession(s: ParkingSession): ParkingState = when (this) {
        is ParkingState.Idle -> this
        is ParkingState.Scheduled -> copy(session = s)
        is ParkingState.LaunchingProvider -> copy(session = s)
        is ParkingState.AwaitingUser -> copy(session = s)
        is ParkingState.Active -> copy(session = s)
        is ParkingState.ExtensionDue -> copy(session = s)
        is ParkingState.Completed -> copy(session = s)
        is ParkingState.Cancelled -> copy(session = s)
        is ParkingState.Error -> copy(session = s)
    }

    private fun defaultPolicy() = ZonePolicy(
        zoneId = _uiState.value.zone.ifBlank { "DEFAULT" },
        maxTotalMinutes = null,
        extensionAllowed = true,
        confirmationRequired = true,
    )
}

internal fun SessionEntity.toParkingSession(): ParkingSession {
    val provider = Provider.entries.firstOrNull { it.name == this.provider } ?: Provider.MOCK
    return ParkingSession(
        id = id,
        provider = provider,
        zone = zone,
        licensePlate = licensePlate,
        ticketDurationMinutes = ticketDurationMinutes,
        maxExtensions = maxExtensions,
        extensionsUsed = extensionsUsed,
        startTime = Instant.ofEpochMilli(startTimeEpoch),
        confirmedByUser = confirmedByUser,
    )
}

internal fun SessionEntity.toRestoredParkingState(session: ParkingSession): ParkingState = when (state) {
    ParkingStateNames.SCHEDULED -> ParkingState.Scheduled(session, session.startTime)
    ParkingStateNames.LAUNCHING_PROVIDER,
    ParkingStateNames.AWAITING_USER -> ParkingState.AwaitingUser(session)
    ParkingStateNames.ACTIVE -> ParkingState.Active(
        session,
        session.startTime.plusSeconds((session.ticketDurationMinutes * (1 + session.extensionsUsed) * 60L))
    )
    ParkingStateNames.EXTENSION_DUE -> ParkingState.ExtensionDue(
        session,
        session.startTime.plusSeconds((session.ticketDurationMinutes * (1 + session.extensionsUsed) * 60L))
    )
    ParkingStateNames.ERROR -> ParkingState.Error(session, RESTORE_INCOMPLETE_PROVIDER_STATE_MESSAGE)
    else -> ParkingState.Idle
}
