package ru.kot.sked.components.settings

import android.graphics.BitmapFactory
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.kot.sked.LocalActivity
import ru.kot.sked.R
import ru.kot.sked.colorSchemeLive
import ru.kot.sked.components.SwitchPreference
import ru.kot.sked.components.ThemeCard
import ru.kot.sked.darkThemeLive
import ru.kot.sked.get
import ru.kot.sked.hasCustomWallpaperLive
import ru.kot.sked.isAmoledLive
import ru.kot.sked.launchWallpaperPickerLive
import ru.kot.sked.mainPrefs
import ru.kot.sked.save
import ru.kot.sked.ui.theme.CustomColorScheme
import ru.kot.sked.useWallpaperColorLive
import ru.kot.sked.wallpaperBlurLive
import ru.kot.sked.wallpaperDimLive
import ru.kot.sked.wallpaperTriggerLive
import java.io.File

@Composable
fun Appearance() {
    val activity = LocalActivity.current
    val isSystemDark = isSystemInDarkTheme()
    val darkThemePref by darkThemeLive.observeAsState(isSystemDark)
    val isDark = darkThemePref ?: isSystemDark
    val darkThemeState = remember(isDark) { mutableStateOf(isDark) }
    var selectedTheme by remember { mutableStateOf(colorSchemeLive.value ?: -1) }

    LazyRow {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            item {
                MaterialTheme.colorScheme.run {
                    val isDynamic = selectedTheme == -1
                    ThemeCard(
                        isDynamic,
                        if (isDynamic) primary else MaterialTheme.colorScheme.surface,
                        if (isDynamic) secondary else MaterialTheme.colorScheme.surface,
                        if (isDynamic) surfaceVariant else MaterialTheme.colorScheme.surface,
                        Modifier.padding(start = 8.dp),
                        true
                    ) {
                        colorSchemeLive.postValue(-1)
                        selectedTheme = -1
                        activity.mainPrefs.save("theme" to -1)
                    }
                }
            }
        }
        items(CustomColorScheme.values()) {
            val scheme = remember(isDark) {
                if (isDark) it.darkColorScheme else it.lightColorScheme
            }
            scheme.run {
                ThemeCard(
                    selectedTheme == it.ordinal,
                    primary,
                    secondary,
                    surfaceVariant
                ) {
                    colorSchemeLive.postValue(it.ordinal)
                    selectedTheme = it.ordinal
                    activity.mainPrefs.save("theme" to it.ordinal)
                }
            }
        }
    }

    SwitchPreference(
        title = stringResource(R.string.dark_theme),
        listenState = darkThemeState
    ) {
        darkThemeState.value = it
        darkThemeLive.value = it
        activity.mainPrefs.save("is_dark_theme" to it)
    }

    val isAmoledPref by isAmoledLive.observeAsState(activity.mainPrefs.get("is_amoled") ?: false)
    val isAmoledState = remember(isAmoledPref) { mutableStateOf(isAmoledPref) }
    if (isDark) {
        SwitchPreference(
            title = "AMOLED (глубокий чёрный)",
            description = "Чистый чёрный фон для экономии батареи",
            listenState = isAmoledState
        ) {
            isAmoledState.value = it
            isAmoledLive.value = it
            activity.mainPrefs.save("is_amoled" to it)
        }
    }

    val hapticEnabled = remember {
        mutableStateOf(activity.mainPrefs.get<Boolean>("haptic_feedback") != false)
    }
    SwitchPreference(
        title = "Тактильный отклик (вибрация)",
        description = "Вибрация при кликах и отметке заданий",
        listenState = hapticEnabled
    ) {
        hapticEnabled.value = it
        activity.mainPrefs.save("haptic_feedback" to it)
    }

    // --- Фоновое изображение (Пользовательские обои) ---
    val hasCustomWallpaper by hasCustomWallpaperLive.observeAsState(false)
    val wallpaperBlur by wallpaperBlurLive.observeAsState(12f)
    val wallpaperDim by wallpaperDimLive.observeAsState(0.40f)
    val useWallpaperColor by useWallpaperColorLive.observeAsState(true)
    val wallpaperTrigger by wallpaperTriggerLive.observeAsState(0L)

    Text(
        text = "Фоновое изображение",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp)
    )

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilledTonalButton(
            onClick = {
                launchWallpaperPickerLive.value?.invoke()
            },
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                Icons.Rounded.Wallpaper,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(if (hasCustomWallpaper) "Сменить фото" else "Выбрать фото")
        }

        if (hasCustomWallpaper) {
            OutlinedButton(
                onClick = {
                    try {
                        val file = File(activity.filesDir, "custom_wallpaper.png")
                        if (file.exists()) file.delete()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    activity.mainPrefs.save(
                        "has_custom_wallpaper" to false,
                        "wallpaper_timestamp" to System.currentTimeMillis()
                    )
                    hasCustomWallpaperLive.postValue(false)
                    wallpaperTriggerLive.postValue(System.currentTimeMillis())
                }
            ) {
                Icon(
                    Icons.Rounded.DeleteOutline,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("Удалить")
            }
        }
    }

    if (hasCustomWallpaper) {
        val wallpaperBitmap = remember(wallpaperTrigger, hasCustomWallpaper) {
            val file = File(activity.filesDir, "custom_wallpaper.png")
            if (file.exists()) {
                try {
                    val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(file.absolutePath, boundsOptions)
                    val sampleSize = (maxOf(boundsOptions.outWidth, boundsOptions.outHeight) / 540).coerceAtLeast(1)
                    val options = BitmapFactory.Options().apply {
                        inSampleSize = sampleSize
                    }
                    BitmapFactory.decodeFile(file.absolutePath, options)?.asImageBitmap()
                } catch (e: Exception) {
                    null
                }
            } else null
        }

        var currentBlur by remember(wallpaperBlur) { mutableStateOf(wallpaperBlur) }
        var currentDim by remember(wallpaperDim) { mutableStateOf(wallpaperDim) }

        // Интерактивный предпросмотр (Live Preview)
        Text(
            text = "Предпросмотр на фоне",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 6.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(210.dp)
                .clip(MaterialTheme.shapes.extraLarge)
        ) {
            if (wallpaperBitmap != null) {
                Image(
                    bitmap = wallpaperBitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (currentBlur > 0.5f) Modifier.blur(currentBlur.dp) else Modifier
                        )
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }

            if (currentDim > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = currentDim.coerceIn(0f, 1f)))
                )
            }

            // Образец карточки задания
            Card(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Русский язык",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "каб. 204",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Упр. 142 — составить сложноподчинённые предложения",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Пример отображения задания",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Ползунок размытия (Blur)
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Размытие фона", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Сглаживает детали для читаемости текста",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "${currentBlur.toInt()} dp",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Slider(
                value = currentBlur,
                onValueChange = {
                    currentBlur = it
                    wallpaperBlurLive.value = it
                    activity.mainPrefs.save("wallpaper_blur" to it)
                },
                valueRange = 0f..30f,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Ползунок затемнения (Dim)
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Затемнение фона", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Повышает контрастность карточек и расписания",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "${(currentDim * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Slider(
                value = currentDim,
                onValueChange = {
                    currentDim = it
                    wallpaperDimLive.value = it
                    activity.mainPrefs.save("wallpaper_dim" to it)
                },
                valueRange = 0f..0.80f,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Адаптивные цвета под обои
        val useWpColorState = remember(useWallpaperColor) { mutableStateOf(useWallpaperColor) }
        SwitchPreference(
            title = "Цвета интерфейса под фото",
            description = "Акцентные цвета приложения адаптируются под фон",
            listenState = useWpColorState
        ) {
            useWpColorState.value = it
            useWallpaperColorLive.value = it
            activity.mainPrefs.save("use_wallpaper_colors" to it)
        }
    }
}
