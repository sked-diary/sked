package ru.kot.sked.screens.navsections.daybook

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import ru.kot.sked.DataService
import ru.kot.sked.R
import ru.kot.sked.components.ErrorMessage
import ru.kot.sked.components.MarkComp
import ru.kot.sked.components.WebViewDialog
import ru.kot.sked.formatToDay
import ru.kot.sked.formatToHumanDay
import ru.kot.sked.formatToTime
import ru.kot.sked.getDemoProperty
import ru.kot.sked.getMarkConfig
import ru.kot.sked.isDemo
import ru.kot.sked.authPrefs
import ru.kot.sked.get
import ru.kot.sked.models.lesson2.Details
import ru.kot.sked.models.lesson2.LessonHomework
import ru.kot.sked.models.lesson2.LessonResponse
import ru.kot.sked.models.lesson2.Teacher
import ru.kot.sked.models.lesson2.Theme
import ru.kot.sked.parseFromDay
import ru.kot.sked.parseLongDate
import ru.kot.sked.webViewDialogLive

@Composable
fun LessonSheetContent(lessonId: Long) {
    var lessonInfo by remember { mutableStateOf<LessonResponse?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val isGosuslugi = context.authPrefs.get<String>("cookie") != null
    LaunchedEffect(Unit) {
        if (context.isDemo) {
            lessonInfo = context.getDemoProperty(R.raw.demo_lesson_info)
        } else if (isGosuslugi) {
            val event = DataService.eventCalendar.firstOrNull { it.id == lessonId }
            if (event != null) {
                val lessonDate = event.startAt.parseLongDate().formatToDay()
                val hwItems = mutableListOf<LessonHomework>()

                event.homework?.descriptions?.forEachIndexed { idx, desc ->
                    hwItems.add(
                        LessonHomework(
                            additionalMaterials = emptyList(),
                            attachments = emptyList(),
                            dateAssignedOn = lessonDate,
                            datePreparedFor = lessonDate,
                            homework = desc,
                            homeworkCreatedAt = event.startAt,
                            homeworkEntryId = (lessonId + idx + 1),
                            homeworkEntryStudentId = (lessonId + idx + 1),
                            homeworkId = (lessonId + idx + 1).toInt(),
                            homeworkUpdatedAt = event.startAt,
                            isDone = false,
                            isSmart = false,
                            writtenAnswer = null
                        )
                    )
                }

                if (hwItems.isEmpty() && DataService.hasHomeworks) {
                    val subject = event.subjectName ?: event.title ?: ""
                    DataService.homeworks.filter { hw ->
                        hw.subjectName.equals(subject, ignoreCase = true) &&
                                (hw.dateAssignedOn == lessonDate || hw.date == lessonDate) &&
                                !hw.description.startsWith("Без задания", ignoreCase = true)
                    }.forEachIndexed { idx, hw ->
                        val text = if (hw.dateAssignedOn == lessonDate && hw.date != lessonDate) {
                            "${hw.description} (к ${hw.date})"
                        } else {
                            hw.description
                        }
                        val addMats = mutableListOf<ru.kot.sked.models.lessonschedule.AdditionalMaterialX>()
                        hw.gosuslugiMaterials?.forEach { mat ->
                            val link = mat.link
                            if (!link.isNullOrBlank()) {
                                addMats.add(
                                    ru.kot.sked.models.lessonschedule.AdditionalMaterialX(
                                        actionId = 0,
                                        actionName = "Открыть",
                                        contentType = null,
                                        description = null,
                                        id = null,
                                        selectedMode = null,
                                        title = mat.name ?: "Прикрепленный файл",
                                        type = "attachments",
                                        typeName = "Файл",
                                        urls = listOf(ru.kot.sked.models.lessonschedule.UrlX(url = link, type = "file_link")),
                                        uuid = null
                                    )
                                )
                            }
                        }
                        hwItems.add(
                            LessonHomework(
                                additionalMaterials = addMats,
                                attachments = emptyList(),
                                dateAssignedOn = hw.dateAssignedOn,
                                datePreparedFor = hw.date,
                                homework = text,
                                homeworkCreatedAt = event.startAt,
                                homeworkEntryId = (lessonId + idx + 1),
                                homeworkEntryStudentId = (lessonId + idx + 1),
                                homeworkId = (lessonId + idx + 1).toInt(),
                                homeworkUpdatedAt = event.startAt,
                                isDone = false,
                                isSmart = false,
                                writtenAnswer = null
                            )
                        )
                    }
                }

                val author = event.authorName as? String ?: ""
                val tParts = author.split(" ")
                val lName = tParts.getOrNull(0) ?: ""
                val fName = tParts.getOrNull(1) ?: ""
                val mName = tParts.drop(2).joinToString(" ")

                lessonInfo = LessonResponse(
                    beginTime = event.startAt.parseLongDate().formatToTime(),
                    beginUtc = 0,
                    buildingName = "",
                    comment = event.description,
                    control = null,
                    courseLessonType = null,
                    createdDateTime = null,
                    date = lessonDate,
                    details = Details(
                        additionalMaterials = emptyList(),
                        content = emptyList(),
                        lessonId = event.id.toInt(),
                        lessonTopic = event.description ?: "",
                        theme = Theme(
                            averageMark = null,
                            egeTaskName = null,
                            id = null,
                            ogeTaskName = null,
                            themeFrames = emptyList(),
                            themeIntegrationId = null,
                            title = event.description
                        )
                    ),
                    diseaseStatusType = null,
                    endTime = event.finishAt.parseLongDate().formatToTime(),
                    endUtc = 0,
                    eszFieldId = null,
                    evaluation = null,
                    fieldName = null,
                    groupId = 0L,
                    homeworkPresenceStatusId = 0,
                    homeworkToGive = null,
                    id = event.id.toInt(),
                    isMissedLesson = false,
                    isVirtual = false,
                    lessonEducationType = "",
                    lessonHomeworks = hwItems,
                    lessonType = "",
                    lessonTypeNsi = null,
                    marks = event.marks ?: emptyList(),
                    planId = 0L,
                    remoteLesson = null,
                    roomName = event.roomNumber ?: "",
                    roomNumber = if (event.roomNumber != "-") event.roomNumber ?: "" else "",
                    subjectId = event.subjectId ?: (event.subjectName ?: "").hashCode().toLong(),
                    subjectName = event.subjectName ?: event.title ?: "",
                    teacher = Teacher(
                        birthDate = null,
                        firstName = fName,
                        lastName = lName,
                        middleName = mName,
                        sex = null,
                        userId = null
                    ),
                    teacherComments = emptyList(),
                    themeMastery = null
                )
            } else {
                errorText = "Информация об уроке не найдена"
            }
        } else DataService.getLessonInfo(lessonId, { errorText = it }) {
            lessonInfo = it
        }
    }

    Box(
        Modifier
            .heightIn(192.dp, Int.MAX_VALUE.dp)
            .fillMaxWidth()
    ) {
        if (lessonInfo != null) {
            Column(Modifier.padding(16.dp)) {
                with(lessonInfo!!) {
                    Text(
                        subjectName,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    val teacherName = teacher.run { "$lastName $firstName $middleName" }.trim()
                    if (teacherName.isNotBlank() && teacherName != "Не указан") {
                        Text(teacherName, modifier = Modifier.padding(bottom = 2.dp))
                    }
                    if (roomNumber.isNotBlank() && roomNumber != "-") {
                        Row {
                            Text(
                                stringResource(R.string.lesson_location),
                                Modifier
                                    .padding(end = 3.dp)
                                    .alpha(0.8f)
                            )
                            Text(roomNumber)
                        }
                    }
                    Text(
                        stringResource(
                            id = R.string.date_weekday,
                            date.parseFromDay().formatToHumanDay(),
                            stringResource(id = R.string.time_from_to, beginTime, endTime)
                        )
                    )
                    if (comment != null && comment is String && comment.isNotBlank() && comment != "-") {
                        Text("Тема: $comment", modifier = Modifier.padding(top = 4.dp))
                    }
                    HorizontalDivider(Modifier.padding(16.dp))
                    if (lessonHomeworks.isEmpty()) {
                        Text(
                            "Домашнее задание не задано",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        lessonHomeworks.forEach { homework ->
                            val clipboardManager = LocalClipboardManager.current
                            Text(homework.homework, modifier = Modifier.clickable {
                                clipboardManager.setText(AnnotatedString(homework.homework))
                            })
                            homework.additionalMaterials.forEach { material ->
                                val ctx = LocalContext.current
                                OutlinedButton(onClick = {
                                    if (material.type == "attachments" || context.isDemo) {
                                        val browserIntent = Intent(
                                            Intent.ACTION_VIEW,
                                            material.urls.firstOrNull { it.type == "file_link" }?.url?.toUri()
                                        )
                                        ContextCompat.startActivity(ctx, browserIntent, null)
                                    } else {
                                        DataService.getLaunchUrl(
                                            homework.homeworkEntryId,
                                            material.uuid ?: "",
                                        ) {
                                            webViewDialogLive.postValue {
                                                WebViewDialog(it) {
                                                    webViewDialogLive.postValue(null)
                                                }
                                            }
                                        }
                                    }
                                }, contentPadding = ButtonDefaults.ButtonWithIconContentPadding) {
                                    Icon(
                                        material.icon,
                                        stringResource(id = R.string.image),
                                        Modifier.size(ButtonDefaults.IconSize)
                                    )
                                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                    Text(material.title)
                                }
                            }
                        }
                    }
                    val markConfig = getMarkConfig()
                    LazyRow {
                        items(marks) {
                            MarkComp(it, subjectId = subjectId, markConfig = markConfig)
                        }
                    }
                }
            }
        } else if (errorText == null) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        } else {
            ErrorMessage(Modifier.align(Alignment.Center), errorText!!)
        }
    }
}