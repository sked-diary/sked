package ru.kot.sked

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import ru.kot.sked.network.gosuslugi.GMark
import ru.kot.sked.network.gosuslugi.GosuslugiDataBridge
import kotlin.concurrent.thread

private const val TAG = "Notifications"

class UpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val isAuth = context.authPrefs.get<Boolean>("auth") ?: false
        if (!isAuth || context.isDemo) return

        val isGosuslugi = context.authPrefs.get<String>("cookie") != null
        if (isGosuslugi) {
            checkGosuslugiMarks(context)
        }
    }

    private fun checkGosuslugiMarks(context: Context) {
        thread {
            try {
                AppLog.d(TAG, "Checking for new Gosuslugi marks in background...")

                // Load cached known mark IDs
                val cachedKnownIdsJson = context.notificationPrefs.get<String>("gosuslugi_known_mark_ids")
                val knownIds: MutableSet<String> = if (!cachedKnownIdsJson.isNullOrEmpty()) {
                    Gson().fromJson(cachedKnownIdsJson, object : TypeToken<MutableSet<String>>() {}.type)
                } else {
                    mutableSetOf()
                }

                // If knownIds is completely empty (first time run), seed it from current cached marks
                if (knownIds.isEmpty()) {
                    val currentCacheJson = context.cachePrefs.get<String>("gosuslugi_marks")
                    if (!currentCacheJson.isNullOrEmpty()) {
                        val currentMarks: List<GMark> = Gson().fromJson(
                            currentCacheJson,
                            object : TypeToken<List<GMark>>() {}.type
                        ) ?: emptyList()
                        currentMarks.forEach { m -> m.id?.let { knownIds.add(it) } }
                        context.notificationPrefs.save("gosuslugi_known_mark_ids" to Gson().toJson(knownIds))
                    }
                }

                // Query Gosuslugi DataBridge
                GosuslugiDataBridge.fetchAndApply(context) { success ->
                    if (success) {
                        try {
                            val newCacheJson = context.cachePrefs.get<String>("gosuslugi_marks")
                            if (!newCacheJson.isNullOrEmpty()) {
                                val freshMarks: List<GMark> = Gson().fromJson(
                                    newCacheJson,
                                    object : TypeToken<List<GMark>>() {}.type
                                ) ?: emptyList()

                                var newCount = 0
                                freshMarks.forEach { mark ->
                                    val markId = mark.id
                                    if (markId != null && !knownIds.contains(markId)) {
                                        knownIds.add(markId)
                                        newCount++
                                        val valStr = mark.value ?: "5"
                                        val weight = mark.weight?.toIntOrNull() ?: 1
                                        val subName = mark.subjectName
                                        val workDesc = mark.workType ?: mark.workKind ?: "Работа"

                                        AppLog.d(TAG, "New mark detected: $subName - $valStr (weight=$weight)")
                                        context.sendMarkNotification(valStr, weight, subName, workDesc)
                                    }
                                }

                                if (newCount > 0) {
                                    context.notificationPrefs.save("gosuslugi_known_mark_ids" to Gson().toJson(knownIds))
                                    AppLog.d(TAG, "Sent notifications for $newCount new marks.")
                                } else {
                                    AppLog.d(TAG, "No new marks found.")
                                }
                            }
                        } catch (e: Exception) {
                            AppLog.e(TAG, "Error comparing fresh marks", e)
                        }
                    } else {
                        AppLog.d(TAG, "Gosuslugi background check did not return success")
                    }
                }
            } catch (e: Exception) {
                AppLog.e(TAG, "Error during background notification check", e)
            }
        }
    }

    private fun Context.sendMarkNotification(
        value: String,
        weight: Int,
        subjectName: String,
        controlFormName: String
    ) {
        val totalCount = notificationPrefs.get<Int>("total_count") ?: 0
        val suffix = if (weight > 1) " (вес $weight)" else ""
        val hideValue = notificationPrefs.get<Boolean>("_hide_mark_value") == true

        val title = if (hideValue) subjectName else "$subjectName • $value$suffix"
        val text = controlFormName

        val notification: Notification = Notification.Builder(this, "data_update")
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setSmallIcon(R.drawable.ic_round_menu_book_24)
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    1,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            .build()

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val nextId = totalCount + 1
        notificationPrefs.save("total_count" to nextId)
        NotificationManagerCompat.from(this).notify(nextId, notification)
    }
}
