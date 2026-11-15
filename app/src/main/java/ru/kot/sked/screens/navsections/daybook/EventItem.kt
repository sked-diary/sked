package ru.kot.sked.screens.navsections.daybook

import android.webkit.URLUtil
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuOpen
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.kot.sked.DataService
import ru.kot.sked.R
import ru.kot.sked.components.MarkComp
import ru.kot.sked.formatToTime
import ru.kot.sked.getMarkConfig
import ru.kot.sked.modalBottomSheetContentLive
import ru.kot.sked.modalBottomSheetStateLive
import ru.kot.sked.models.events.Event
import ru.kot.sked.parseLongDate
import ru.kot.sked.snackbarHostStateLive
import ru.kot.sked.toTimeDisplay

@Composable
fun EventItem(event: Event, index: Int = -1, showLessonNumbers: Boolean = true) {
    var isExpanded by remember { mutableStateOf(false) }
    val enterTransition = remember {
        expandVertically(
            expandFrom = Alignment.Top, animationSpec = tween(200)
        )
    }
    val exitTransition = remember {
        shrinkVertically(
            shrinkTowards = Alignment.Top, animationSpec = tween(200)
        )
    }
    val markConfig = getMarkConfig()
    Column(Modifier.clickable {
        isExpanded = !isExpanded
    }) {
        Column(
            Modifier.padding(
                top = 16.dp, start = 16.dp, end = 16.dp
            )
        ) {
            Row(
                Modifier
                    .padding(
                        bottom = 16.dp
                    )
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    Modifier
                        .weight(1f)
                        .padding(end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (index >= 0 && showLessonNumbers) {
                        Box(
                            Modifier
                                .padding(end = 8.dp)
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            Alignment.Center
                        ) {
                            Text(
                                (index + 1).toString(),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                    Text(
                        event.subjectName ?: (event.title ?: ""),
                        Modifier
                            .weight(1f, false)
                            .animateContentSize(),
                        maxLines = if (!isExpanded) 1 else Int.MAX_VALUE,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (event.source == "PLAN") {
                        EventIndicators(event, Modifier.padding(horizontal = 8.dp))
                    }
                }
                Text(
                    if (event.isAllDay != true) stringResource(
                        id = R.string.time_from_to,
                        event.startAt.toTimeDisplay(),
                        event.finishAt.toTimeDisplay()
                    ) else stringResource(id = R.string.all_day),
                    Modifier
                        .alpha(0.8f),
                    maxLines = 1,
                    softWrap = false
                )

            }
            AnimatedVisibility(
                visible = isExpanded, enter = enterTransition, exit = exitTransition
            ) {
                when (event.source) {
                    in listOf("AE", "CE", "PLAN") -> Box {
                        Column(
                            Modifier
                                .padding(
                                    bottom = 16.dp
                                )
                                .fillMaxWidth()
                        ) {
                            if (!event.roomNumber.isNullOrBlank() && event.roomNumber != "-") {
                                Row(Modifier.padding(bottom = 2.dp)) {
                                    Text(
                                        stringResource(R.string.lesson_location),
                                        Modifier
                                            .padding(end = 4.dp)
                                            .alpha(0.8f),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(event.roomNumber, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            val author = event.authorName as? String
                            if (!author.isNullOrBlank() && author != "Не указан") {
                                Row(Modifier.padding(bottom = 2.dp)) {
                                    Text(
                                        "Учитель: ",
                                        Modifier
                                            .padding(end = 4.dp)
                                            .alpha(0.8f),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(author, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            val theme = (event.lessonTheme as? String) ?: event.description
                            if (!theme.isNullOrBlank() && theme != "-") {
                                Row(Modifier.padding(bottom = 4.dp)) {
                                    Text(
                                        "Тема: ",
                                        Modifier
                                            .padding(end = 4.dp)
                                            .alpha(0.8f),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(theme, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            val lessonDate = event.startAt.substringBefore("T")
                            val hwList = remember(event.id, DataService.hasHomeworks) {
                                val items = mutableListOf<String>()
                                if (event.homework != null && event.homework.descriptions.isNotEmpty()) {
                                    items.addAll(event.homework.descriptions)
                                }
                                if (items.isEmpty() && DataService.hasHomeworks) {
                                    val subject = event.subjectName ?: event.title ?: ""
                                    val matching = DataService.homeworks.filter { hw ->
                                        hw.subjectName.equals(subject, ignoreCase = true) &&
                                                (hw.dateAssignedOn == lessonDate || hw.date == lessonDate) &&
                                                !hw.description.startsWith("Без задания", ignoreCase = true)
                                    }
                                    matching.forEach { hw ->
                                        val text = if (hw.dateAssignedOn == lessonDate && hw.date != lessonDate) {
                                            "${hw.description} (к ${hw.date})"
                                        } else {
                                            hw.description
                                        }
                                        if (!items.contains(text)) items.add(text)
                                    }
                                }
                                items
                            }
                            if (hwList.isNotEmpty()) {
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Text(
                                        stringResource(R.string.homework),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    )
                                    Surface(
                                        shape = MaterialTheme.shapes.small,
                                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        SelectionContainer {
                                            Column(Modifier.padding(10.dp)) {
                                                hwList.forEach { hwItem ->
                                                    Text(
                                                        hwItem,
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    "Задания нет",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                            if (event.marks != null) {
                                Row(
                                    Modifier.padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    event.marks.forEach {
                                        if (event.subjectId != null) {
                                            MarkComp(
                                                it,
                                                subjectId = event.subjectId,
                                                markConfig = markConfig
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        FilledTonalIconButton(
                            onClick = {
                                modalBottomSheetContentLive.postValue { LessonSheetContent(event.id) }
                                modalBottomSheetStateLive.postValue(true)
                            }, modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(bottom = 16.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.MenuOpen,
                                stringResource(id = R.string.expand)
                            )
                        }
                    }

                    "EVENTS" -> Column(
                        Modifier
                            .padding(
                                bottom = 16.dp
                            )
                            .fillMaxWidth()
                    ) {
                        Text(
                            event.description ?: stringResource(R.string.no_description)
                        )
                        if (event.place != null) {
                            Row {
                                Text(
                                    stringResource(R.string.lesson_location),
                                    Modifier
                                        .padding(end = 3.dp)
                                        .alpha(0.8f)
                                )
                                Text(event.place)
                            }
                        }
                        if (event.conferenceLink != null) {
                            val uriHandler = LocalUriHandler.current
                            val clipboardManager = LocalClipboardManager.current
                            val coroutineScope = rememberCoroutineScope()
                            val urlCopiedMessage = stringResource(R.string.url_copied_to_clipboard)
                            val unsupportedUrlMessage = stringResource(R.string.unsupported_url)
                            val interactionSource = remember { MutableInteractionSource() }
                            val viewConfiguration = LocalViewConfiguration.current


                            LaunchedEffect(interactionSource) {
                                var isLongClick = false

                                interactionSource.interactions.collectLatest { interaction ->
                                    when (interaction) {
                                        is PressInteraction.Press -> {
                                            isLongClick = false
                                            delay(viewConfiguration.longPressTimeoutMillis)
                                            isLongClick = true
                                            clipboardManager.setText(
                                                AnnotatedString(event.conferenceLink)
                                            )
                                            coroutineScope.launch {
                                                snackbarHostStateLive.value?.showSnackbar(
                                                    urlCopiedMessage
                                                )
                                            }
                                        }

                                        is PressInteraction.Release -> {
                                            if (!isLongClick && URLUtil.isValidUrl(event.conferenceLink)) {
                                                uriHandler.openUri(event.conferenceLink)
                                            } else if (!URLUtil.isValidUrl(event.conferenceLink)) {
                                                coroutineScope.launch {
                                                    snackbarHostStateLive.value?.showSnackbar(
                                                        unsupportedUrlMessage
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            OutlinedButton(
                                onClick = {},
                                modifier = Modifier
                                    .alpha(if (URLUtil.isValidUrl(event.conferenceLink)) 1f else .5f)
                                    .padding(top = 16.dp),
                                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                                interactionSource = interactionSource
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.OpenInNew,
                                    stringResource(id = R.string.conference),
                                    Modifier.size(ButtonDefaults.IconSize),
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                Text(
                                    stringResource(id = R.string.conference),
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}