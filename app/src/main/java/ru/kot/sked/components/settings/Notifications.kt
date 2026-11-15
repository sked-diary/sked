package ru.kot.sked.components.settings

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import ru.kot.sked.LocalActivity
import ru.kot.sked.R
import ru.kot.sked.components.SwitchPreference
import ru.kot.sked.get
import ru.kot.sked.notificationPrefs
import ru.kot.sked.save

@Composable
fun Notifications() {
    val activity = LocalActivity.current
    val notifyWithValue = remember {
        mutableStateOf(
            !(activity.notificationPrefs.get<Boolean>("_hide_mark_value") ?: false)
        )
    }
    if (Manifest.permission.POST_NOTIFICATIONS.let {
            activity.checkCallingOrSelfPermission(it)
        } == PackageManager.PERMISSION_GRANTED) {
        SwitchPreference(
            title = stringResource(R.string.show_mark_value_in_notification),
            listenState = notifyWithValue
        ) {
            notifyWithValue.value = it
            activity.notificationPrefs.save("_hide_mark_value" to !it)
        }
    }
}