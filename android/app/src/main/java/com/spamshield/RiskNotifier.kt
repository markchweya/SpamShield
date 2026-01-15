package com.spamshield

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

object RiskNotifier {
    private const val CH = "spamshield_risk"

    fun notify(context: Context, senderDisplay: String, score: Int, reason: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(CH, "SpamShield Alerts", NotificationManager.IMPORTANCE_HIGH)
            )
        }

        val n = NotificationCompat.Builder(context, CH)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("Possible scam/spam ($score)")
            .setContentText("$senderDisplay  $reason")
            .setStyle(NotificationCompat.BigTextStyle().bigText("$senderDisplay\n$reason\nScore: $score"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        nm.notify((senderDisplay.hashCode() and 0x7fffffff), n)
    }
}
