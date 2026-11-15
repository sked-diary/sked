package ru.kot.sked.components.settings

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.kot.sked.BuildConfig
import ru.kot.sked.R
import ru.kot.sked.components.ChangelogDialog
import ru.kot.sked.get
import ru.kot.sked.mainPrefs
import ru.kot.sked.network.Developer
import ru.kot.sked.network.NetworkService
import ru.kot.sked.network.NetworkService.ExternalIntegrationConfig.CONTRIBUTORS_GITHUB_URL
import ru.kot.sked.network.NetworkService.ExternalIntegrationConfig.PYTHON_LIB_URL
import ru.kot.sked.network.NetworkService.ExternalIntegrationConfig.TELEGRAM_BOT_URL
import ru.kot.sked.openUri
import ru.kot.sked.save
import ru.kot.sked.ui.theme.OctoDiaryTheme

@Composable
fun About() {
    Column {
        Card()
        SkedInfo()
        AttributionCard()
        PrivacyPolicyCard()
    }
}

@Composable
private fun AttributionCard() {
    Card(
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainer),
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Favorite,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(16.dp))
                Text("Благодарности", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Спасибо Andrey Lyalkov (bxkr) за открытую кодовую базу OctoDiary и отличную основу дизайна интерфейса (лицензия MIT).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PrivacyPolicyCard() {
    var showPolicy by remember { mutableStateOf(false) }
    Card(
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainer),
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { showPolicy = true }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
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
                Text("Политика конфиденциальности", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Локальное хранение, шифрование Keystore, без передачи данных третьим лицам",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
    if (showPolicy) {
        PrivacyPolicyDialog { showPolicy = false }
    }
}

@Composable
private fun PrivacyPolicyDialog(onDismiss: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Политика конфиденциальности") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "Мобильное приложение Sked (ru.kot.sked) является независимым неофициальным клиентом с открытым исходным кодом для школьных электронных дневников (Моя Школа / Госуслуги).\n\n" +
                    "1. Разработчик не связан с Министерством цифрового развития или Минпросвещения РФ.\n\n" +
                    "2. Приложение не собирает персональные данные на сторонние серверы. У разработчика нет серверов или баз данных для сбора телеметрии.\n\n" +
                    "3. Все авторизационные данные (логин, пароль, 2FA) шифруются аппаратно чипом Android Keystore (AES-GCM 256) и хранятся исключительно на вашем устройстве.\n\n" +
                    "4. Обмен данными происходит напрямую с официальными серверами Госуслуг (esia.gosuslugi.ru, myschool.gosuslugi.ru) по защищенному протоколу HTTPS.\n\n" +
                    "5. Вы можете в любой момент удалить данные через настройки приложения или стандартным удалением приложения.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Понятно")
            }
        }
    )
}

@Composable
private fun SkedInfo() {
    val ctx = LocalContext.current
    var taps by remember { mutableStateOf(0) }
    var showLogs by remember { mutableStateOf(false) }
    if (showLogs) LogsDialog { showLogs = false }
    Card(
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainer),
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "О приложении Sked",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.clickable(
                    indication = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                ) {
                    if (ctx.mainPrefs.get<Boolean>("force_debug") == true) {
                        taps++
                        if (taps >= 5) { taps = 0; showLogs = true }
                    } else taps = 0
                }
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Быстрый неофициальный клиент электронного дневника (Моя Школа / Госуслуги) с поддержкой Material You, мгновенной загрузкой и локальным To-Do списком заданий.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun Card() {
    var isChangelogDialogShown by remember { mutableStateOf(false) }
    val context = LocalContext.current
    Card(
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainer),
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            var isDebug by remember {
                mutableStateOf(
                    context.mainPrefs.get<Boolean>("force_debug") ?: false
                )
            }
            Row {
                Box {
                    val debugOn = stringResource(id = R.string.debug_on)
                    val debugOff = stringResource(id = R.string.debug_off)
                    Box(
                        Modifier
                            .size(56.dp)
                            .align(Alignment.Center)
                            .clip(CircleShape)
                            .clickable {
                                isDebug = !isDebug
                                context.mainPrefs.save("force_debug" to isDebug)
                                if (isDebug)
                                    Toast.makeText(context, debugOn, Toast.LENGTH_SHORT).show()
                                else
                                    Toast.makeText(context, debugOff, Toast.LENGTH_SHORT).show()
                            }
                            .background(MaterialTheme.colorScheme.run { if (isDebug) onSecondary else secondary })
                    )
                    Icon(
                        painterResource(R.drawable.ic_launcher_foreground),
                        stringResource(R.string.app_name),
                        Modifier.size(64.dp),
                        MaterialTheme.colorScheme.run { if (isDebug) secondary else onSecondary }
                    )
                }
                Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.Center) {
                    Text(
                        stringResource(id = R.string.app_name),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Row {
                        Text(
                            BuildConfig.VERSION_NAME,
                            Modifier.padding(end = 2.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            stringResource(R.string.changes_label),
                            Modifier.clickable {
                                isChangelogDialogShown = true
                            },
                            textDecoration = TextDecoration.Underline,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
    AnimatedVisibility(isChangelogDialogShown) {
        ChangelogDialog { isChangelogDialogShown = false }
    }
}

@Composable
private fun Developers() {
    Row(
        Modifier.padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(Modifier.weight(1f))
        Text(
            stringResource(R.string.developers),
            Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.titleMedium
        )
        HorizontalDivider(Modifier.weight(1f))
    }
    Column(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Developer.developers.forEach {
                DevelopersGridItem(it)
            }
        }
        val bottomBoxShape = MaterialTheme.shapes.extraSmall.copy(
            bottomEnd = MaterialTheme.shapes.extraLarge.bottomEnd,
            bottomStart = MaterialTheme.shapes.extraLarge.bottomStart
        )
        val context = LocalContext.current
        Box(
            Modifier
                .padding(top = 2.dp)
                .border(2.dp, MaterialTheme.colorScheme.primaryContainer, bottomBoxShape)
                .background(Color.Transparent)
                .fillMaxWidth()
                .clickable { context.openUri(CONTRIBUTORS_GITHUB_URL) }
        ) {
            Text(
                stringResource(R.string.and_other_contributors),
                Modifier
                    .align(Alignment.Center)
                    .padding(vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun RowScope.DevelopersGridItem(developer: Developer) {
    val context = LocalContext.current
    Card(
        Modifier
            .weight(1f),
        shape = MaterialTheme.shapes.extraSmall,
        colors = CardDefaults
            .cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                developer.nickname,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 8.dp),
                minLines = 2,
                maxLines = 2
            )
            Row {
                if (developer.githubLink != null) {
                    FilledTonalIconButton(onClick = { context.openUri(developer.githubLink) }) {
                        Icon(
                            painterResource(id = R.drawable.github_24),
                            "GitHub",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                if (developer.telegramLink != null) {
                    FilledTonalIconButton(onClick = { context.openUri(developer.telegramLink) }) {
                        Icon(
                            painterResource(id = R.drawable.telegram_24),
                            "Telegram",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OtherProjects() {
    Column(Modifier.padding(16.dp)) {
        Row(Modifier.padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(Modifier.weight(1f))
            Text(
                stringResource(R.string.other_octodiary_projects),
                Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.titleMedium
            )
            HorizontalDivider(Modifier.weight(1f))
        }
        val context = LocalContext.current
        Card(
            colors = CardDefaults.cardColors(Color.Transparent),
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        0.5f to MaterialTheme.colorScheme.surfaceContainer,
                        1f to MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    MaterialTheme.shapes.extraLarge
                )
                .clip(MaterialTheme.shapes.extraLarge)
                .clickable {
                    context.openUri(TELEGRAM_BOT_URL)
                }
        ) {
            Row(
                Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.SmartToy,
                        stringResource(R.string.bot),
                        Modifier
                            .padding(end = 8.dp)
                            .size(40.dp)
                    )
                    Column {
                        Text(
                            stringResource(R.string.bot),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            stringResource(R.string.bot_description),
                            Modifier.padding(end = 8.dp)
                        )
                    }
                }
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowForward,
                    stringResource(R.string.open),
                    Modifier
                        .padding(end = 8.dp)
                        .size(32.dp),
                    MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
        Card(
            colors = CardDefaults.cardColors(Color.Transparent),
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .background(
                    Brush.linearGradient(
                        0.5f to MaterialTheme.colorScheme.surfaceContainer,
                        1f to MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    MaterialTheme.shapes.extraLarge
                )
                .clip(MaterialTheme.shapes.extraLarge)
                .clickable {
                    context.openUri(PYTHON_LIB_URL)
                }
        ) {
            Row(
                Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.Book,
                        stringResource(R.string.bot),
                        Modifier
                            .padding(end = 8.dp)
                            .size(40.dp)
                    )
                    Column {
                        Text(
                            stringResource(R.string.python_lib),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            stringResource(R.string.python_lib_description),
                            Modifier.padding(end = 8.dp)
                        )
                    }
                }
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowForward,
                    stringResource(R.string.open),
                    Modifier
                        .padding(end = 8.dp)
                        .size(32.dp),
                    MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}

@Preview
@Composable
fun AboutPreview() {
    OctoDiaryTheme {
        Surface {
            About()
        }
    }
}

@Composable
private fun LogsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val lines = ru.kot.sked.AppLog.lines
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Логи") },
        text = {
            val state = androidx.compose.foundation.lazy.rememberLazyListState()
            androidx.compose.runtime.LaunchedEffect(lines.size) {
                if (lines.isNotEmpty()) state.scrollToItem(lines.size - 1)
            }
            if (lines.isEmpty()) Text("Пока пусто")
            androidx.compose.foundation.lazy.LazyColumn(state = state) {
                items(lines.size) { i ->
                    Text(lines[i], style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 2.dp))
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = {
                val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                cm.setPrimaryClip(android.content.ClipData.newPlainText("logs", lines.joinToString("\n")))
                Toast.makeText(context, "Скопировано", Toast.LENGTH_SHORT).show()
            }) { Text("Копировать") }
        },
        dismissButton = {
            Row {
                androidx.compose.material3.TextButton(onClick = { lines.clear() }) { Text("Очистить") }
                androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Закрыть") }
            }
        }
    )
}
