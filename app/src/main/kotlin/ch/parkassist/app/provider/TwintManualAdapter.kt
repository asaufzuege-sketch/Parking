package ch.parkassist.app.provider

import android.content.Context
import android.content.Intent
import android.net.Uri
import ch.parkassist.app.domain.model.ParkingSession
import ch.parkassist.app.domain.model.Provider

/**
 * TWINT Parking manual adapter. Opens the official TWINT app via a resolvable deep link.
 * No Accessibility service, no undocumented APIs.
 * After returning, the user must explicitly confirm the outcome.
 *
 * Extension point: replace this class with an official TwintOfficialAdapter that
 * implements [ProviderAdapter] using the TWINT AppSwitch API and a secure backend.
 */
object TwintManualAdapter : ProviderAdapter {
    override val provider = Provider.TWINT
    override val capabilities = ProviderCapabilities(
        supportsAutomation = false,
        requiresManualHandoff = true,
    )

    private const val TWINT_PACKAGE = "ch.twint.payment"
    private const val TWINT_DEEPLINK = "twint://parking"
    private const val TWINT_HTTPS_FALLBACK = "https://www.twint.ch/consumer/"

    override fun buildStartIntent(context: Context, session: ParkingSession) = buildHandoffIntent(context)
    override fun buildExtendIntent(context: Context, session: ParkingSession) = buildHandoffIntent(context)
    override fun buildStopIntent(context: Context, session: ParkingSession) = buildHandoffIntent(context)

    private fun buildHandoffIntent(context: Context): LaunchResult {
        val nativeIntent = Intent(Intent.ACTION_VIEW, Uri.parse(TWINT_DEEPLINK)).apply {
            setPackage(TWINT_PACKAGE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (context.packageManager.resolveActivity(nativeIntent, 0) != null) {
            return LaunchResult.Success(nativeIntent)
        }
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(TWINT_HTTPS_FALLBACK)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (context.packageManager.resolveActivity(browserIntent, 0) != null) {
            return LaunchResult.Success(browserIntent)
        }
        return LaunchResult.NotAvailable(
            "TWINT nicht installiert. Bitte TWINT manuell öffnen und Parkvorgang starten."
        )
    }

    override fun dryRunDescription(session: ParkingSession, action: ProviderAction): String =
        "[DRY-RUN] TWINT Parking (manuell) – Aktion: ${action.name}, Zone: ${session.zone}, " +
            "Dauer: ${session.ticketDurationMinutes} Min. Kein externer Aufruf. " +
            "Ergebnis muss manuell bestätigt werden."

    override fun logMetadata(session: ParkingSession): Map<String, String> = mapOf(
        "provider" to provider.name,
        "zone" to session.zone,
        "durationMinutes" to session.ticketDurationMinutes.toString(),
    )
}
