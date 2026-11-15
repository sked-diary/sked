package ru.kot.sked.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideOut
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import ru.kot.sked.MainActivity
import ru.kot.sked.authPrefs
import ru.kot.sked.mainPrefs
import ru.kot.sked.triggerBirthdayConfettiLive
import ru.kot.sked.components.debugutils.PrefEditor
import ru.kot.sked.components.debugutils.RemoteEditor
import ru.kot.sked.components.debugutils.WidgetConfig
import ru.kot.sked.components.debugutils.TestNotificationDialog
import ru.kot.sked.get
import android.widget.Toast
import ru.kot.sked.save
import ru.kot.sked.network.gosuslugi.GosuslugiDataBridge

@Composable
fun DebugMenu(
    context: MainActivity,
) {
    var isHide by remember { mutableStateOf(false) }
    var isShownMenu by remember { mutableStateOf(false) }
    var currentFunction: @Composable () -> Unit by remember { mutableStateOf({}) }
    val interactionSource = remember { MutableInteractionSource() }
    val viewConfiguration = LocalViewConfiguration.current
    var clickCount by remember { mutableIntStateOf(0) }
    var lastClickTime by remember { mutableLongStateOf(0L) }

    LaunchedEffect(interactionSource) {
        var isLongClick = false

        interactionSource.interactions.collectLatest { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    isLongClick = false
                    delay(viewConfiguration.longPressTimeoutMillis)
                    isLongClick = true
                    isHide = true
                }

                is PressInteraction.Release -> {
                    if (isLongClick.not()) {
                        val now = System.currentTimeMillis()
                        if (now - lastClickTime < 650L) {
                            clickCount++
                            if (clickCount >= 10) {
                                clickCount = 0
                                isShownMenu = false
                                val currentForce = context.mainPrefs.get<Boolean>("force_birthday") ?: false
                                val newForce = !currentForce
                                context.mainPrefs.save("force_birthday" to newForce)
                                triggerBirthdayConfettiLive.postValue(System.currentTimeMillis())
                                val msg = if (newForce) "🎂 Режим Дня Рождения активирован! 🎉" else "Режим Дня Рождения выключен"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            } else {
                                isShownMenu = !isShownMenu
                            }
                        } else {
                            clickCount = 1
                            isShownMenu = !isShownMenu
                        }
                        lastClickTime = now
                    }
                }
            }
        }
    }
    AnimatedVisibility(visible = !isHide, exit = slideOut { IntOffset(0, -it.height * 2) }) {
        OutlinedButton(
            onClick = {},
            Modifier.padding(end = 8.dp),
            interactionSource = interactionSource
        ) {
            Text("Debug")
        }
    }
    DropdownMenu(isShownMenu, onDismissRequest = { isShownMenu = false }) {
        DebugMenuItems.values().forEach {
            if (it.showCondition()) {
                DropdownMenuItem(
                    { Text(it.title) },
                    onClick = {
                        currentFunction = { it.function(context) { currentFunction = {} } }
                    })
            }
        }
    }
    currentFunction()
}

enum class DebugMenuItems(
    val title: String,
    val function: @Composable MainActivity.(clearFn: () -> Unit) -> Unit,
    val showCondition: @Composable () -> Boolean = { true },
) {
    PreferenceEditor("Preference editor", { clearFn -> PrefEditor { clearFn() } }),
    RemoteSettingsEditor(
        "Remote settings editor",
        { clearFn -> RemoteEditor { clearFn() } },
        { LocalContext.current.authPrefs.get<String>("access_token") != null }),
    ConfigureWidget(
        "Widget config",
        { clearFn -> WidgetConfig { clearFn() } }
    ),
    TestNotification(
        "Тест уведомления об оценке",
        { clearFn -> TestNotificationDialog { clearFn() } }
    ),
    ExpireToken(
        "Испортить токен (Тест автовхода)",
        { clearFn ->
            val cookie = authPrefs.get<String>("cookie") ?: ""
            val expiredJwt = "eyJhbGciOiAiUlMyNTYiLCAidHlwIjogIkpXVCJ9.eyJzdWIiOiAiMTIwNDk4MTAwNCIsICJhdWQiOiAibXlzY2hvb2wiLCAiZXhwIjogMTU3NzgzNjgwMCwgImlhdCI6IDE1Nzc4MDAwMDB9.fake_signature"
            val newCookie = if (cookie.contains("acc_t=")) {
                cookie.replace(Regex("acc_t=[^;]+"), "acc_t=$expiredJwt")
            } else {
                "$cookie; acc_t=$expiredJwt"
            }
            authPrefs.save("cookie" to newCookie)
            Toast.makeText(this, "Токен заменен на протухший! Запускаем проверку...", Toast.LENGTH_SHORT).show()
            GosuslugiDataBridge.fetchAndApply(this) { success ->
                val resultMsg = if (success) "Автовход успешно сработал и обновил токен!" else "Ошибка автовхода"
                Toast.makeText(this, resultMsg, Toast.LENGTH_LONG).show()
            }
            clearFn()
        }
    )
}
