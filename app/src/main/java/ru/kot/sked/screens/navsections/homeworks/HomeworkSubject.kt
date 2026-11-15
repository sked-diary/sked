package ru.kot.sked.screens.navsections.homeworks

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import ru.kot.sked.DataService
import ru.kot.sked.R
import ru.kot.sked.authPrefs
import ru.kot.sked.get
import ru.kot.sked.performHapticFeedback
import java.util.regex.Pattern

private fun openHomeworkUrl(context: Context, url: String) {
    try {
        val uri = Uri.parse(url)
        val customTabsIntent = CustomTabsIntent.Builder().setShowTitle(true).build()
        customTabsIntent.launchUrl(context, uri)
    } catch (_: Exception) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Не удалось открыть ссылку", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun HomeworkSubject(homeworks: List<ru.kot.sked.models.homeworks2.Homework>) {
    Column {
        Text(
            homeworks.first().subjectName,
            Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
            style = MaterialTheme.typography.titleMedium
        )
        val context = LocalContext.current
        val isGosuslugi = context.authPrefs.get<String>("cookie") != null
        val hwPrefs = remember { context.getSharedPreferences("homework_done", Context.MODE_PRIVATE) }

        homeworks.forEach { it ->
            val hwKey = remember(it) { "${it.subjectName}_${it.date}_${it.description.hashCode()}" }
            var isDone by rememberSaveable(hwKey) {
                mutableStateOf(if (isGosuslugi) hwPrefs.getBoolean(hwKey, it.isDone) else it.isDone)
            }
            var expanded by remember { mutableStateOf(false) }

            val desc = it.description
            val linkColor = MaterialTheme.colorScheme.primary
            val annotatedText = remember(desc, linkColor) {
                val urlPattern = Pattern.compile("(https?://[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]+)")
                val matcher = urlPattern.matcher(desc)
                buildAnnotatedString {
                    var lastIdx = 0
                    while (matcher.find()) {
                        val start = matcher.start()
                        val end = matcher.end()
                        val url = matcher.group()
                        append(desc.substring(lastIdx, start))
                        pushStringAnnotation(tag = "URL", annotation = url)
                        withStyle(
                            SpanStyle(
                                color = linkColor,
                                textDecoration = TextDecoration.Underline
                            )
                        ) {
                            append(url)
                        }
                        pop()
                        lastIdx = end
                    }
                    if (lastIdx < desc.length) {
                        append(desc.substring(lastIdx))
                    }
                }
            }

            val gMaterials = it.gosuslugiMaterials ?: emptyList()
            val textUrls = remember(desc) {
                val list = mutableListOf<String>()
                val m = Pattern.compile("(https?://[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]+)").matcher(desc)
                while (m.find()) {
                    val u = m.group()
                    if (gMaterials.none { mat -> mat.link?.contains(u) == true }) {
                        list.add(u)
                    }
                }
                list
            }

            Column(
                Modifier
                    .padding(
                        start = 8.dp, end = 16.dp, bottom = 16.dp
                    )
            ) {
                Column(Modifier.padding(start = 8.dp)) {
                    val mats = it.materials
                    if (!mats.isNullOrEmpty()) {
                        if (mats.any { m -> m.selectedMode == "learn" }) {
                            Text(
                                stringResource(
                                    R.string.learn_t,
                                    mats.count { m -> m.selectedMode == "learn" }
                                ),
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        if (mats.any { m -> m.selectedMode == "execute" }) {
                            Text(
                                stringResource(
                                    R.string.do_t,
                                    mats.count { m -> m.selectedMode == "execute" }
                                ),
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }

                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Checkbox(
                        checked = isDone,
                        onCheckedChange = { state ->
                            context.performHapticFeedback()
                            isDone = state
                            it.isDone = state
                            if (isGosuslugi) {
                                hwPrefs.edit().putBoolean(hwKey, state).apply()
                                val gHwId = it.gosuslugiId
                                val gIntervalId = it.gosuslugiIntervalId ?: hwPrefs.getString("interval_${gHwId ?: ""}", null)
                                if (!gHwId.isNullOrEmpty() && !gIntervalId.isNullOrEmpty()) {
                                    hwPrefs.edit().putBoolean("gosuslugi_$gHwId", state).apply()
                                    ru.kot.sked.network.gosuslugi.GosuslugiDataBridge.toggleHomework(context, gHwId, gIntervalId, state)
                                }
                            } else {
                                try {
                                    if (DataService.token.isNotEmpty()) {
                                        DataService.setHomeworkDoneState(it.homeworkEntryStudentId, state) {}
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        },
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    Column(
                        Modifier
                            .weight(1f)
                            .padding(start = 4.dp, top = 8.dp)
                    ) {
                        ClickableText(
                            text = annotatedText,
                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                            maxLines = if (!expanded) 3 else Int.MAX_VALUE,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.animateContentSize(),
                            onClick = { offset ->
                                annotatedText.getStringAnnotations(tag = "URL", start = offset, end = offset)
                                    .firstOrNull()?.let { annotation ->
                                        openHomeworkUrl(context, annotation.item)
                                    } ?: run {
                                        expanded = !expanded
                                    }
                            }
                        )

                        if (gMaterials.isNotEmpty() || textUrls.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                gMaterials.forEach { mat ->
                                    val link = mat.link
                                    val name = mat.name ?: "Прикрепленный файл"
                                    if (!link.isNullOrBlank()) {
                                        AttachmentChip(
                                            name = name,
                                            onClick = { openHomeworkUrl(context, link) }
                                        )
                                    }
                                }
                                textUrls.forEach { url ->
                                    AttachmentChip(
                                        name = url,
                                        isUrl = true,
                                        onClick = { openHomeworkUrl(context, url) }
                                    )
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
private fun AttachmentChip(
    name: String,
    isUrl: Boolean = false,
    onClick: () -> Unit
) {
    val isImage = name.endsWith(".png", true) || name.endsWith(".jpg", true) || name.endsWith(".jpeg", true)
    val isDoc = name.endsWith(".docx", true) || name.endsWith(".doc", true) || name.endsWith(".pdf", true) || name.endsWith(".txt", true)
    val icon: ImageVector = when {
        isImage -> Icons.Rounded.Image
        isDoc -> Icons.Rounded.Description
        isUrl -> Icons.Rounded.Link
        else -> Icons.Rounded.AttachFile
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                contentDescription = "Открыть",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
