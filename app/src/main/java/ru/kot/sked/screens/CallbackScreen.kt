package ru.kot.sked.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.kot.sked.R
import ru.kot.sked.Screen
import ru.kot.sked.authPrefs
import ru.kot.sked.network.MESLoginService.MosExchangeToken
import ru.kot.sked.network.MySchoolLoginService.EsiaExchangeToken
import ru.kot.sked.save
import ru.kot.sked.screenLive
import ru.kot.sked.widget.StatusWidget.Companion.setUpdateFor
import java.util.Date

@Composable
fun CallbackScreen(code: String, type: CallbackType, subsystem: Int?) {
    val hasToken: MutableState<Boolean> = remember { mutableStateOf(false) }

    if (!hasToken.value) {
        when (type) {
            CallbackType.MosRu -> MosExchangeToken(code, hasToken)
            CallbackType.Esia -> EsiaExchangeToken(code, hasToken)
            CallbackType.TgBot -> {
                if (subsystem != null) {
                    LocalContext.current.authPrefs.save(
                        "auth" to true,
                        "subsystem" to subsystem,
                        "access_token" to code
                    )
                    hasToken.value = true
                    LocalContext.current.setUpdateFor(Date())
                } else {
                    screenLive.postValue(Screen.Login)
                }
            }
        }
    }

    Surface(Modifier.fillMaxSize()) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!hasToken.value) {
                Text(text = stringResource(R.string.exchanging_code_to_token), modifier = Modifier.padding(16.dp))
                CircularProgressIndicator()

            } else {
                screenLive.value = Screen.MainNav
            }
        }
    }
}

enum class CallbackType(val host: String) {
    MosRu("oauth2redirect"),
    Esia("authRegionRedirect"),
    TgBot("tgbot")
}