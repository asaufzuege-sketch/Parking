package ch.parkassist.app.provider

import android.content.Context
import android.content.Intent
import ch.parkassist.app.domain.model.ParkingSession
import ch.parkassist.app.domain.model.Provider

/** Package of the local mock provider app. Kept for consumers that reference it by name. */
const val MOCK_PACKAGE = MockProviderContract.PACKAGE

object MockProviderAdapter : ProviderAdapter {

    override val provider = Provider.MOCK

    override fun buildStartIntent(context: Context, session: ParkingSession): LaunchResult =
        buildIntent(context, MockProviderContract.ACTION_START, session)

    override fun buildExtendIntent(context: Context, session: ParkingSession): LaunchResult =
        buildIntent(context, MockProviderContract.ACTION_EXTEND, session)

    override fun buildStopIntent(context: Context, session: ParkingSession): LaunchResult =
        buildIntent(context, MockProviderContract.ACTION_STOP, session)

    private fun buildIntent(context: Context, action: String, session: ParkingSession): LaunchResult {
        val intent = Intent(action).apply {
            setPackage(MockProviderContract.PACKAGE)
            putExtra(MockProviderContract.EXTRA_ZONE, session.zone)
            putExtra(MockProviderContract.EXTRA_PLATE, session.licensePlate)
            putExtra(MockProviderContract.EXTRA_DURATION_MINUTES, session.ticketDurationMinutes)
            putExtra(MockProviderContract.EXTRA_SESSION_ID, session.id)
        }
        val resolvable = context.packageManager.resolveActivity(intent, 0) != null
        return if (resolvable) LaunchResult.Success(intent)
        else LaunchResult.NotAvailable("Mock Parking App nicht installiert (Paket: ${MockProviderContract.PACKAGE})")
    }

    // Kept for backward compat with tests
    const val EXTRA_ZONE = MockProviderContract.EXTRA_ZONE
    const val EXTRA_PLATE = MockProviderContract.EXTRA_PLATE
    const val EXTRA_DURATION_MINUTES = MockProviderContract.EXTRA_DURATION_MINUTES
    const val EXTRA_SESSION_ID = MockProviderContract.EXTRA_SESSION_ID
    const val RESULT_STATUS = MockProviderContract.RESULT_STATUS
    const val STATUS_CONFIRMED = MockProviderContract.STATUS_CONFIRMED
    const val STATUS_DENIED = MockProviderContract.STATUS_DENIED
    const val STATUS_ERROR = MockProviderContract.STATUS_ERROR
}
