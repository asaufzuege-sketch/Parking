package ch.parkassist.app.provider

import android.content.Context
import android.content.Intent
import android.net.Uri
import ch.parkassist.app.domain.model.ParkingSession
import ch.parkassist.app.domain.model.Provider

/**
 * EasyPark adapter. Attempts to launch the official EasyPark app via a resolvable deep link.
 * No Accessibility service, no undocumented APIs, no credential handling.
 * If the app is not installed, the result is NotAvailable and the user must continue manually.
 */
object EasyParkAdapter : ProviderAdapter {

    override val provider = Provider.EASYPARK

    private const val EASYPARK_PACKAGE = "net.easypark"
    private const val EASYPARK_DEEPLINK = "easypark://parking"

    override fun buildStartIntent(context: Context, session: ParkingSession): LaunchResult =
        buildDeepLink(context)

    override fun buildExtendIntent(context: Context, session: ParkingSession): LaunchResult =
        buildDeepLink(context)

    override fun buildStopIntent(context: Context, session: ParkingSession): LaunchResult =
        buildDeepLink(context)

    private fun buildDeepLink(context: Context): LaunchResult {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(EASYPARK_DEEPLINK)).apply {
            setPackage(EASYPARK_PACKAGE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val resolvable = context.packageManager.resolveActivity(intent, 0) != null
        return if (resolvable) LaunchResult.Success(intent)
        else LaunchResult.NotAvailable(
            "EasyPark nicht installiert. Bitte EasyPark manuell öffnen und Parkvorgang starten."
        )
    }
}
