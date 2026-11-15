package ru.kot.sked.screens.navsections.dashboard

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import ru.kot.sked.DataService
import ru.kot.sked.R
import ru.kot.sked.authPrefs
import ru.kot.sked.components.settings.CommonPrefs
import ru.kot.sked.demoScheduleDate
import ru.kot.sked.formatToDay
import ru.kot.sked.formatToHumanDay
import ru.kot.sked.formatToTime
import ru.kot.sked.get
import androidx.compose.ui.unit.sp
import ru.kot.sked.isDemo
import ru.kot.sked.mainPrefs
import ru.kot.sked.triggerBirthdayConfettiLive
import ru.kot.sked.components.BirthdayHelper
import ru.kot.sked.components.BirthdayCard
import ru.kot.sked.modalBottomSheetContentLive
import ru.kot.sked.modalBottomSheetStateLive
import ru.kot.sked.models.events.Event
import ru.kot.sked.models.homeworks2.Homework
import ru.kot.sked.models.marklistdate.Mark as DateMark
import ru.kot.sked.parseFromDay
import ru.kot.sked.parseLongDate
import ru.kot.sked.screens.navsections.daybook.DayItem
import ru.kot.sked.screens.navsections.marks.ExtendedMark
import ru.kot.sked.screens.navsections.profile.avatarTriggerLive
import ru.kot.sked.screens.navsections.profile.showAvatarDialog
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen() {
    val context = LocalContext.current
    val isGosuslugi = context.authPrefs.get<String>("cookie") != null || context.isDemo
    val showNumbers =
        context.mainPrefs.get(CommonPrefs.showLessonNumbers.prefKey) ?: true
    val state = rememberLazyListState()

    // 1. Live Ticker: updates every 10 seconds so status, minutes left, and breaks update in real time!
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(10000L)
            currentTime = System.currentTimeMillis()
        }
    }

    if (isGosuslugi) {
        val events = DataService.eventCalendar
        val todayStr = remember(currentTime) {
            val d = if (context.isDemo) demoScheduleDate else Date(currentTime)
            d.formatToDay()
        }

        // 2. Pre-calculated & Memoized schedule for Butter-Smooth 120 FPS scrolling
        val (targetEvents, scheduleTitle) = remember(events, todayStr, currentTime) {
            val nowTime = if (context.isDemo) demoScheduleDate.time else currentTime
            val todayEvents = events.filter {
                try {
                    it.startAt.parseLongDate().formatToDay() == todayStr
                } catch (e: Exception) {
                    false
                }
            }.sortedBy {
                try { it.startAt.parseLongDate().time } catch (e: Exception) { 0L }
            }

            val isTodayOngoing = todayEvents.isNotEmpty() &&
                    (todayEvents.lastOrNull()?.finishAt?.parseLongDate()?.time ?: 0L) > nowTime

            if (isTodayOngoing) {
                todayEvents to "Расписание на сегодня"
            } else {
                val nextDayEvent = events.filter {
                    try {
                        it.startAt.parseLongDate().time > nowTime
                    } catch (e: Exception) {
                        false
                    }
                }.minByOrNull {
                    try { it.startAt.parseLongDate().time - nowTime } catch (e: Exception) { Long.MAX_VALUE }
                }

                if (nextDayEvent != null) {
                    val nextDayStr = nextDayEvent.startAt.parseLongDate().formatToDay()
                    val dayLessons = events.filter {
                        try {
                            it.startAt.parseLongDate().formatToDay() == nextDayStr
                        } catch (e: Exception) {
                            false
                        }
                    }.sortedBy {
                        try { it.startAt.parseLongDate().time } catch (e: Exception) { 0L }
                    }
                    val formattedDate = try {
                        SimpleDateFormat("d MMMM", Locale.getDefault()).format(nextDayEvent.startAt.parseLongDate())
                    } catch (e: Exception) {
                        ""
                    }
                    dayLessons to "Расписание на $formattedDate"
                } else {
                    emptyList<Event>() to "Расписание уроков"
                }
            }
        }

        val recentHomeworks = remember(DataService.homeworks) {
            DataService.homeworks.filter { !it.isDone }.take(3)
        }

        val recentMarks = remember(DataService.marksDate.payload) {
            DataService.marksDate.payload.take(3)
        }

        LazyColumn(
            state = state,
            reverseLayout = false,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            item(key = "header_profile_card") {
                val child = try {
                    DataService.profile.children[DataService.currentProfile]
                } catch (e: Exception) {
                    null
                }
                val firstName = child?.firstName ?: "Ученик"
                val className = child?.className ?: ""
                val school = child?.school?.shortName ?: "Школа"
                val trigger = avatarTriggerLive.observeAsState()
                val avatarFile = remember { File(context.filesDir, "custom_avatar.png") }
                val localBitmap = remember(trigger.value) {
                    if (avatarFile.exists()) {
                        try {
                            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                            BitmapFactory.decodeFile(avatarFile.absolutePath, bounds)
                            val sample = (maxOf(bounds.outWidth, bounds.outHeight) / 120).coerceAtLeast(1)
                            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
                            BitmapFactory.decodeFile(avatarFile.absolutePath, opts)
                        } catch (e: Exception) {
                            null
                        }
                    } else null
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 4.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.85f)
                    )
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Привет, $firstName 👋",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "$className класс • $school",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Box(
                            Modifier
                                .padding(start = 12.dp)
                                .size(54.dp)
                                .clip(CircleShape)
                                .clickable {
                                    showAvatarDialog(context)
                                }
                        ) {
                            if (localBitmap != null) {
                                Image(
                                    bitmap = localBitmap.asImageBitmap(),
                                    contentDescription = "Аватар",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            MaterialTheme.colorScheme.primaryContainer,
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = firstName.take(1).uppercase(),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            if (BirthdayHelper.isTodayBirthday(context)) {
                                Text(
                                    "🥳",
                                    fontSize = 16.sp,
                                    modifier = Modifier.align(Alignment.BottomEnd)
                                )
                            }
                        }
                    }
                }
            }

            if (BirthdayHelper.isTodayBirthday(context)) {
                item(key = "birthday_greeting_card") {
                    BirthdayCard(context) {
                        triggerBirthdayConfettiLive.postValue(System.currentTimeMillis())
                    }
                }
            }

            item(key = "live_lesson_status") {
                LiveLessonStatusCard(context, currentTime)
            }

            dashboardScheduleGosuslugi(targetEvents, scheduleTitle, showNumbers)
            dashboardHomeworksGosuslugi(recentHomeworks)
            dashboardRecentMarksGosuslugi(recentMarks)

            item(key = "footer_spacer") {
                Spacer(Modifier.height(16.dp))
            }
        }
    } else {
        LazyColumn(
            state = state,
            reverseLayout = true,
            verticalArrangement = Arrangement.Bottom,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxSize()
        ) {
            item {
                Spacer(Modifier.height(16.dp))
            }
            dashboardRatingVisits()
            dashboardSchedule(context, showNumbers)
        }
    }
}

@Composable
fun LiveLessonStatusCard(context: Context, currentTime: Long) {
    val events = DataService.eventCalendar
    if (events.isEmpty()) return

    val now = remember(currentTime) { if (context.isDemo) demoScheduleDate else Date(currentTime) }
    val todayStr = remember(now) { now.formatToDay() }
    val todayLessons = remember(events, todayStr) {
        events.filter {
            try {
                it.startAt.parseLongDate().formatToDay() == todayStr
            } catch (_: Exception) {
                false
            }
        }.sortedBy {
            try { it.startAt.parseLongDate().time } catch (_: Exception) { 0L }
        }
    }

    if (todayLessons.isEmpty()) return

    val currentLesson = todayLessons.firstOrNull {
        try {
            val start = it.startAt.parseLongDate().time
            val finish = it.finishAt.parseLongDate().time
            now.time in start until finish
        } catch (_: Exception) {
            false
        }
    }

    if (currentLesson != null) {
        val start = currentLesson.startAt.parseLongDate().time
        val finish = currentLesson.finishAt.parseLongDate().time
        val minutesLeft = ((finish - now.time) / 60000).toInt().coerceAtLeast(1)
        val progress = ((now.time - start).toFloat() / (finish - start).coerceAtLeast(1)).coerceIn(0f, 1f)
        val room = if (!currentLesson.roomNumber.isNullOrBlank() && currentLesson.roomNumber != "-") " • каб. ${currentLesson.roomNumber}" else ""

        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4CAF50))
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Идёт урок",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Text(
                        "Осталось $minutesLeft мин",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    (currentLesson.subjectName ?: currentLesson.title ?: "") + room,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                val theme = (currentLesson.lessonTheme as? String) ?: currentLesson.description
                if (!theme.isNullOrBlank() && theme != "-") {
                    Text(
                        theme,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            }
        }
    } else {
        val nextLesson = todayLessons.firstOrNull {
            try {
                it.startAt.parseLongDate().time > now.time
            } catch (_: Exception) {
                false
            }
        }
        val prevLesson = todayLessons.lastOrNull {
            try {
                it.finishAt.parseLongDate().time <= now.time
            } catch (_: Exception) {
                false
            }
        }

        if (nextLesson != null && prevLesson != null) {
            val breakMinutes = ((nextLesson.startAt.parseLongDate().time - now.time) / 60000).toInt().coerceAtLeast(1)
            val room = if (!nextLesson.roomNumber.isNullOrBlank() && nextLesson.roomNumber != "-") " • каб. ${nextLesson.roomNumber}" else ""

            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFB300))
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Перемена",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        Text(
                            "До звонка $breakMinutes мин",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Следующий: ${nextLesson.subjectName ?: nextLesson.title ?: ""}$room",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        "Начало в ${nextLesson.startAt.parseLongDate().formatToTime()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        } else if (nextLesson != null && prevLesson == null) {
            val minutesUntil = ((nextLesson.startAt.parseLongDate().time - now.time) / 60000).toInt().coerceAtLeast(1)
            val room = if (!nextLesson.roomNumber.isNullOrBlank() && nextLesson.roomNumber != "-") " • каб. ${nextLesson.roomNumber}" else ""

            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Первый урок в ${nextLesson.startAt.parseLongDate().formatToTime()}",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            "${nextLesson.subjectName ?: nextLesson.title ?: ""}$room • через $minutesUntil мин",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

fun LazyListScope.dashboardScheduleGosuslugi(
    targetEvents: List<Event>,
    title: String,
    showNumbers: Boolean
) {
    item(key = "schedule_header") {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.85f),
            modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }
    }

    if (targetEvents.isNotEmpty()) {
        DayItem(day = targetEvents, showLessonNumbers = showNumbers, showBreaks = true, reversed = false)
    } else {
        item(key = "no_lessons_msg") {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Text(
                    "Уроков нет, можно отдыхать! 🎉",
                    Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

fun LazyListScope.dashboardHomeworksGosuslugi(hw: List<Homework>) {
    if (hw.isNotEmpty()) {
        item(key = "hw_header") {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.85f),
                modifier = Modifier.padding(top = 14.dp, bottom = 4.dp)
            ) {
                Text(
                    text = "Ближайшие задания",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }
        item(key = "hw_content") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                hw.forEach { item ->
                    Card(
                        Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    item.subjectName ?: "",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    item.datePreparedFor ?: "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                item.description ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }
    }
}

fun LazyListScope.dashboardRecentMarksGosuslugi(recentMarks: List<DateMark>) {
    if (recentMarks.isNotEmpty()) {
        item(key = "marks_header") {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.85f),
                modifier = Modifier.padding(top = 14.dp, bottom = 4.dp)
            ) {
                Text(
                    text = "Последние оценки",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }
        item(key = "marks_content") {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                recentMarks.forEach { m ->
                    Card(
                        Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        ExtendedMark(m)
                    }
                }
            }
        }
    }
}

fun LazyListScope.dashboardSchedule(context: Context, showNumbers: Boolean) {
    val date = if (context.isDemo) {
        demoScheduleDate
    } else Date()
    item {
        Spacer(Modifier.size(8.dp))
    }
    DayItem(
        day = DataService.eventCalendar.filter { it.startAt.parseLongDate().time > date.time }
            .minByOrNull {
                it.startAt.parseLongDate().time - date.time
            }?.startAt?.parseLongDate()?.formatToDay()?.let { day ->
                DataService.eventCalendar.filter {
                    it.startAt.parseLongDate().formatToDay() == day
                }
            } ?: listOf(), showNumbers, showBreaks = false, reversed = true)
    item {
        val currentDay = remember { date.formatToDay() }
        Column(
            verticalArrangement = Arrangement.Bottom
        ) {
            val todayCalendar = DataService.eventCalendar.filter {
                it.startAt.parseLongDate().formatToDay() == currentDay
            }
            val nearestEvent =
                DataService.eventCalendar.filter { it.startAt.parseLongDate().time > date.time }
                    .minByOrNull {
                        it.startAt.parseLongDate().time - date.time
                    }
            if (todayCalendar.isNotEmpty() && date < todayCalendar.maxBy { it.finishAt.parseLongDate() }.finishAt.parseLongDate()) {
                Text(
                    stringResource(id = R.string.schedule_today),
                    style = MaterialTheme.typography.labelLarge
                )
            } else if (nearestEvent != null) {
                Text(
                    stringResource(
                        id = R.string.schedule_for,
                        nearestEvent.startAt.parseLongDate().formatToHumanDay()
                    ),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

fun LazyListScope.dashboardRatingVisits() {
    item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom)) {
            Column {
                if (LocalContext.current.mainPrefs.get<Boolean>(CommonPrefs.mainRating.prefKey) != false) {
                    Text(
                        stringResource(id = R.string.rating),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Card(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                modalBottomSheetContentLive.value = { RankingList() }
                                modalBottomSheetStateLive.postValue(true)
                            }
                    ) {
                        Column(
                            Modifier
                                .padding(16.dp)
                        ) {
                            Text(
                                stringResource(
                                    id = R.string.rating_place,
                                    DataService
                                        .run { ranking.firstOrNull { it.personId == profile.children[currentProfile].contingentGuid } }
                                        ?.rank?.rankPlace ?: "?"
                                )
                            )
                        }
                    }
                }
            }
            Column {
                if (DataService.hasVisits && DataService.visits.payload.isNotEmpty()) {
                    val lastVisit =
                        DataService.visits.payload.filter { day -> !day.visits.any { it.inX == "-" && it.out == "-" } }
                            .maxByOrNull {
                                it.date.parseFromDay().toInstant().toEpochMilli()
                            }
                    if (lastVisit != null) {
                        Text(
                            text = stringResource(
                                R.string.visits_t,
                                lastVisit.date.parseFromDay().formatToHumanDay()
                            ),
                            style = MaterialTheme.typography.labelLarge
                        )
                        Card(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    modalBottomSheetContentLive.value = { VisitsList() }
                                    modalBottomSheetStateLive.postValue(true)
                                }
                        ) {
                            Row(
                                Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(lastVisit.visits[0].inX)
                                Icon(
                                    Icons.AutoMirrored.Rounded.ArrowForward,
                                    stringResource(id = R.string.to)
                                )
                                Text(lastVisit.visits[0].out)
                            }
                        }
                    }
                }
            }
        }
    }
}
