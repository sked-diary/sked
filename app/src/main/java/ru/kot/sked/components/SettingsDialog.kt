package ru.kot.sked.components

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import ru.kot.sked.DataService
import ru.kot.sked.LocalActivity
import ru.kot.sked.R
import ru.kot.sked.components.settings.About
import ru.kot.sked.components.settings.Appearance
import ru.kot.sked.components.settings.Common
import ru.kot.sked.components.settings.Notifications
import ru.kot.sked.components.settings.Security
import ru.kot.sked.launchUrlLive
import ru.kot.sked.logOut
import ru.kot.sked.network.NetworkService
import ru.kot.sked.network.NetworkService.ExternalIntegrationConfig.TELEGRAM_REPORT_URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(onDismissRequest: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val defaultTitle = stringResource(R.string.settings)
    var selectScreen by remember { mutableStateOf(true) }
    var currentTitle by remember { mutableStateOf(defaultTitle) }

    Dialog(
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        ),
        onDismissRequest = {
            if (!selectScreen) {
                selectScreen = true
                currentTitle = defaultTitle
            } else {
                onDismissRequest()
            }
        }
    ) {
        BackHandler(enabled = !selectScreen) {
            selectScreen = true
            currentTitle = defaultTitle
        }
        Scaffold(
            topBar = {
                MediumTopAppBar(
                    title = {
                        if (selectScreen) {
                            Text(stringResource(R.string.settings))
                        } else {
                            Text(currentTitle)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (!selectScreen) {
                                selectScreen = true
                                currentTitle = defaultTitle
                            } else {
                                onDismissRequest()
                            }
                        }) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowBack,
                                stringResource(R.string.back)
                            )
                        }
                    }
                )
            }
        ) { padding ->
            Surface(
                Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(),
                ) {
                    val activity = LocalActivity.current
                    var currentScreen by remember { mutableStateOf<@Composable () -> Unit>({}) }

                    val enterTransition1 = remember {
                        slideInHorizontally(
                            tween(200)
                        ) { it }
                    }
                    val exitTransition1 = remember {
                        slideOutHorizontally(
                            tween(200)
                        ) { it }
                    }
                    val enterTransition2 = remember {
                        slideInHorizontally(
                            tween(200)
                        ) { -it }
                    }
                    val exitTransition2 = remember {
                        slideOutHorizontally(
                            tween(200)
                        ) { -it }
                    }

                    val SettingsSection: @Composable (icon: ImageVector, title: String, description: String, content: @Composable () -> Unit) -> Unit =
                        { icon, title, description, content ->
                            Card(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .clickable {
                                        selectScreen = false
                                        currentScreen = content
                                        currentTitle = title
                                    },
                                shape = MaterialTheme.shapes.extraLarge,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                            ) {
                                Row(
                                    Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        icon, title,
                                        Modifier
                                            .padding(horizontal = 8.dp)
                                            .size(25.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Column {
                                        Text(
                                            title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            lineHeight = 1.em
                                        )
                                        Text(
                                            description,
                                            lineHeight = 1.em,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                    AnimatedVisibility(
                        visible = !selectScreen,
                        enter = enterTransition1,
                        exit = exitTransition1
                    ) {
                        Column(
                            Modifier
                                .verticalScroll(rememberScrollState())
                        ) {
                            currentScreen()
                        }
                    }
                    AnimatedVisibility(
                        visible = selectScreen,
                        enter = enterTransition2,
                        exit = exitTransition2
                    ) {
                        Column(
                            Modifier
                                .verticalScroll(rememberScrollState())
                        ) {
                            SettingsSection(
                                Icons.Rounded.Settings,
                                stringResource(R.string.common),
                                stringResource(R.string.common_desc),
                            ) { Common() }
                            SettingsSection(
                                Icons.Rounded.Notifications,
                                stringResource(R.string.notifications),
                                stringResource(R.string.notifications_desc)
                            ) { Notifications() }
                            SettingsSection(
                                Icons.Rounded.Brush,
                                stringResource(R.string.appearance),
                                stringResource(R.string.appearance_desc)
                            ) { Appearance() }
                            SettingsSection(
                                Icons.Rounded.Lock,
                                stringResource(R.string.security),
                                stringResource(R.string.security_desc)
                            ) { Security() }
                            SettingsSection(
                                Icons.Rounded.Info,
                                stringResource(R.string.about),
                                stringResource(R.string.about_desc)
                            ) { About() }

                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                OutlinedButton(onClick = {
                                    onDismissRequest()
                                    activity.logOut("User initiated the log out from settings screen")
                                }) {
                                    Text(stringResource(R.string.log_out))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ThemeCard(
    selected: Boolean,
    top: Color,
    middle: Color,
    bottom: Color,
    modifier: Modifier = Modifier,
    showIcon: Boolean = false,
    onClick: () -> Unit,
) {
    Box(Modifier.clip(MaterialTheme.shapes.large)) {
        OutlinedCard(modifier.padding(8.dp), shape = MaterialTheme.shapes.extraLarge) {
            Box(Modifier.clickable(onClick = onClick)) {
                Column(
                    Modifier.border(
                        4.dp,
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.shapes.extraLarge
                    )
                ) {
                    listOf(top, middle, bottom).forEach {
                        AnimatedContent(targetState = it, label = "color_anim") { color ->
                            Box(
                                Modifier
                                    .size(width = 72.dp, height = 36.dp)
                                    .background(color)
                            )
                        }
                    }
                }
                if (showIcon) {
                    Icon(
                        Icons.Rounded.Image,
                        stringResource(R.string.wallpaper_dynamic_color),
                        Modifier
                            .align(Alignment.Center)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .padding(8.dp),
                        MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
        AnimatedVisibility(selected) {
            Icon(
                Icons.Rounded.Done,
                stringResource(R.string.wallpaper_dynamic_color),
                Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiary)
                    .size(18.dp)
                    .padding(2.dp),
                MaterialTheme.colorScheme.onTertiary
            )
        }
    }
}
