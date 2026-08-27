package ch.parkassist.app.service

import android.app.*
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import ch.parkassist.app.R
import ch.parkassist.app.ui.MainActivity

class ParkingForegroundService : Service() {

    companion object {
        const val ACTION_START = "ch.parkassist.app.ACTION_START_SERVICE"
        const val ACTION_STOP = "ch.parkassist.app.ACTION_STOP_SERVICE"
        const val EXTRA_STATUS_TEXT = "statusText"
        const val NOTIF_ID = 1001
        const val CHANNEL_ID = "parking_session"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            else -> {
                val text = intent?.getStringExtra(EXTRA_STATUS_TEXT) ?: getString(R.string.status_active)
                startForeground(NOTIF_ID, buildNotification(text))
            }
        }
        return START_STICKY
    }

    private fun buildNotification(contentText: String): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, ParkingForegroundService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notif_title_active))
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_menu_directions)
            .setContentIntent(openIntent)
            .addAction(android.R.drawable.ic_delete, getString(R.string.notif_action_stop), stopIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
