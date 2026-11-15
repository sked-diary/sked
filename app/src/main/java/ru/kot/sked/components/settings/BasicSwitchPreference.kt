package ru.kot.sked.components.settings

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import ru.kot.sked.components.SwitchPreference
import ru.kot.sked.get
import ru.kot.sked.mainPrefs
import ru.kot.sked.save

class SwitchPreferenceSpec(
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int? = null,
    val prefKey: String,
    val defaultValue: Boolean,
)

@Composable
fun SwitchPreferenceSpec.BasicSwitchPreference(
    enabled: Boolean = true,
) {
    val context = LocalContext.current
    val state = remember { mutableStateOf(context.mainPrefs.get(prefKey) ?: defaultValue) }

    SwitchPreference(stringResource(titleRes), descriptionRes?.let { stringResource(it) }, state, enabled = enabled) {
        state.value = it
        context.mainPrefs.save(prefKey to it)
    }
}