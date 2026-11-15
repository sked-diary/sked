package ru.kot.sked.network

import android.content.Context
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import ru.kot.sked.DataService
import ru.kot.sked.DataService.loadDemoCache
import ru.kot.sked.Diary
import ru.kot.sked.Screen
import ru.kot.sked.authPrefs
import ru.kot.sked.baseEnqueue
import ru.kot.sked.get
import ru.kot.sked.mainPrefs
import ru.kot.sked.network.NetworkService.MySchoolAPIConfig.LOGIN_PASSWORD_TEMPLATE
import ru.kot.sked.network.interfaces.SecondaryAPI
import ru.kot.sked.save
import ru.kot.sked.screenLive
import ru.kot.sked.widget.StatusWidget.Companion.setUpdateFor
import java.util.Date
import java.util.UUID

object MySchoolLoginService {
    lateinit var diary: Diary

    fun logInWithEsia(context: Context, diary: Diary) {
        this.diary = diary
        val tabIntent = CustomTabsIntent.Builder().build()
        tabIntent.launchUrl(context, NetworkService.MySchoolAPIConfig.run {
            val guid = UUID.randomUUID().toString()
            context.authPrefs.save("state" to guid)
            ESIA_AUTH_URL_TEMPLATE.format(
                SecondaryAPI.getBaseUrl(diary), REDIRECT_URI, guid
            )
        }.toUri())
    }

    @Composable
    fun EsiaExchangeToken(code: String, hasToken: MutableState<Boolean>) {
        val context = LocalContext.current
        NetworkService.secondaryApi(SecondaryAPI.getBaseUrl(diary)).esiaExchangeToken(
            code, context.authPrefs.get<String>("state")!!
        ).baseEnqueue {
            context.authPrefs.save(
                "auth" to true,
                "subsystem" to diary.ordinal,
                "access_token" to it.token
            )
            context.mainPrefs.save(
                "first_launch" to true
            )
            context.setUpdateFor(Date())
            hasToken.value = true
        }
    }

    fun logInWithPassword(
        context: Context,
        demo: Boolean = false
    ) {
        this@MySchoolLoginService.diary = Diary.MySchool
        if (demo) {
            return context.logInDemo()
        }
        val guid = UUID.randomUUID().toString()
        context.authPrefs.save("state" to guid)
        val tabIntent = CustomTabsIntent.Builder().build()
        tabIntent.launchUrl(context, LOGIN_PASSWORD_TEMPLATE.format(guid).toUri())
    }

    private fun Context.logInDemo() {
        authPrefs.save(
            "auth" to true,
            "subsystem" to Diary.MES.ordinal,
            "access_token" to "gitgud",
            "cookie" to null,
            "student_id" to null
        )
        mainPrefs.save(
            "first_launch" to true,
            "demo" to true
        )
        DataService.subsystem = Diary.MES
        DataService.run { loadDemoCache() }
        DataService.loadingStarted = true
        DataService.loadedEverything.value = true
        screenLive.postValue(Screen.MainNav)
    }
}