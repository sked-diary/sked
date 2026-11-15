package ru.kot.sked.components.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import ru.kot.sked.LocalActivity
import ru.kot.sked.R
import ru.kot.sked.components.GosuslugiVaultDialog
import ru.kot.sked.components.SwitchPreference
import ru.kot.sked.get
import ru.kot.sked.mainPrefs
import ru.kot.sked.save
import ru.kot.sked.screens.SetPinDialog
import ru.kot.sked.security.GosuslugiVault

object SecurityPrefs {
    val biometric =
        SwitchPreferenceSpec(R.string.fingerprint, prefKey = "biometric", defaultValue = true)
}

@Composable
fun Security() {
    val activity = LocalActivity.current
    var setPin by remember { mutableStateOf(false) }
    val pinEnabled =
        remember {
            mutableStateOf(
                activity.mainPrefs.get<Boolean>("has_pin")
                    ?: GosuslugiVault.hasCredentials(activity)
            )
        }
    val biometricEnabled =
        remember {
            mutableStateOf(
                if (activity.mainPrefs.get<Boolean>("biometric_disabled_manually") == true) false
                else (activity.mainPrefs.get<Boolean>("biometric") ?: true)
            )
        }
    SwitchPreference(
        title = stringResource(id = R.string.use_pin),
        listenState = pinEnabled
    ) {
        if (it) {
            setPin = true
        } else {
            activity.mainPrefs.save(
                "has_pin" to false,
                "pin" to null,
                "pin_disabled_manually" to true
            )
            pinEnabled.value = false
        }
    }
    SwitchPreference(
        title = stringResource(id = R.string.fingerprint),
        listenState = biometricEnabled,
        enabled = pinEnabled.value
    ) {
        biometricEnabled.value = it
        activity.mainPrefs.save(
            "biometric" to it,
            "biometric_disabled_manually" to !it
        )
    }

    AnimatedVisibility(setPin) {
        val pinFinished = remember { mutableStateOf(false) }
        val initialPin = remember { mutableStateOf(emptyList<Int>()) }
        val secondPin = remember { mutableStateOf(emptyList<Int>()) }

        SetPinDialog(
            pinFinished = pinFinished,
            initialPin = initialPin,
            secondPin = secondPin,
            closeButtonTitle = stringResource(id = R.string.cancel)
        )

        if (pinFinished.value) {
            setPin = false
        }

        if (
            initialPin.value.size == 4 &&
            secondPin.value.size == 4
        ) {
            if (initialPin.value == secondPin.value) {
                LocalContext.current.mainPrefs.save(
                    "has_pin" to true,
                    "pin" to secondPin.value.joinToString(""),
                    "biometric" to true,
                    "biometric_disabled_manually" to false,
                    "first_launch" to false,
                    "pin_disabled_manually" to false
                )
                pinFinished.value = true
                setPin = false
                pinEnabled.value = true
                biometricEnabled.value = true
            } else {
                initialPin.value = emptyList()
                secondPin.value = emptyList()
                pinFinished.value = false
            }
        }
    }

    HorizontalDivider(Modifier.padding(vertical = 12.dp))

    var showVaultDialog by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showVaultDialog = true }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Rounded.Security,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text("Безопасный автовход Госуслуг", style = MaterialTheme.typography.titleMedium)
            Text(
                if (GosuslugiVault.hasCredentials(activity)) "Защищено аппаратным чипом Keystore" else "Настроить пароль и 2FA для автообновления",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }

    if (showVaultDialog) {
        Dialog(onDismissRequest = { showVaultDialog = false }) {
            Card(
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                GosuslugiVaultDialog(
                    onDismiss = { showVaultDialog = false },
                    onSaved = {
                        pinEnabled.value = activity.mainPrefs.get<Boolean>("has_pin") ?: true
                        biometricEnabled.value = true
                    }
                )
            }
        }
    }
}