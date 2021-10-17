package me.chitholian.sipdialer

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class CallService : Service() {
    private lateinit var app: TheApp

    override fun onCreate() {
        super.onCreate()
        app = application as TheApp
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val callIntent = Intent(this, CallActivity::class.java)
        callIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        val pIntent = PendingIntent.getActivity(this, 111, callIntent, 0)
        val notification = NotificationCompat.Builder(this, "OngoingCall")
            .setSmallIcon(R.drawable.ic_dialer_sip)
            .setContentTitle("Ongoing Call")
            .setContentText("A call is ongoing")
            .setContentIntent(pIntent)
        startForeground(111, notification.build())
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "OngoingCall",
                "Active Call Notification",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.description = "Show notification for ongoing call."
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}
