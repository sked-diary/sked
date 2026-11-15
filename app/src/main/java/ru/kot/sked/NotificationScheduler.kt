package ru.kot.sked

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object NotificationScheduler {
    fun schedule(context: Context) {
        if (context.isDemo) return
        val isAuth = context.authPrefs.get<Boolean>("auth") ?: false
        if (!isAuth) return

        val intent = Intent(context, UpdateReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intervalMs = 20 * 60 * 1000L // every 20 minutes
        val triggerAt = System.currentTimeMillis() + intervalMs

        try {
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                intervalMs,
                pendingIntent
            )
            AppLog.d("NotificationScheduler", "Alarm scheduled for background checks every 20 min")
        } catch (e: Exception) {
            AppLog.e("NotificationScheduler", "Failed to schedule alarm", e)
        }
    }
}
