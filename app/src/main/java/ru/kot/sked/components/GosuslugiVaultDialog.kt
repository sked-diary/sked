package ru.kot.sked.components

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import ru.kot.sked.mainPrefs
import ru.kot.sked.save
import ru.kot.sked.security.GosuslugiVault

@Composable
fun GosuslugiVaultDialog(onDismiss: () -> Unit = {}, onSaved: () -> Unit = {}) {
    val context = LocalContext.current
    var isConfigured by remember { mutableStateOf(GosuslugiVault.hasCredentials(context)) }

    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var has2fa by remember { mutableStateOf(false) }
    var totpSecret by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Icon(
                Icons.Rounded.Security,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                "Безопасный автовход",
                style = MaterialTheme.typography.titleLarge
            )
        }

        if (isConfigured) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "✅ Автовход настроен и активен",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Данные зашифрованы аппаратным ключом Android Keystore (AES-256). При истечении сессии Госуслуг приложение само продлевает её в фоне, не выкидывая из дневника.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (GosuslugiVault.has2fa(context)) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "• Авторизация с автогенерацией кода 2FA (TOTP)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    GosuslugiVault.clearCredentials(context)
                    isConfigured = false
                    Toast.makeText(context, "Данные автовхода удалены", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Удалить сохранённые данные")
            }
        } else {
            Text(
                "Данные защищаются крипточипом процессора (Android Keystore). Они нужны, чтобы Госуслуги больше не выбивали каждые 3 часа.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = login,
                onValueChange = { login = it },
                label = { Text("Телефон / СНИЛС / Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Пароль от Госуслуг") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Подтверждение по 2FA (коду)",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium
                )
                Switch(
                    checked = has2fa,
                    onCheckedChange = { has2fa = it }
                )
            }

            if (has2fa) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = totpSecret,
                    onValueChange = { totpSecret = it },
                    label = { Text("Секретный ключ 2FA (буквы)") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    supportingText = {
                        Text("Ключ привязки из настроек Госуслуг -> Безопасность -> Вход по коду")
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.length <= 4) pin = it },
                label = { Text("ПИН-код для приложения (4 цифры)") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    if (login.isBlank() || password.isBlank() || pin.length < 4) {
                        Toast.makeText(context, "Заполните логин, пароль и 4-значный ПИН", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (has2fa && totpSecret.isBlank()) {
                        Toast.makeText(context, "Укажите секретный ключ 2FA", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val success = GosuslugiVault.saveCredentials(
                        context,
                        login = login,
                        pass = password,
                        has2fa = has2fa,
                        totpSecret = totpSecret,
                        pin = pin
                    )
                    if (success) {
                        context.mainPrefs.save(
                            "has_pin" to true,
                            "pin" to pin,
                            "biometric" to true,
                            "biometric_disabled_manually" to false,
                            "first_launch" to false,
                            "pin_disabled_manually" to false
                        )
                        isConfigured = true
                        Toast.makeText(context, "Успешно зашифровано в Keystore! ПИН-код входа включён.", Toast.LENGTH_SHORT).show()
                        onDismiss()
                        onSaved()
                    } else {
                        Toast.makeText(context, "Ошибка сохранения ключа", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.Lock, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Сохранить и включить автовход")
            }
        }
    }
}
