package ch.parkassist.app.provider

import android.content.Context
import android.content.Intent
import ch.parkassist.app.domain.model.ParkingSession
import ch.parkassist.app.domain.model.Provider

/** What a provider adapter can do. */
data class ProviderCapabilities(
    /** True only for MockParkingAdapter (ch.parkassist.mockparking). */
    val supportsAutomation: Boolean = false,
    /** True for adapters that require manual user action in an external app. */
    val requiresManualHandoff: Boolean = true,
)

/** Result of attempting to launch a provider. */
sealed class LaunchResult {
    data class Success(val intent: Intent) : LaunchResult()
    data class NotAvailable(val reason: String) : LaunchResult()
}

enum class ProviderAction { START, EXTEND, STOP }

/**
 * Generic contract for parking provider adapters.
 *
 * Extension point: to add an official Parkingpay or TWINT SDK integration,
 * implement this interface and replace [ParkingpayManualAdapter] or [TwintManualAdapter]
 * in [ProviderRegistry]. The state machine and Compose screens do NOT need to change.
 */
interface ProviderAdapter {
    val provider: Provider
    val capabilities: ProviderCapabilities

    fun buildStartIntent(context: Context, session: ParkingSession): LaunchResult
    fun buildExtendIntent(context: Context, session: ParkingSession): LaunchResult
    fun buildStopIntent(context: Context, session: ParkingSession): LaunchResult

    /**
     * Human-readable description of what would happen in dry-run mode.
     * Must not include payment data, credentials, or tokens.
     */
    fun dryRunDescription(session: ParkingSession, action: ProviderAction): String

    /**
     * Structured log metadata. Sensitive fields (plate, zone) may be included at a safe
     * granularity; payment data, credentials, and tokens must never be included.
     */
    fun logMetadata(session: ParkingSession): Map<String, String>
}
