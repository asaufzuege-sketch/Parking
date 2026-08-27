package ch.parkassist.app.worker

import android.content.Context
import android.content.Intent
import androidx.work.*
import ch.parkassist.app.service.ParkingForegroundService
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker that triggers the parking foreground service at the scheduled time.
 * Persists across process death and device restart.
 */
class ParkingWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    companion object {
        const val TAG = "ParkingWorker"
        const val KEY_STATUS_TEXT = "statusText"

        fun buildRequest(delayMinutes: Long, statusText: String): OneTimeWorkRequest =
            OneTimeWorkRequestBuilder<ParkingWorker>()
                .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                .setInputData(workDataOf(KEY_STATUS_TEXT to statusText))
                .addTag(TAG)
                .setConstraints(Constraints.Builder().build())
                .build()
    }

    override fun doWork(): Result {
        val statusText = inputData.getString(KEY_STATUS_TEXT) ?: "Parkvorgang aktiv"
        val serviceIntent = Intent(applicationContext, ParkingForegroundService::class.java).apply {
            action = ParkingForegroundService.ACTION_START
            putExtra(ParkingForegroundService.EXTRA_STATUS_TEXT, statusText)
        }
        applicationContext.startForegroundService(serviceIntent)
        return Result.success()
    }
}
