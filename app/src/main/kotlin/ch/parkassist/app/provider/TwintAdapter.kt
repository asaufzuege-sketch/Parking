package ch.parkassist.app.provider

import android.content.Context
import android.content.Intent
import android.net.Uri
import ch.parkassist.app.domain.model.ParkingSession
import ch.parkassist.app.domain.model.Provider

/**
 * TWINT Parking adapter. Attempts to launch the official TWINT app via a resolvable deep link.
 * No Accessibility service, no undocumented APIs.
 */
object TwintAdapter : ProviderAdapter {

    override val provider = Provider.TWINT

    private const val TWINT_PACKAGE = "ch.twint.payment"
    private const val TWINT_DEEPLINK = "twint://parking"

    override fun buildStartIntent(context: Context, session: ParkingSession): LaunchResult =
        buildDeepLink(context)

    override fun buildExtendIntent(context: Context, session: ParkingSession): LaunchResult =
        buildDeepLink(context)

    override fun buildStopIntent(context: Context, session: ParkingSession): LaunchResult =
        buildDeepLink(context)

    private fun buildDeepLink(context: Context): LaunchResult {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(TWINT_DEEPLINK)).apply {
            setPackage(TWINT_PACKAGE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val resolvable = context.packageManager.resolveActivity(intent, 0) != null
        return if (resolvable) LaunchResult.Success(intent)
        else LaunchResult.NotAvailable(
            "TWINT nicht installiert. Bitte TWINT manuell öffnen und Parkvorgang starten."
        )
    }
}
