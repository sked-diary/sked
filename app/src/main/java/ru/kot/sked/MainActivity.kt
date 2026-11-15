package ru.kot.sked

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.ActivityCompat
import androidx.core.graphics.scale
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import ru.kot.sked.components.DebugMenu
import ru.kot.sked.components.MigrationDialog
import ru.kot.sked.components.ProfileChooser
import ru.kot.sked.components.SettingsDialog
import ru.kot.sked.components.TokenLogin
import ru.kot.sked.components.settings.About
import ru.kot.sked.screens.CallbackScreen
import ru.kot.sked.screens.CallbackType
import ru.kot.sked.screens.GosuslugiLoginScreen
import ru.kot.sked.screens.LoginScreen
import ru.kot.sked.screens.NavScreen
import ru.kot.sked.screens.navsections.daybook.DayChooser
import ru.kot.sked.screens.navsections.profile.avatarTriggerLive
import ru.kot.sked.ui.theme.CustomColorScheme
import ru.kot.sked.ui.theme.OctoDiaryTheme
import ru.kot.sked.ui.theme.dynamicColorSchemeFromSeed
import ru.kot.sked.ui.theme.extractDominantColor
import java.io.ByteArrayOutputStream
import java.io.File

data class FullThemeState(
    val darkTheme: Boolean,
    val colorScheme: Int,
    val isAmoled: Boolean,
    val wallpaperColorActive: Boolean,
    val wallpaperSeedColor: Int?
)

val modalBottomSheetStateLive = MutableLiveData(false)
val modalBottomSheetContentLive = MutableLiveData<@Composable () -> Unit> {}
val snackbarHostStateLive = MutableLiveData(SnackbarHostState())
val navControllerLive = MutableLiveData<NavHostController?>(null)
val showFilterLive = MutableLiveData(false)
val contentDependentActionLive = MutableLiveData<@Composable () -> Unit> {}
val contentDependentActionIconLive = MutableLiveData(Icons.Rounded.FilterAlt)
val screenLive = MutableLiveData<Screen>()
val modalDialogStateLive = MutableLiveData(false)
val modalDialogContentLive = MutableLiveData<@Composable () -> Unit> {}
val modalDialogCloseListenerLive = MutableLiveData<() -> Unit> {}
val reloadEverythingLive = MutableLiveData {}
val darkThemeLive = MutableLiveData<Boolean>(null)
val isAmoledLive = MutableLiveData(false)
val colorSchemeLive = MutableLiveData(-1)
val launchUrlLive = MutableLiveData<Uri?>(null)
val launchPickerLive = MutableLiveData<() -> Unit>({})
val webViewDialogLive = MutableLiveData<(@Composable () -> Unit)?>(null)
val hasCustomWallpaperLive = MutableLiveData(false)
val wallpaperBlurLive = MutableLiveData(12f)
val wallpaperDimLive = MutableLiveData(0.40f)
val useWallpaperColorLive = MutableLiveData(true)
val wallpaperSeedColorLive = MutableLiveData<Int?>(null)
val wallpaperTriggerLive = MutableLiveData(0L)
val wallpaperPreviewLive = MutableLiveData(false)
val launchWallpaperPickerLive = MutableLiveData<() -> Unit>({})
val triggerBirthdayConfettiLive = MutableLiveData(0L)
val LocalActivity = staticCompositionLocalOf<FragmentActivity> {
    error("No LocalActivity provided!")
}

class MainActivity : FragmentActivity() {
    private val pinFinishedState = mutableStateOf(false)
    private var isLaunchingExternalPicker = false

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations && !isLaunchingExternalPicker) {
            pinFinishedState.value = false
            ru.kot.sked.components.BirthdayHelper.confettiShownThisSession = false
        }
    }

    override fun onResume() {
        super.onResume()
        isLaunchingExternalPicker = false
    }

    private fun createNotificationChannel() {
        val name = getString(R.string.data_update_channel_name)
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel("data_update", name, importance)
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.util.Log.e("SKED_CRASH", "FATAL CRASH on thread $thread", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
        super.onCreate(savedInstanceState)
        createNotificationChannel()
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                1
            )
        }
        val picker = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri ?: run {
                modalDialogStateLive.postValue(false)
                return@registerForActivityResult
            }
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (originalBitmap != null) {
                    val maxDim = maxOf(originalBitmap.width, originalBitmap.height)
                    val factor = if (maxDim > 512) 512f / maxDim else 1f
                    val scaledBitmap = Bitmap.createScaledBitmap(
                        originalBitmap,
                        (originalBitmap.width * factor).toInt().coerceAtLeast(1),
                        (originalBitmap.height * factor).toInt().coerceAtLeast(1),
                        true
                    )

                    // Always save locally to internal storage
                    val localAvatarFile = File(filesDir, "custom_avatar.png")
                    localAvatarFile.outputStream().use { out ->
                        scaledBitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
                    }
                    authPrefs.save(
                        "has_custom_avatar" to true,
                        "avatar_timestamp" to System.currentTimeMillis()
                    )

                    val isGosuslugi = authPrefs.get<String>("cookie") != null
                    if (isGosuslugi) {
                        modalDialogStateLive.postValue(false)
                        avatarTriggerLive.postValue(avatarTriggerLive.value?.not() ?: true)
                    } else {
                        val byteOutputStream = ByteArrayOutputStream()
                        scaledBitmap.compress(Bitmap.CompressFormat.PNG, 100, byteOutputStream)
                        val requestFile = RequestBody.create(
                            MediaType.parse("image/png"),
                            byteOutputStream.toByteArray()
                        )
                        val part = MultipartBody.Part.createFormData("file", "avatar.png", requestFile)

                        val upload = {
                            DataService.secondaryApi.uploadAvatar(
                                "Bearer ${DataService.token}",
                                DataService.profile.children[DataService.currentProfile].contingentGuid,
                                part
                            ).baseEnqueueOrNull {
                                DataService.updateAvatars {
                                    modalDialogStateLive.postValue(false)
                                    avatarTriggerLive.postValue(avatarTriggerLive.value?.not() ?: true)
                                }
                            }
                        }

                        DataService.run {
                            if (avatars.isNotEmpty()) {
                                secondaryApi.deleteAvatar(
                                    "Bearer $token",
                                    profile.children[currentProfile].contingentGuid,
                                    avatars.first().id.toString()
                                ).baseEnqueueOrNull {
                                    upload()
                                }
                            } else upload()
                        }
                    }
                } else {
                    modalDialogStateLive.postValue(false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                modalDialogStateLive.postValue(false)
            }
        }
        launchPickerLive.postValue {
            isLaunchingExternalPicker = true
            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        val wallpaperPicker = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri ?: return@registerForActivityResult
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (originalBitmap != null) {
                    val maxDim = maxOf(originalBitmap.width, originalBitmap.height)
                    val factor = if (maxDim > 1080) 1080f / maxDim else 1f
                    val scaledBitmap = Bitmap.createScaledBitmap(
                        originalBitmap,
                        (originalBitmap.width * factor).toInt().coerceAtLeast(1),
                        (originalBitmap.height * factor).toInt().coerceAtLeast(1),
                        true
                    )

                    val wallpaperFile = File(filesDir, "custom_wallpaper.png")
                    wallpaperFile.outputStream().use { out ->
                        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }

                    val seedColor = extractDominantColor(scaledBitmap)
                    mainPrefs.save(
                        "has_custom_wallpaper" to true,
                        "wallpaper_seed_color" to seedColor,
                        "wallpaper_timestamp" to System.currentTimeMillis()
                    )
                    wallpaperSeedColorLive.postValue(seedColor)
                    hasCustomWallpaperLive.postValue(true)
                    wallpaperTriggerLive.postValue(System.currentTimeMillis())
                    wallpaperPreviewLive.postValue(true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        launchWallpaperPickerLive.postValue {
            isLaunchingExternalPicker = true
            wallpaperPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        val wpFile = File(filesDir, "custom_wallpaper.png")
        val hasWp = (mainPrefs.get<Boolean>("has_custom_wallpaper") ?: false) && wpFile.exists()
        hasCustomWallpaperLive.value = hasWp
        wallpaperBlurLive.value = mainPrefs.get<Float>("wallpaper_blur") ?: 12f
        wallpaperDimLive.value = mainPrefs.get<Float>("wallpaper_dim") ?: 0.40f
        useWallpaperColorLive.value = mainPrefs.get<Boolean>("use_wallpaper_colors") ?: true
        if (hasWp) {
            try {
                val bmp = BitmapFactory.decodeFile(wpFile.absolutePath)
                if (bmp != null) {
                    val seed = extractDominantColor(bmp)
                    mainPrefs.save("wallpaper_seed_color" to seed)
                    wallpaperSeedColorLive.value = seed
                } else {
                    wallpaperSeedColorLive.value = mainPrefs.get<Int>("wallpaper_seed_color")
                }
            } catch (_: Exception) {
                wallpaperSeedColorLive.value = mainPrefs.get<Int>("wallpaper_seed_color")
            }
        } else {
            wallpaperSeedColorLive.value = mainPrefs.get<Int>("wallpaper_seed_color")
        }
        wallpaperTriggerLive.value = mainPrefs.get<Long>("wallpaper_timestamp") ?: 0L

        colorSchemeLive.value = mainPrefs.get("theme") ?: -1
        darkThemeLive.value = mainPrefs.get("is_dark_theme")
        isAmoledLive.value = mainPrefs.get("is_amoled") ?: false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val cur = display?.mode
                val modes = display?.supportedModes?.filter {
                    cur == null || (it.physicalWidth == cur.physicalWidth && it.physicalHeight == cur.physicalHeight)
                }
                val maxMode = modes?.maxByOrNull { it.refreshRate }
                if (maxMode != null) {
                    val lp = window.attributes
                    lp.preferredDisplayModeId = maxMode.modeId
                    lp.preferredRefreshRate = maxMode.refreshRate
                    window.attributes = lp
                }
            } catch (_: Exception) {}
        }

        if (ru.kot.sked.security.GosuslugiVault.hasCredentials(this) &&
            mainPrefs.get<Boolean>("pin_disabled_manually") != true &&
            mainPrefs.get<Boolean>("has_pin") != true
        ) {
            val vaultPin = ru.kot.sked.security.GosuslugiVault.getDecryptedPin(this)
            if (vaultPin != null) {
                mainPrefs.save("has_pin" to true, "pin" to vaultPin, "first_launch" to false)
            } else {
                mainPrefs.save("has_pin" to true, "first_launch" to false)
            }
        }
        if ((mainPrefs.get<Boolean>("has_pin") == true || ru.kot.sked.security.GosuslugiVault.hasCredentials(this)) &&
            mainPrefs.get<Boolean>("biometric_disabled_manually") != true
        ) {
            mainPrefs.save("biometric" to true)
        }

        val isGosuslugiAuth = authPrefs.get<String>("cookie") != null && !isDemo
        if (authPrefs.get<Boolean>("auth") == true) {
            screenLive.value = Screen.MainNav
            if (isDemo) {
                DataService.subsystem = Diary.MES
                DataService.run { loadDemoCache() }
                DataService.loadedEverything.value = true
                DataService.loadingStarted = true
            } else if (isGosuslugiAuth) {
                DataService.token = "gosuslugi"
                DataService.subsystem = Diary.MES
                val hasCache = ru.kot.sked.network.gosuslugi.GosuslugiDataBridge.loadFromCache(this)
                if (hasCache && DataService.hasProfile && DataService.hasMarksSubject && DataService.hasHomeworks) {
                    DataService.loadedEverything.value = true
                    DataService.loadingStarted = true
                }
                ru.kot.sked.network.gosuslugi.GosuslugiDataBridge.fetchAndApply(this) {}
            } else {
                if (cachePrefs.get<Long>("age")?.let { (System.currentTimeMillis() - it) < 86400000 } == true) {
                    DataService.subsystem = Diary.values()[authPrefs.get<Int>("subsystem") ?: 0]
                    DataService.loadFromCache { cachePrefs.get<String>(it) ?: "" }
                    DataService.loadedEverything.value = true
                    DataService.loadingStarted = true
                }
            }
        } else {
            screenLive.value = Screen.Login
        }

        enableEdgeToEdge()
        setContent {
            val colorScheme by colorSchemeLive.observeAsState(-1)
            val isSystemDark = isSystemInDarkTheme()
            val darkThemePref by darkThemeLive.observeAsState(null)
            val darkTheme = darkThemePref ?: isSystemDark
            val isAmoled by isAmoledLive.observeAsState(false)
            val hasCustomWallpaper by hasCustomWallpaperLive.observeAsState(false)
            val useWallpaperColor by useWallpaperColorLive.observeAsState(true)
            val wallpaperSeedColor by wallpaperSeedColorLive.observeAsState(null)

            val isWallpaperColorActive = hasCustomWallpaper && useWallpaperColor && wallpaperSeedColor != null

            val (lightScheme, darkScheme) = if (isWallpaperColorActive && wallpaperSeedColor != null) {
                dynamicColorSchemeFromSeed(wallpaperSeedColor!!)
            } else {
                val currentScheme = when {
                    colorScheme == -1 -> CustomColorScheme.Yellow
                    else -> CustomColorScheme.values()[colorScheme]
                }
                Pair(currentScheme.lightColorScheme, currentScheme.darkColorScheme)
            }

            OctoDiaryTheme(
                darkTheme = darkTheme,
                dynamicColor = colorScheme == -1 && !isWallpaperColorActive,
                lightScheme = lightScheme,
                darkScheme = darkScheme,
                isAmoled = isAmoled
            ) {
                MyApp(modifier = Modifier.fillMaxSize())
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
    @Composable
    private fun MyApp(
        modifier: Modifier = Modifier,
    ) {
        var title by rememberSaveable { mutableIntStateOf(R.string.app_name) }
        val pinFinished = pinFinishedState
        val isPinLocked = (mainPrefs.get<Boolean>("has_pin") == true ||
                (ru.kot.sked.security.GosuslugiVault.hasCredentials(this@MainActivity) &&
                        mainPrefs.get<Boolean>("pin_disabled_manually") != true)) && !pinFinished.value
        val currentScreen = screenLive.observeAsState()
        val showBottomSheet by modalBottomSheetStateLive.observeAsState()
        var hideBottomSheet by remember { mutableStateOf(false) } // lol
        val bottomSheetContent by modalBottomSheetContentLive.observeAsState()
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val snackbarHostState = snackbarHostStateLive.value!!
        val rememberedNavController = rememberNavController()
        SideEffect {
            navControllerLive.value = rememberedNavController
        }
        val surfaceColor = MaterialTheme.colorScheme.surface
        val elevatedColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        var topAppBarColor by remember { mutableStateOf(surfaceColor) }
        val contentDependentAction = contentDependentActionLive.observeAsState()
        val showDialog = modalDialogStateLive.observeAsState()
        val dialogContent = modalDialogContentLive.observeAsState()
        val showFilter = showFilterLive.observeAsState(false)
        val launchUrl = launchUrlLive.observeAsState()
        val webViewDialog by webViewDialogLive.observeAsState()
        if (authPrefs.get<String>("access_token") != null) {
            if ((mainPrefs.get<Int>("version") ?: 25) <= 25) {
                modalDialogCloseListenerLive.value = {
                    mainPrefs.save("version" to BuildConfig.VERSION_CODE)
                    logOut("Migration from version 25")
                }
                modalDialogContentLive.value = {
                    MigrationDialog { modalDialogCloseListenerLive.value?.invoke() }
                }
                modalDialogStateLive.value = true
            } else if ((mainPrefs.get<Int>("version") ?: 31) <= 31) {
                logOut("Migration from version 31")
            } else if (mainPrefs.get<Int>("version") != BuildConfig.VERSION_CODE) {
                mainPrefs.save("version" to BuildConfig.VERSION_CODE)
            }
        } else if (mainPrefs.get<Int>("version") != BuildConfig.VERSION_CODE) {
            mainPrefs.save("version" to BuildConfig.VERSION_CODE)
        }

        if (launchUrl.value != null) {
            isLaunchingExternalPicker = true
            val tabIntent = CustomTabsIntent.Builder().build()
            tabIntent.launchUrl(LocalContext.current, launchUrl.value!!)
            launchUrlLive.postValue(null)
        }

        SideEffect {
            rememberedNavController.addOnDestinationChangedListener { _, destination, _ ->
                topAppBarColor =
                    (if (destination.route == NavSection.Daybook.route || destination.route == NavSection.Homeworks.route) elevatedColor else surfaceColor)
            }
        }

        val intentData = intent.dataString
        intent.setData(null)
        if (intentData != null) {
            if (authPrefs.get<Boolean>("auth") == true) {
                LaunchedEffect(Unit) {
                    snackbarHostState.showSnackbar(getString(R.string.already_auth))
                }
            } else {
                screenLive.value = Screen.Callback
            }
        }

        var localLoadedState by remember { mutableStateOf(DataService.loadedEverything.value) }
        var settingsShown by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            if (ru.kot.sked.components.BirthdayHelper.isTodayBirthday(this@MainActivity)) {
                val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
                val lastCelebrated = mainPrefs.get<String>("last_birthday_celebrated")
                if (lastCelebrated != today) {
                    mainPrefs.save("last_birthday_celebrated" to today)
                    triggerBirthdayConfettiLive.postValue(System.currentTimeMillis())
                }
            }
        }
        LaunchedEffect(rememberCoroutineScope()) {
            snapshotFlow { DataService.loadedEverything.value }.onEach { localLoadedState = it }
                .launchIn(this)
        }

        LaunchedEffect(webViewDialog) {
            hideBottomSheet = webViewDialog != null
        }

        val hasCustomWallpaper by hasCustomWallpaperLive.observeAsState(false)
        val wallpaperBlur by wallpaperBlurLive.observeAsState(12f)
        val wallpaperDim by wallpaperDimLive.observeAsState(0.40f)
        val wallpaperTrigger by wallpaperTriggerLive.observeAsState(0L)

        val wallpaperBitmap = remember(wallpaperTrigger, hasCustomWallpaper, wallpaperBlur > 0.5f) {
            if (hasCustomWallpaper) {
                val file = File(filesDir, "custom_wallpaper.png")
                if (file.exists()) {
                    try {
                        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeFile(file.absolutePath, boundsOptions)
                        val sampleSize = (maxOf(boundsOptions.outWidth, boundsOptions.outHeight) / 1280).coerceAtLeast(1)
                        val options = BitmapFactory.Options().apply {
                            inSampleSize = sampleSize
                        }
                        BitmapFactory.decodeFile(file.absolutePath, options)?.asImageBitmap()
                    } catch (e: Exception) {
                        null
                    }
                } else null
            } else null
        }
        val shownWallpaper by produceState<androidx.compose.ui.graphics.ImageBitmap?>(wallpaperBitmap, wallpaperBitmap, wallpaperBlur, wallpaperDim) {
            val src = wallpaperBitmap
            if (src == null) { value = null; return@produceState }
            value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                bakeWallpaper(src.asAndroidBitmap(), wallpaperBlur, wallpaperDim).asImageBitmap()
            }
        }
        val isWallpaperActive = wallpaperBitmap != null && hasCustomWallpaper

        CompositionLocalProvider(LocalActivity provides this) {
            Box(Modifier.fillMaxSize()) {
                if (isWallpaperActive) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer()
                    ) {
                        Image(
                            bitmap = shownWallpaper ?: wallpaperBitmap!!,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            filterQuality = androidx.compose.ui.graphics.FilterQuality.High,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                val previewing by wallpaperPreviewLive.observeAsState(false)
                LaunchedEffect(previewing) { if (previewing) settingsShown = false }
                Scaffold(
                    modifier = modifier,
                    containerColor = if (isWallpaperActive) Color.Transparent else MaterialTheme.colorScheme.background,
                    topBar = {
                    Column {
                        TopAppBar(
                            navigationIcon = {
                                if (currentScreen.value == Screen.Login && authPrefs.get<Boolean>("auth") == true) {
                                    IconButton(onClick = { screenLive.postValue(Screen.MainNav) }) {
                                        Icon(
                                            Icons.AutoMirrored.Rounded.ArrowBack,
                                            stringResource(R.string.back)
                                        )
                                    }
                                }
                            },
                            title = {
                                AnimatedContent(targetState = title, label = "title_anim") {
                                    if ((currentScreen.value == Screen.MainNav && localLoadedState) || currentScreen.value != Screen.MainNav) {
                                        Text(stringResource(it))
                                    } else {
                                        Text(stringResource(R.string.app_name))
                                    }
                                }
                            }, actions = {
                                if (mainPrefs.get<Boolean>("force_debug") == true) {
                                    DebugMenu(this@MainActivity)
                                }
                                if (localLoadedState && currentScreen.value == Screen.MainNav && !isPinLocked) {
                                    val currentRoute =
                                        rememberedNavController.currentBackStackEntryAsState().value?.destination?.route
                                    AnimatedVisibility(currentRoute == NavSection.Profile.route) {
                                        Row(Modifier) {
                                            IconButton(onClick = {
                                                modalDialogContentLive.value = { ProfileChooser() }
                                                modalDialogStateLive.postValue(true)
                                            }) {
                                                Icon(
                                                    Icons.Rounded.Groups,
                                                    stringResource(id = R.string.choose_context_profile)
                                                )
                                            }
                                            IconButton(onClick = { settingsShown = true }) {
                                                Icon(
                                                    Icons.Rounded.Settings,
                                                    stringResource(id = R.string.settings)
                                                )
                                            }
                                        }
                                    }
                                    AnimatedVisibility(currentRoute == NavSection.Daybook.route || currentRoute == NavSection.Homeworks.route) {
                                        Row {
                                            IconButton(onClick = {
                                                modalDialogContentLive.value = { DayChooser() }
                                                modalDialogStateLive.postValue(true)
                                            }) {
                                                Icon(
                                                    Icons.Rounded.CalendarMonth,
                                                    stringResource(id = R.string.by_date)
                                                )
                                            }
                                        }
                                    }
                                    AnimatedVisibility(showFilter.value) {
                                        var expanded by remember {
                                            mutableStateOf(false)
                                        }
                                        Box(contentAlignment = Alignment.Center) {
                                            val icon =
                                                contentDependentActionIconLive.observeAsState(Icons.Rounded.FilterAlt)
                                            IconButton(onClick = { expanded = !expanded }) {
                                                AnimatedContent(
                                                    targetState = icon.value,
                                                    label = "action_icon_anim"
                                                ) {
                                                    Icon(it, "action")
                                                }
                                            }
                                            DropdownMenu(expanded, { expanded = false }) {
                                                contentDependentAction.value?.invoke()
                                            }

                                        }
                                    }
                                } else if (currentScreen.value == Screen.Login) {
                                    var expanded by remember { mutableStateOf(false) }
                                    var showAboutDialog by remember { mutableStateOf(false) }
                                    IconButton(onClick = { expanded = !expanded }) {
                                        Icon(
                                            Icons.Rounded.MoreVert,
                                            stringResource(R.string.menu)
                                        )
                                    }
                                    DropdownMenu(expanded, { expanded = false }) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.log_in_by_token)) },
                                            onClick = {
                                                modalDialogContentLive.value = { TokenLogin() }
                                                modalDialogStateLive.postValue(true)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.about)) },
                                            onClick = { showAboutDialog = true })
                                    }
                                    AnimatedVisibility(showAboutDialog) {
                                        Dialog(
                                            onDismissRequest = { showAboutDialog = false },
                                            properties = DialogProperties(
                                                usePlatformDefaultWidth = false
                                            )
                                        ) {
                                            Surface(Modifier.fillMaxSize()) {
                                                Column(Modifier.verticalScroll(rememberScrollState())) {
                                                    IconButton(
                                                        onClick = { showAboutDialog = false },
                                                        Modifier.padding(8.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.AutoMirrored.Rounded.ArrowBack,
                                                            stringResource(R.string.back)
                                                        )
                                                    }
                                                    About()
                                                }
                                            }
                                        }
                                    }
                                }
                            }, colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = if (isWallpaperActive) MaterialTheme.colorScheme.surface.copy(alpha = 0.5f) else topAppBarColor
                            )
                        )
                    }
                }, snackbarHost = { SnackbarHost(hostState = snackbarHostState) }, bottomBar = {
                    if ((currentScreen.value != Screen.MainNav) || !localLoadedState || isPinLocked) return@Scaffold
                    if (previewing && isWallpaperActive) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Размытие")
                                androidx.compose.material3.Slider(
                                    value = wallpaperBlur,
                                    valueRange = 0f..25f,
                                    onValueChange = { wallpaperBlurLive.value = it },
                                    onValueChangeFinished = { mainPrefs.save("wallpaper_blur" to (wallpaperBlurLive.value ?: 12f)) }
                                )
                                Text("Затемнение")
                                androidx.compose.material3.Slider(
                                    value = wallpaperDim,
                                    valueRange = 0f..0.8f,
                                    onValueChange = { wallpaperDimLive.value = it },
                                    onValueChangeFinished = { mainPrefs.save("wallpaper_dim" to (wallpaperDimLive.value ?: 0.4f)) }
                                )
                                androidx.compose.material3.Button(
                                    onClick = { wallpaperPreviewLive.value = false },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Готово") }
                            }
                        }
                        return@Scaffold
                    }
                    NavigationBar(
                        containerColor = if (isWallpaperActive) MaterialTheme.colorScheme.surface.copy(alpha = 0.65f) else NavigationBarDefaults.containerColor
                    ) {
                        val navBackStackEntry by rememberedNavController.currentBackStackEntryAsState()
                        val currentDestination = navBackStackEntry?.destination
                        NavSection.values().forEach {
                            val selected =
                                currentDestination?.hierarchy?.any { destination -> destination.route == it.route } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    if (it == NavSection.Homeworks) {
                                        showFilterLive.postValue(true)
                                    } else {
                                        showFilterLive.postValue(false)
                                    }
                                    rememberedNavController.navigate(it.route) {
                                        popUpTo(rememberedNavController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(it.icon, stringResource(id = it.title))
                                },
                                label = {
                                    Text(
                                        stringResource(id = it.title),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                })
                        }
                    }
                }) { padding ->
                    Surface(color = Color.Transparent) {
                        title = when (currentScreen.value!!) {
                            Screen.Login -> {
                                GosuslugiLoginScreen(Modifier.padding(padding))
                                R.string.log_in
                            }

                            Screen.Callback -> {
                                val uri = Uri.parse(intentData)
                                val callbackType =
                                    CallbackType.values().firstOrNull { it.host == uri.host }
                                val code = uri.getQueryParameter("code")
                                val subsystem = uri.getQueryParameter("system")?.toIntOrNull()
                                if (code != null && callbackType != null) {
                                    CallbackScreen(code, callbackType, subsystem)
                                } else {
                                    screenLive.postValue(Screen.Login)
                                }
                                R.string.log_in
                            }

                            Screen.MainNav -> {
                                NavScreen(Modifier.padding(padding), pinFinished, rememberedNavController)
                                val navBackStackEntry by rememberedNavController.currentBackStackEntryAsState()
                                val currentRoute = navBackStackEntry?.destination?.route
                                NavSection.values().firstOrNull { it.route == currentRoute }?.title
                                    ?: R.string.app_name
                            }
                        }
                    }
                    if (showBottomSheet == true && !hideBottomSheet) {
                        ModalBottomSheet(
                            onDismissRequest = { modalBottomSheetStateLive.postValue(false) },
                            sheetState = sheetState
                        ) {
                            bottomSheetContent?.invoke()
                        }
                    }
                    if (showDialog.value == true) {
                        Dialog(onDismissRequest = {
                            modalDialogStateLive.postValue(false)
                            modalDialogCloseListenerLive.value?.invoke()
                            modalDialogCloseListenerLive.value = {}
                        }) {
                            Card(
                                Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.extraLarge,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                            ) {
                                dialogContent.value?.invoke()
                            }
                        }
                    }
                    AnimatedVisibility(visible = settingsShown) {
                        SettingsDialog { settingsShown = false }
                    }
                    ru.kot.sked.components.ConfettiOverlay()
                }
                if (webViewDialog != null) {
                    webViewDialog?.invoke()
                }
            }
        }
    }

    @Preview(
        name = "Not Night", locale = "ru"
    )
    @Preview(
        name = "Night", uiMode = Configuration.UI_MODE_NIGHT_YES, locale = "ru"
    )
    @Composable
    fun AppPreview() {
        OctoDiaryTheme {
            MyApp()
        }
    }
}

private fun bakeWallpaper(src: Bitmap, blur: Float, dim: Float): Bitmap {
    var result: Bitmap = src
    if (blur > 0.5f) {
        val smallW = 320
        val smallH = (src.height * smallW.toFloat() / src.width).toInt().coerceAtLeast(2)
        val small = Bitmap.createScaledBitmap(src, smallW, smallH, true)
        val px = IntArray(smallW * smallH)
        small.getPixels(px, 0, smallW, 0, 0, smallW, smallH)
        val radius = (blur / 25f * 14f).toInt().coerceAtLeast(1)
        repeat(3) { boxBlur(px, smallW, smallH, radius) }
        small.setPixels(px, 0, smallW, 0, 0, smallW, smallH)
        val targetW = minOf(src.width, 720)
        val targetH = (src.height * targetW.toFloat() / src.width).toInt().coerceAtLeast(2)
        result = Bitmap.createScaledBitmap(small, targetW, targetH, true)
    }
    val out = result.copy(Bitmap.Config.ARGB_8888, true)
    if (dim > 0f) {
        android.graphics.Canvas(out).drawColor(
            android.graphics.Color.argb((dim.coerceIn(0f, 1f) * 255).toInt(), 0, 0, 0)
        )
    }
    return out
}

private fun boxBlur(px: IntArray, w: Int, h: Int, r: Int) {
    val tmp = IntArray(px.size)
    fun pass(inp: IntArray, outp: IntArray, len: Int, count: Int, stride: Int, step: Int) {
        for (l in 0 until count) {
            val base = l * stride
            var sr = 0; var sg = 0; var sb = 0
            val win = 2 * r + 1
            for (k in -r..r) {
                val c = inp[base + k.coerceIn(0, len - 1) * step]
                sr += (c shr 16) and 255; sg += (c shr 8) and 255; sb += c and 255
            }
            for (k in 0 until len) {
                outp[base + k * step] = (0xFF shl 24) or ((sr / win) shl 16) or ((sg / win) shl 8) or (sb / win)
                val add = inp[base + (k + r + 1).coerceIn(0, len - 1) * step]
                val rem = inp[base + (k - r).coerceIn(0, len - 1) * step]
                sr += ((add shr 16) and 255) - ((rem shr 16) and 255)
                sg += ((add shr 8) and 255) - ((rem shr 8) and 255)
                sb += (add and 255) - (rem and 255)
            }
        }
    }
    pass(px, tmp, w, h, w, 1)
    pass(tmp, px, h, w, 1, w)
}
