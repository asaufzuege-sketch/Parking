package ch.parkassist.app.provider

import android.content.Context
import android.content.Intent
import android.net.Uri
import ch.parkassist.app.domain.model.ParkingSession
import ch.parkassist.app.domain.model.Provider

/**
 * Parkingpay manual adapter. Opens the official Parkingpay app via a resolvable deep link.
 * No Accessibility service, no undocumented APIs, no credential handling.
 * After returning, the user must explicitly confirm the outcome.
 *
 * Extension point: replace this class with an official ParkingpayOfficialAdapter that
 * implements [ProviderAdapter] using the Digitalparking partner API.
 */
object ParkingpayManualAdapter : ProviderAdapter {
    override val provider = Provider.PARKINGPAY
    override val capabilities = ProviderCapabilities(
        supportsAutomation = false,
        requiresManualHandoff = true,
    )

    private const val PARKINGPAY_PACKAGE = "ch.digitalparking.parkingpay"
    private const val PARKINGPAY_DEEPLINK = "https://www.parkingpay.ch/parking"

    override fun buildStartIntent(context: Context, session: ParkingSession) = buildHandoffIntent(context)
    override fun buildExtendIntent(context: Context, session: ParkingSession) = buildHandoffIntent(context)
    override fun buildStopIntent(context: Context, session: ParkingSession) = buildHandoffIntent(context)

    private fun buildHandoffIntent(context: Context): LaunchResult {
        val nativeIntent = Intent(Intent.ACTION_VIEW, Uri.parse(PARKINGPAY_DEEPLINK)).apply {
            setPackage(PARKINGPAY_PACKAGE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (context.packageManager.resolveActivity(nativeIntent, 0) != null) {
            return LaunchResult.Success(nativeIntent)
        }
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(PARKINGPAY_DEEPLINK)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (context.packageManager.resolveActivity(browserIntent, 0) != null) {
            return LaunchResult.Success(browserIntent)
        }
        return LaunchResult.NotAvailable(
            "Parkingpay nicht erreichbar. Bitte Parkingpay manuell öffnen und Parkvorgang starten."
        )
    }

    override fun dryRunDescription(session: ParkingSession, action: ProviderAction): String =
        "[DRY-RUN] Parkingpay (manuell) – Aktion: ${action.name}, Zone: ${session.zone}, " +
            "Dauer: ${session.ticketDurationMinutes} Min. Kein externer Aufruf. " +
            "Ergebnis muss manuell bestätigt werden."

    override fun logMetadata(session: ParkingSession): Map<String, String> = mapOf(
        "provider" to provider.name,
        "zone" to session.zone,
        "durationMinutes" to session.ticketDurationMinutes.toString(),
    )
}
