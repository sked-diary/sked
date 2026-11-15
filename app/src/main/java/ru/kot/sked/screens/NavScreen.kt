package ru.kot.sked.screens

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.AuthenticationCallback
import androidx.biometric.BiometricPrompt.AuthenticationResult
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.google.gson.Gson
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.navigation.NavHostController
import ru.kot.sked.DataService
import ru.kot.sked.Diary
import ru.kot.sked.LocalActivity
import ru.kot.sked.NavSection
import ru.kot.sked.R
import ru.kot.sked.Screen
import ru.kot.sked.NotificationScheduler
import ru.kot.sked.UpdateReceiver
import ru.kot.sked.authPrefs
import ru.kot.sked.cachePrefs
import ru.kot.sked.get
import ru.kot.sked.isDemo
import ru.kot.sked.logOut
import ru.kot.sked.mainPrefs
import ru.kot.sked.navControllerLive
import ru.kot.sked.network.NetworkService
import ru.kot.sked.network.gosuslugi.GosuslugiDataBridge
import ru.kot.sked.network.interfaces.DSchoolAPI
import ru.kot.sked.network.interfaces.MainSchoolAPI
import ru.kot.sked.network.interfaces.SchoolSessionAPI
import ru.kot.sked.network.interfaces.SecondaryAPI
import ru.kot.sked.notificationPrefs
import ru.kot.sked.save
import ru.kot.sked.screenLive
import ru.kot.sked.security.GosuslugiVault
import ru.kot.sked.sumLists
import ru.kot.sked.ui.theme.OctoDiaryTheme
import java.util.Calendar
import java.util.Collections

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavScreen(
    modifier: Modifier,
    pinFinished: MutableState<Boolean>,
    navControllerParam: NavHostController? = null
) {
    with(LocalContext.current) {
        val initialPin = remember { mutableStateOf(emptyList<Int>()) }
        val secondPin = remember { mutableStateOf(emptyList<Int>()) }
        val liveNavController = navControllerLive.observeAsState()
        val activeNavController = navControllerParam ?: liveNavController.value

        if (initialPin.value.size == 4 && secondPin.value.size == 4) {
            if (initialPin.value == secondPin.value) {
                pinFinished.value = true
                mainPrefs.save(
                    "has_pin" to true,
                    "pin" to secondPin.value.joinToString(""),
                    "biometric" to true,
                    "biometric_disabled_manually" to false,
                    "first_launch" to false,
                    "pin_disabled_manually" to false
                )
            } else {
                initialPin.value = emptyList()
                secondPin.value = emptyList()
                pinFinished.value = false

            }
        }

        val hasVaultCreds = GosuslugiVault.hasCredentials(this)
        val pinDisabledManually = mainPrefs.get<Boolean>("pin_disabled_manually") == true
        if (hasVaultCreds && !pinDisabledManually && mainPrefs.get<Boolean>("has_pin") != true) {
            val vaultPin = GosuslugiVault.getDecryptedPin(this)
            if (vaultPin != null) {
                mainPrefs.save("has_pin" to true, "pin" to vaultPin, "first_launch" to false)
            } else {
                mainPrefs.save("has_pin" to true, "first_launch" to false)
            }
        }
        val hasPinEnabled =
            mainPrefs.get<Boolean>("has_pin") == true || (hasVaultCreds && !pinDisabledManually)
        if (hasPinEnabled && mainPrefs.get<Boolean>("biometric_disabled_manually") != true && mainPrefs.get<Boolean>("biometric") != true) {
            mainPrefs.save("biometric" to true)
        }

        Surface(modifier.fillMaxSize(), color = Color.Transparent) {
            if (!hasPinEnabled || pinFinished.value) {
                val context = LocalActivity.current
                val isGosuslugi = context.authPrefs.get<String>("cookie") != null

                if (!isGosuslugi) {
                    DataService.token = authPrefs.get<String>("access_token") ?: ""
                    DataService.subsystem = Diary.values()[authPrefs.get<Int>("subsystem") ?: 0]
                    val diary = DataService.subsystem
                    DataService.mainSchoolApi =
                        NetworkService.mainSchoolApi(MainSchoolAPI.getBaseUrl(diary))
                    DataService.dSchoolApi = NetworkService.dSchoolApi(DSchoolAPI.getBaseUrl(diary))
                    DataService.schoolSessionApi =
                        NetworkService.schoolSessionApi(SchoolSessionAPI.getBaseUrl(diary))
                    DataService.secondaryApi =
                        NetworkService.secondaryApi(SecondaryAPI.getBaseUrl(diary))
                } else {
                    DataService.token = "gosuslugi"
                    DataService.subsystem = Diary.MES
                    DataService.mainSchoolApi =
                        NetworkService.mainSchoolApi(MainSchoolAPI.getBaseUrl(Diary.MES))
                    DataService.dSchoolApi = NetworkService.dSchoolApi(DSchoolAPI.getBaseUrl(Diary.MES))
                    DataService.schoolSessionApi =
                        NetworkService.schoolSessionApi(SchoolSessionAPI.getBaseUrl(Diary.MES))
                    DataService.secondaryApi =
                        NetworkService.secondaryApi(SecondaryAPI.getBaseUrl(Diary.MES))
                }

                LaunchedEffect(DataService.loadedEverything.value) {
                    if (DataService.loadedEverything.value && !isGosuslugi) {
                        context.registerNotifier()
                    }
                    if (DataService.loadedEverything.value &&
                        !ru.kot.sked.components.BirthdayHelper.confettiShownThisSession &&
                        ru.kot.sked.components.BirthdayHelper.isTodayBirthday(context)
                    ) {
                        ru.kot.sked.components.BirthdayHelper.confettiShownThisSession = true
                        delay(450)
                        ru.kot.sked.triggerBirthdayConfettiLive.postValue(System.currentTimeMillis())
                    }
                }
                LaunchedEffect(Unit) {
                    if (!ru.kot.sked.components.BirthdayHelper.confettiShownThisSession &&
                        ru.kot.sked.components.BirthdayHelper.isTodayBirthday(context)
                    ) {
                        ru.kot.sked.components.BirthdayHelper.confettiShownThisSession = true
                        delay(450)
                        ru.kot.sked.triggerBirthdayConfettiLive.postValue(System.currentTimeMillis())
                    }
                }
                if (DataService.loadedEverything.value) {
                    val refreshState = rememberPullToRefreshState()
                    var duringRefresh by rememberSaveable { mutableStateOf(false) }
                    if (refreshState.isRefreshing && !duringRefresh) {
                        if (!isDemo) {
                            if (isGosuslugi) {
                                duringRefresh = true
                                GosuslugiDataBridge.fetchAndApply(context) {
                                    refreshState.endRefresh()
                                    duringRefresh = false
                                }
                            } else {
                                DataService.loadedEverything.value = false
                                DataService.loadingStarted = false
                                DataService.updateAll(context)
                                duringRefresh = true
                            }
                        } else refreshState.endRefresh()
                    }
                    if (DataService.loadedEverything.value) {
                        refreshState.endRefresh()
                        duringRefresh = false
                    }
                    LaunchedEffect(Unit) {
                        if (!isGosuslugi) {
                            DataService.sendStatistic {}
                        }
                    }
                    if (activeNavController != null) {
                        NavHost(
                            navController = activeNavController,
                            startDestination = NavSection.Dashboard.route
                        ) {
                            NavSection.values().forEach {
                                composable(it.route) { _ ->
                                    Box(
                                        Modifier
                                            .fillMaxSize()
                                            .nestedScroll(refreshState.nestedScrollConnection)
                                    ) {
                                        it.composable()
                                        PullToRefreshContainer(
                                            refreshState, Modifier.align(Alignment.TopCenter)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    var progress by remember { mutableFloatStateOf(0f) }
                    val progressAnimated by animateFloatAsState(
                        progress, tween(200), label = "progress_anim"
                    )
                    val coroutineScope = rememberCoroutineScope()
                    DataService.onSingleItemInUpdateAllLoadedHandler = { name, progressParam ->
                        coroutineScope.launch {
                            progress = progressParam
                        }
                        cachePrefs.save(
                            name to Gson().toJson(
                                DataService::class.java.getDeclaredField(name).get(DataService)
                            ), "age" to System.currentTimeMillis()
                        )
                    }

                    val activity = LocalActivity.current
                    DataService.tokenExpirationHandler = {
                        activity.logOut("Performed from token expiration handler")
                    }

                    if (!DataService.loadingStarted || !DataService.hasMarksSubject || DataService.eventCalendar.isEmpty()) {
                        if (isGosuslugi) {
                            DataService.loadingStarted = true
                            val hasCache = GosuslugiDataBridge.loadFromCache(context)
                            if (hasCache) {
                                DataService.loadedEverything.value = true
                            }
                            GosuslugiDataBridge.fetchAndApply(context) {}
                        } else if (cachePrefs.get<Long>("age")
                                ?.let { (System.currentTimeMillis() - it) < 86400000 } == true
                        ) {
                            DataService.loadingStarted = true
                            DataService.subsystem =
                                Diary.values()[authPrefs.get<Int>("subsystem") ?: 0]
                            DataService.loadFromCache { cachePrefs.get<String>(it) ?: "" }
                            DataService.loadedEverything.value = true
                        } else if (isDemo) {
                            DataService.subsystem = Diary.MES
                            DataService.run { loadDemoCache() }
                            DataService.loadedEverything.value = true
                        } else {
                            DataService.updateAll(context)
                        }
                    }
                    Column(
                        Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        LinearProgressIndicator(
                            progress = { progressAnimated },
                        )
                    }
                }
            } else if (hasPinEnabled && !pinFinished.value) {
                EnterPinDialog(pinFinished = pinFinished)
            }
            if ((mainPrefs.get<Boolean>("first_launch") == true) && !hasPinEnabled && !pinFinished.value) {
                SetPinDialog(
                    pinFinished = pinFinished, initialPin = initialPin, secondPin = secondPin
                )
            }
        }
    }
}

private fun FragmentActivity.registerNotifier() {
    NotificationScheduler.schedule(this)
}

@Composable
fun EnterPinDialog(
    pinFinished: MutableState<Boolean>,
) {
    val context = LocalContext.current
    val currentPin = remember { mutableStateOf(emptyList<Int>()) }
    var wrongPin by remember { mutableStateOf(false) }
    val fpShown = remember { mutableStateOf(false) }
    if (currentPin.value.size == 4) {
        val entered = currentPin.value.joinToString("")
        val savedPin = context.mainPrefs.get<String>("pin")
        val vaultPin = GosuslugiVault.getDecryptedPin(context)
        val isPinValid = (savedPin != null && entered == savedPin) ||
                (vaultPin != null && entered == vaultPin) ||
                GosuslugiVault.verifyPin(context, entered)
        if (isPinValid) {
            if (savedPin != entered) {
                context.mainPrefs.save("has_pin" to true, "pin" to entered)
            }
            pinFinished.value = true
        } else {
            currentPin.value = emptyList()
            wrongPin = !wrongPin
        }
    }

    Dialog(properties = DialogProperties(
        dismissOnBackPress = false,
        dismissOnClickOutside = false,
        usePlatformDefaultWidth = false
    ), onDismissRequest = {}) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Box(Modifier.fillMaxSize()) {
                // Top exit/logout action button
                Row(
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    val activity = LocalActivity.current
                    FilledTonalButton(
                        onClick = {
                            activity.logOut("Passcode is forgotten")
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.Logout,
                            stringResource(id = R.string.log_out),
                            Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            stringResource(id = R.string.log_out),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Cute brand avatar with nice circle background & padding
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.size(88.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = androidx.compose.ui.res.painterResource(R.drawable.ic_launcher_foreground),
                                contentDescription = null,
                                modifier = Modifier.size(92.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    Text(
                        text = "Вход в Sked",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = if (wrongPin) "Неверный ПИН-код, попробуйте ещё раз" else "Введите 4-значный код безопасности",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (wrongPin) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(24.dp))

                    AnimatedVisibility(
                        visible = !wrongPin,
                        label = "pin_screen_animation",
                        exit = fadeOut(tween(250)),
                        enter = fadeIn(tween(250))
                    ) {
                        PinScreen(
                            into = currentPin,
                            fp = true,
                            fpShown = fpShown,
                            onAuthSuccess = { pinFinished.value = true }
                        )
                    }
                    AnimatedVisibility(
                        visible = wrongPin,
                        label = "pin_screen_animation",
                        exit = fadeOut(tween(250)),
                        enter = fadeIn(tween(250))
                    ) {
                        PinScreen(
                            into = currentPin,
                            fp = true,
                            fpShown = fpShown,
                            onAuthSuccess = { pinFinished.value = true }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SetPinDialog(
    pinFinished: MutableState<Boolean>,
    initialPin: MutableState<List<Int>>,
    secondPin: MutableState<List<Int>>,
    closeButtonTitle: String = stringResource(id = R.string.skip),
) {
    val context = LocalContext.current
    Dialog(properties = DialogProperties(
        dismissOnBackPress = false,
        dismissOnClickOutside = false,
        usePlatformDefaultWidth = false
    ), onDismissRequest = { pinFinished.value = true }) {
        Surface {
            Column(
                Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp), horizontalArrangement = Arrangement.End
                ) {
                    FilledTonalButton(
                        onClick = {
                            pinFinished.value = true
                            context.mainPrefs.save(
                                "has_pin" to false, "first_launch" to false
                            )
                        }, contentPadding = ButtonDefaults.ButtonWithIconContentPadding
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            closeButtonTitle,
                            Modifier.size(ButtonDefaults.IconSize)
                        )
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text(closeButtonTitle)
                    }
                }
                AnimatedContent(
                    targetState = initialPin, label = "title_animation"
                ) { targetState ->
                    Text(
                        stringResource(
                            if (targetState.value.size == 4) {
                                R.string.repeat_pin
                            } else R.string.enter_new_pin
                        ),
                        Modifier.padding(vertical = 64.dp),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                AnimatedContent(
                    targetState = initialPin, label = "pin_screen_animation"
                ) { targetState ->
                    if (targetState.value.size >= 4) {
                        PinScreen(
                            Modifier.fillMaxSize(), secondPin
                        )
                    } else PinScreen(
                        Modifier.fillMaxSize(), targetState
                    )
                }
            }
        }
    }
}

@Composable
fun PinScreen(
    modifier: Modifier = Modifier,
    into: MutableState<List<Int>>,
    fp: Boolean = false,
    fpShown: MutableState<Boolean> = mutableStateOf(false),
    onAuthSuccess: (() -> Unit)? = null
) {
    val enteredNumbers = remember { mutableStateOf(emptyList<Int>()) }
    val hapticFeedback = LocalHapticFeedback.current
    val context = LocalActivity.current
    val biometricManager = BiometricManager.from(context)
    val isBiometricAvailable = (biometricManager.canAuthenticate(
        BIOMETRIC_STRONG or DEVICE_CREDENTIAL or BIOMETRIC_WEAK
    ) == BiometricManager.BIOMETRIC_SUCCESS) &&
            (context.mainPrefs.get<Boolean>("biometric_disabled_manually") != true) &&
            (context.mainPrefs.get<Boolean>("biometric") ?: true)

    if (enteredNumbers.value.size >= 4) {
        into.value = enteredNumbers.value.take(4)
    }

    LaunchedEffect(fp) {
        if (!fpShown.value && fp && (context.mainPrefs.get<String>("pin") != null || GosuslugiVault.hasCredentials(context))) {
            fpShown.value = true
            if (isBiometricAvailable) {
                biometricAuth(context, enteredNumbers, onAuthSuccess)
            }
        }
    }

    Column(
        modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // PIN Indicators (Dots)
        Row(
            Modifier
                .padding(bottom = 36.dp)
                .zIndex(2f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            (0..3).forEach { index ->
                val isFilled = enteredNumbers.value.size > index
                val dotScale by animateFloatAsState(
                    targetValue = if (isFilled) 1.25f else 1.0f,
                    animationSpec = tween(150),
                    label = "dot_scale"
                )
                Box(
                    Modifier
                        .padding(horizontal = 12.dp)
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(
                            if (isFilled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            (0..9).toList().also { Collections.rotate(it, -1) }.chunked(3).forEach { triplet ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (triplet[0] == 0 && isBiometricAvailable && fp) {
                        Surface(
                            modifier = Modifier
                                .padding(8.dp)
                                .size(72.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            onClick = {
                                hapticFeedback.performHapticFeedback(
                                    HapticFeedbackType.LongPress
                                )
                                biometricAuth(context, enteredNumbers, onAuthSuccess)
                            }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.Fingerprint,
                                    stringResource(R.string.fingerprint),
                                    Modifier.size(32.dp),
                                    MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    } else if (triplet[0] == 0) {
                        Box(Modifier.padding(8.dp).size(72.dp))
                    }
                    triplet.forEach {
                        PinKeyboardButton(it, enteredNumbers)
                    }
                    if (triplet[0] == 0) {
                        Surface(
                            modifier = Modifier
                                .padding(8.dp)
                                .size(72.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            onClick = {
                                removeNumber(enteredNumbers)
                                hapticFeedback.performHapticFeedback(
                                    HapticFeedbackType.LongPress
                                )
                            }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.Backspace,
                                    stringResource(R.string.cancel),
                                    Modifier.size(26.dp),
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PinKeyboardButton(number: Int, enteredNumbers: MutableState<List<Int>>) {
    val hapticFeedback = LocalHapticFeedback.current
    Surface(
        modifier = Modifier
            .padding(8.dp)
            .size(72.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        onClick = {
            if (enteredNumbers.value.size < 4) {
                clickNumber(number, enteredNumbers)
                hapticFeedback.performHapticFeedback(
                    HapticFeedbackType.LongPress
                )
            }
        }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

fun clickNumber(number: Int, enteredNumbers: MutableState<List<Int>>) {
    enteredNumbers.value += listOf(number)
}

fun removeNumber(enteredNumbers: MutableState<List<Int>>) {
    if (enteredNumbers.value.isNotEmpty()) {
        enteredNumbers.value = enteredNumbers.value.subList(0, enteredNumbers.value.lastIndex)
    }
}

fun biometricAuth(
    context: FragmentActivity,
    enteredNumbers: MutableState<List<Int>>,
    onAuthSuccess: (() -> Unit)? = null
) {
    with(context) {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt =
            BiometricPrompt(this, executor, object : AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    val pin = mainPrefs.get<String>("pin") ?: GosuslugiVault.getDecryptedPin(this@with)
                    if (pin != null) {
                        enteredNumbers.value = pin.mapNotNull { it.digitToIntOrNull() }
                    }
                    onAuthSuccess?.invoke()
                }
            })
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.fingerprint))
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL or BIOMETRIC_WEAK)
            .build()
        biometricPrompt.authenticate(promptInfo)
    }
}

@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_NO, locale = "ru"
)
@Composable
fun PinDialogPreview() {
    OctoDiaryTheme {
        Surface(Modifier.fillMaxSize()) {
            SetPinDialog(remember { mutableStateOf(false) },
                remember { mutableStateOf(emptyList()) },
                remember { mutableStateOf(emptyList()) })
        }
    }
}