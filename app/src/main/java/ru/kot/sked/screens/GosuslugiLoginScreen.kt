package ru.kot.sked.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Build
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import ru.kot.sked.DataService
import ru.kot.sked.Screen
import ru.kot.sked.authPrefs
import ru.kot.sked.components.GosuslugiVaultDialog
import ru.kot.sked.get
import ru.kot.sked.mainPrefs
import ru.kot.sked.save
import ru.kot.sked.screenLive
import ru.kot.sked.network.gosuslugi.GosuslugiDataBridge

@Composable
fun GosuslugiLoginScreen(modifier: Modifier = Modifier) {
    var mode by remember { mutableStateOf("choose") }
    androidx.activity.compose.BackHandler(enabled = mode == "web") { mode = "choose" }
    if (mode == "web") GosuslugiWebLogin(modifier) else LoginChooser(modifier) { mode = "web" }
}

@Composable
private fun LoginChooser(modifier: Modifier, onWeb: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var showVault by remember { mutableStateOf(false) }
    var autoLogging by remember { mutableStateOf(false) }
    var configured by remember { mutableStateOf(ru.kot.sked.security.GosuslugiVault.hasCredentials(context)) }

    val startAutoLogin = {
        autoLogging = true
        ru.kot.sked.AppLog.d("Login", "Auto-login started")
        GosuslugiDataBridge.refreshSessionSilently(context) { ok ->
            if (ok) {
                val cookies = context.authPrefs.get<String>("cookie") ?: ""
                Thread {
                    val studentId = extractCookie(cookies, "studentId")
                        ?: GosuslugiDataBridge.resolveStudentId(cookies)
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        if (studentId == null) {
                            ru.kot.sked.AppLog.w("Login", "Auto-login: student id not found")
                            autoLogging = false
                            android.widget.Toast.makeText(context, "Не удалось определить ученика. Попробуйте войти вручную", android.widget.Toast.LENGTH_LONG).show()
                        } else {
                            context.authPrefs.save("auth" to true, "student_id" to studentId)
                            val vaultPin = ru.kot.sked.security.GosuslugiVault.getDecryptedPin(context)
                            if (vaultPin != null) {
                                context.mainPrefs.save(
                                    "has_pin" to true,
                                    "pin" to vaultPin,
                                    "biometric" to true,
                                    "biometric_disabled_manually" to false,
                                    "first_launch" to false,
                                    "pin_disabled_manually" to false
                                )
                            } else if (ru.kot.sked.security.GosuslugiVault.hasCredentials(context)) {
                                context.mainPrefs.save(
                                    "has_pin" to true,
                                    "biometric" to true,
                                    "biometric_disabled_manually" to false,
                                    "first_launch" to false,
                                    "pin_disabled_manually" to false
                                )
                            }
                            DataService.loadingStarted = false
                            DataService.loadedEverything.value = false
                            ru.kot.sked.AppLog.d("Login", "Auto-login OK, loading data")
                            GosuslugiDataBridge.fetchAndApply(context) { screenLive.postValue(Screen.MainNav) }
                        }
                    }
                }.start()
            } else {
                ru.kot.sked.AppLog.w("Login", "Auto-login failed")
                autoLogging = false
                android.widget.Toast.makeText(context, "Автовход не удался. Войдите через Госуслуги вручную", android.widget.Toast.LENGTH_LONG).show()
            }
        }
        Unit
    }

    Box(modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text("Вход в Sked", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(16.dp))
            LoginOption(
                "Войти через Госуслуги",
                "Откроется страница Госуслуг. Войдите вручную, вход завершится автоматически.\n⚠️ Внимание: при обычном входе сессия Госуслуг периодически истекает, поэтому для автоматического продления без повторного ввода рекомендуется настроить Автовход ниже.",
                filled = true,
                enabled = !autoLogging,
                onClick = onWeb
            )
            LoginOption(
                "Автовход",
                if (configured) "Данные сохранены. Приложение сразу попытается войти." else "Введите логин и пароль один раз. Они хранятся зашифрованно (Android Keystore), и после ввода приложение сразу попытается войти.",
                filled = false,
                enabled = !autoLogging,
                onClick = { if (configured) startAutoLogin() else showVault = true }
            )
            if (configured) {
                TextButton(onClick = { showVault = true }, enabled = !autoLogging) { Text("Изменить данные автовхода") }
            }
            LoginOption(
                "Демо-режим",
                "Пробный вход с вымышленным учеником и тестовыми данными. Без аккаунта Госуслуг.",
                filled = false,
                enabled = !autoLogging,
                onClick = { ru.kot.sked.network.MySchoolLoginService.logInWithPassword(context, demo = true) }
            )
        }
        if (autoLogging) {
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Входим через Госуслуги…")
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Spacer(Modifier.height(32.dp))
            }
        }
        if (showVault) {
            Dialog(onDismissRequest = { showVault = false }) {
                Card(
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    GosuslugiVaultDialog(
                        onDismiss = {
                            showVault = false
                            configured = ru.kot.sked.security.GosuslugiVault.hasCredentials(context)
                        },
                        onSaved = { startAutoLogin() }
                    )
                }
            }
        }
    }
}

@Composable
private fun LoginOption(title: String, note: String, filled: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        if (filled) Button(onClick, Modifier.fillMaxWidth(), enabled = enabled) { Text(title) }
        else OutlinedButton(onClick, Modifier.fillMaxWidth(), enabled = enabled) { Text(title) }
        Text(
            note,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp, top = 4.dp, end = 8.dp)
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun GosuslugiWebLogin(modifier: Modifier = Modifier) {
    var isLoading by remember { mutableStateOf(true) }
    var loginHandled by remember { mutableStateOf(false) }

    val handleLoginSuccess = { ctx: android.content.Context, cookies: String ->
        if (!loginHandled) {
            loginHandled = true
            isLoading = true
            Thread {
                val studentId = extractCookie(cookies, "studentId")
                    ?: GosuslugiDataBridge.resolveStudentId(cookies)
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    if (studentId == null) {
                        loginHandled = false
                        isLoading = false
                        android.widget.Toast.makeText(ctx, "Не удалось определить ученика. Попробуйте ещё раз", android.widget.Toast.LENGTH_LONG).show()
                    } else {
                        ctx.authPrefs.save(
                            "auth" to true,
                            "cookie" to cookies,
                            "student_id" to studentId
                        )
                        val vaultPin = ru.kot.sked.security.GosuslugiVault.getDecryptedPin(ctx)
                        if (vaultPin != null) {
                            ctx.mainPrefs.save(
                                "has_pin" to true,
                                "pin" to vaultPin,
                                "first_launch" to false,
                                "pin_disabled_manually" to false
                            )
                        } else if (ru.kot.sked.security.GosuslugiVault.hasCredentials(ctx)) {
                            ctx.mainPrefs.save(
                                "has_pin" to true,
                                "first_launch" to false,
                                "pin_disabled_manually" to false
                            )
                        } else if (ctx.mainPrefs.get<Boolean>("has_pin") != true) {
                            ctx.mainPrefs.save("first_launch" to true)
                        }
                        DataService.loadingStarted = false
                        DataService.loadedEverything.value = false
                        GosuslugiDataBridge.fetchAndApply(ctx) {
                            screenLive.postValue(Screen.MainNav)
                        }
                    }
                }
            }.start()
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_YES
                    }

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        saveFormData = true
                        userAgentString =
                            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                    }

                    val checkCookie = { url: String? ->
                        val cookies = CookieManager.getInstance().getCookie("https://www.gosuslugi.ru") ?: ""
                        val isOnEsiaLogin = url?.contains("esia.gosuslugi.ru/login") == true || url?.contains("esia.gosuslugi.ru/aas") == true
                        val accT = extractCookie(cookies, "acc_t")

                        // We ONLY complete login if we have a fresh, valid (non-expired) acc_t and we are not stuck on ESIA login
                        if (!isOnEsiaLogin && !accT.isNullOrEmpty() && !GosuslugiDataBridge.isJwtExpired(accT) && !loginHandled) {
                            handleLoginSuccess(ctx, cookies)
                        }
                    }

                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            isLoading = true
                            checkCookie(url)
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isLoading = false
                            checkCookie(url)
                        }

                        override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                            super.doUpdateVisitedHistory(view, url, isReload)
                            checkCookie(url)
                        }
                    }

                    loadUrl("https://www.gosuslugi.ru/school/feed")
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            )
        }

        var showVaultDialog by remember { mutableStateOf(false) }

        ExtendedFloatingActionButton(
            onClick = { showVaultDialog = true },
            icon = { Icon(Icons.Rounded.Security, null) },
            text = { Text("Безопасный автовход") },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        )

        if (showVaultDialog) {
            Dialog(onDismissRequest = { showVaultDialog = false }) {
                Card(
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    GosuslugiVaultDialog(onDismiss = { showVaultDialog = false })
                }
            }
        }
    }
}

private fun extractCookie(cookieString: String, name: String): String? {
    val items = cookieString.split(";")
    for (item in items) {
        val trimmed = item.trim()
        if (trimmed.startsWith("$name=")) {
            return trimmed.substringAfter("$name=")
        }
    }
    return null
}
