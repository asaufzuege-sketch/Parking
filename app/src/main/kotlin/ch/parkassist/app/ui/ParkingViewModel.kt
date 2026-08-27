package ch.parkassist.app.ui

import android.app.Activity
import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ch.parkassist.app.data.db.ParkingDatabase
import ch.parkassist.app.data.db.SessionEntity
import ch.parkassist.app.data.repository.ParkingRepository
import ch.parkassist.app.domain.model.ActivityLogEntry
import ch.parkassist.app.domain.model.ParkingSession
import ch.parkassist.app.domain.model.Provider
import ch.parkassist.app.domain.model.ZonePolicy
import ch.parkassist.app.domain.policy.PolicyResult
import ch.parkassist.app.domain.policy.PolicyValidator
import ch.parkassist.app.domain.state.ManualOutcome
import ch.parkassist.app.domain.state.ParkingEvent
import ch.parkassist.app.domain.state.ParkingState
import ch.parkassist.app.domain.state.ParkingStateMachine
import ch.parkassist.app.domain.state.ParkingStateNames
import ch.parkassist.app.provider.LaunchResult
import ch.parkassist.app.provider.MockAutomationConfig
import ch.parkassist.app.provider.MockParkingAdapter
import ch.parkassist.app.provider.ProviderAction
import ch.parkassist.app.provider.ProviderRegistry
import ch.parkassist.app.provider.requireMockAutomation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

private const val PROVIDER_ERROR_MESSAGE = "Anbieter meldet Fehler"
private const val PROVIDER_UNKNOWN_STATUS_MESSAGE = "Unbekannter Anbieterstatus"
private const val PROVIDER_MANUAL_UNCLEAR_MESSAGE = "Ergebnis unklar – bitte manuell prüfen"
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
    val dryRunMode: Boolean = false,
    val showExperimentalWarning: Boolean = false,
    val pendingManualOutcome: Boolean = false,
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
                        showExperimentalWarning = false,
                        pendingManualOutcome = false,
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
    fun setDryRunMode(enabled: Boolean) = _uiState.update { it.copy(dryRunMode = enabled) }

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
        val newState = ParkingStateMachine.transition(current, ParkingEvent.ExtensionRequired)
        applyStateChange(newState)
    }

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
            _uiState.value.parkingState,
            ParkingEvent.Reset
        )
        applyStateChange(newState)
    }

    fun dismissExperimentalWarning(confirmed: Boolean) {
        val state = _uiState.value
        if (confirmed) {
            _uiState.update { it.copy(showExperimentalWarning = false) }
            return
        }

        val currentState = state.parkingState
        when {
            state.pendingProviderAction == ProviderAction.START -> {
                applyStateChange(ParkingStateMachine.transition(currentState, ParkingEvent.ProviderCancelled))
            }
            state.stateBeforeProviderLaunch != null -> {
                applyStateChange(state.stateBeforeProviderLaunch)
            }
        }
        _uiState.update {
            it.copy(
                showExperimentalWarning = false,
                pendingLaunchIntent = null,
                pendingProviderAction = null,
                stateBeforeProviderLaunch = null,
                pendingManualOutcome = false,
            )
        }
    }

    fun reportManualOutcome(outcome: ManualOutcome) {
        val state = _uiState.value
        val current = state.parkingState
        val session = sessionFromState(current)
        val action = state.pendingProviderAction
        val stateBeforeProviderLaunch = state.stateBeforeProviderLaunch

        if (session == null) {
            _uiState.update {
                it.copy(
                    pendingManualOutcome = false,
                    pendingProviderAction = null,
                    stateBeforeProviderLaunch = null,
                )
            }
            return
        }

        when (action) {
            ProviderAction.START, null -> {
                applyStateChange(ParkingStateMachine.transition(current, ParkingEvent.ManualOutcomeReported(outcome)))
            }
            ProviderAction.EXTEND -> when (outcome) {
                ManualOutcome.CONFIRMED -> {
                    applyStateChange(ParkingStateMachine.transition(current, ParkingEvent.ProviderConfirmed))
                }
                ManualOutcome.NOT_COMPLETED -> {
                    applyStateChange(stateBeforeProviderLaunch ?: ParkingState.Active(session, session.startTime))
                }
                ManualOutcome.UNCLEAR -> {
                    applyStateChange(ParkingState.Error(session, PROVIDER_MANUAL_UNCLEAR_MESSAGE))
                }
            }
            ProviderAction.STOP -> when (outcome) {
                ManualOutcome.CONFIRMED -> {
                    applyStateChange(ParkingState.Completed(session, Instant.now()))
                }
                ManualOutcome.NOT_COMPLETED -> {
                    applyStateChange(stateBeforeProviderLaunch ?: ParkingState.Active(session, session.startTime))
                }
                ManualOutcome.UNCLEAR -> {
                    applyStateChange(ParkingState.Error(session, PROVIDER_MANUAL_UNCLEAR_MESSAGE))
                }
            }
        }

        log(session.id, "ManualOutcome", "${session.provider.name}:${action?.name ?: "START"}:${outcome.name}")
        _uiState.update {
            it.copy(
                pendingManualOutcome = false,
                pendingProviderAction = null,
                stateBeforeProviderLaunch = null,
                pendingLaunchIntent = null,
            )
        }
    }

    fun runMockAutomation(config: MockAutomationConfig) {
        requireMockAutomation(uiState.value.provider)
        log(0, "MockAutomation", "repetitions=${config.repetitions}, intervalSeconds=${config.intervalSeconds}")
        startParking()
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
        val adapter = session?.let { ProviderRegistry.adapterFor(it.provider) }

        if (adapter?.capabilities?.requiresManualHandoff == true) {
            _uiState.update {
                it.copy(
                    pendingManualOutcome = true,
                    pendingLaunchIntent = null,
                    showExperimentalWarning = false,
                )
            }
            log(session.id, "ManualHandoffReturned", "${session.provider.name}:${action?.name ?: "UNKNOWN"}")
            return
        }

        when {
            resultCode == Activity.RESULT_OK && status == MockParkingAdapter.STATUS_CONFIRMED -> {
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
            resultCode == Activity.RESULT_OK && status == MockParkingAdapter.STATUS_DENIED -> {
                if (action == ProviderAction.STOP && stateBeforeProviderLaunch != null) {
                    applyStateChange(stateBeforeProviderLaunch)
                } else {
                    applyStateChange(ParkingStateMachine.transition(state, ParkingEvent.ProviderDenied))
                }
            }
            resultCode == Activity.RESULT_OK && status == MockParkingAdapter.STATUS_ERROR -> {
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

        _uiState.update {
            it.copy(
                pendingProviderAction = null,
                stateBeforeProviderLaunch = null,
                pendingManualOutcome = false,
                showExperimentalWarning = false,
            )
        }
    }

    private fun launchProvider(session: ParkingSession, action: ProviderAction) {
        val context = getApplication<Application>()
        val previousState = _uiState.value.parkingState
        val adapter = ProviderRegistry.adapterFor(session.provider)

        if (_uiState.value.dryRunMode) {
            val providerState = if (action == ProviderAction.START) {
                ParkingStateMachine.transition(_uiState.value.parkingState, ParkingEvent.ProviderLaunched)
            } else {
                ParkingState.AwaitingUser(session)
            }
            applyStateChange(providerState)
            _uiState.update {
                it.copy(
                    pendingProviderAction = action,
                    stateBeforeProviderLaunch = previousState,
                    pendingManualOutcome = true,
                    showExperimentalWarning = false,
                    pendingLaunchIntent = null,
                )
            }
            log(session.id, "DryRun", adapter.dryRunDescription(session, action))
            log(session.id, "ProviderMetadata", adapter.logMetadata(session).entries.joinToString())
            return
        }

        val result = when (action) {
            ProviderAction.START -> adapter.buildStartIntent(context, session)
            ProviderAction.EXTEND -> adapter.buildExtendIntent(context, session)
            ProviderAction.STOP -> adapter.buildStopIntent(context, session)
        }
        when (result) {
            is LaunchResult.Success -> {
                val providerState = if (action == ProviderAction.START) {
                    ParkingStateMachine.transition(_uiState.value.parkingState, ParkingEvent.ProviderLaunched)
                } else {
                    ParkingState.AwaitingUser(session)
                }
                _uiState.update {
                    it.copy(
                        pendingLaunchIntent = result.intent,
                        pendingProviderAction = action,
                        stateBeforeProviderLaunch = previousState,
                        showExperimentalWarning = adapter.capabilities.requiresManualHandoff,
                        pendingManualOutcome = false,
                    )
                }
                applyStateChange(providerState)
                log(session.id, "ProviderLaunched", "${session.provider.name}:${action.name}")
                log(session.id, "ProviderMetadata", adapter.logMetadata(session).entries.joinToString())
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
    val provider = when (this.provider) {
        "EASYPARK" -> Provider.PARKINGPAY
        else -> Provider.entries.firstOrNull { it.name == this.provider } ?: Provider.MOCK
    }
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
