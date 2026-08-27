package ch.parkassist.app.provider

import android.content.Context
import android.content.Intent
import ch.parkassist.app.domain.model.ParkingSession
import ch.parkassist.app.domain.model.Provider

/** Result of attempting to launch a provider. */
sealed class LaunchResult {
    data class Success(val intent: Intent) : LaunchResult()
    data class NotAvailable(val reason: String) : LaunchResult()
}

/** Adapter that builds launch Intents for a parking provider. */
interface ProviderAdapter {
    val provider: Provider
    fun buildStartIntent(context: Context, session: ParkingSession): LaunchResult
    fun buildExtendIntent(context: Context, session: ParkingSession): LaunchResult
    fun buildStopIntent(context: Context, session: ParkingSession): LaunchResult
}
