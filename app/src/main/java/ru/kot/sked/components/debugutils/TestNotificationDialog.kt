package ru.kot.sked.components.debugutils

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import ru.kot.sked.MainActivity
import ru.kot.sked.R
import ru.kot.sked.get
import ru.kot.sked.notificationPrefs
import ru.kot.sked.save

@Composable
fun TestNotificationDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var subject by remember { mutableStateOf("Алгебра") }
    var markValue by remember { mutableStateOf("5") }
    var weight by remember { mutableStateOf("2") }
    var workType by remember { mutableStateOf("Контрольная работа") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    "Тест уведомления об оценке",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Предмет") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = markValue,
                        onValueChange = { markValue = it },
                        label = { Text("Оценка") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { weight = it },
                        label = { Text("Вес") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = workType,
                    onValueChange = { workType = it },
                    label = { Text("Вид работы") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = {
                        val w = weight.toIntOrNull() ?: 1
                        sendTestNotification(context, markValue, w, subject, workType)
                        Toast.makeText(context, "Уведомление отправлено в шторку!", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Отправить уведомление")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Закрыть")
                }
            }
        }
    }
}

private fun sendTestNotification(
    context: Context,
    value: String,
    weight: Int,
    subjectName: String,
    workType: String
) {
    val totalCount = context.notificationPrefs.get<Int>("total_count") ?: 0
    val suffix = if (weight > 1) " (вес $weight)" else ""
    val hideValue = context.notificationPrefs.get<Boolean>("_hide_mark_value") == true

    val title = if (hideValue) subjectName else "$subjectName • $value$suffix"
    val text = workType

    val notification: Notification = Notification.Builder(context, "data_update")
        .setContentTitle(title)
        .setContentText(text)
        .setAutoCancel(true)
        .setSmallIcon(R.drawable.ic_round_menu_book_24)
        .setContentIntent(
            PendingIntent.getActivity(
                context,
                1,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        )
        .build()

    if (ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        return
    }

    val nextId = totalCount + 1
    context.notificationPrefs.save("total_count" to nextId)
    NotificationManagerCompat.from(context).notify(nextId, notification)
}
