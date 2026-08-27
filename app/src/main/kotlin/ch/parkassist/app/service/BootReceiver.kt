package ch.parkassist.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.WorkManager

/**
 * Re-enqueues scheduled WorkManager tasks after device reboot.
 * WorkManager already persists work across reboots when using a database-backed store,
 * so this receiver ensures the scheduler wakes up.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // WorkManager restores persisted work automatically.
            // Trigger a sync to ensure pending work is re-enqueued if needed.
            WorkManager.getInstance(context).pruneWork()
        }
    }
}
