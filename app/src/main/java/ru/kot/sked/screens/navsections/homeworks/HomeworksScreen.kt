package ru.kot.sked.screens.navsections.homeworks

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.MutableLiveData
import ru.kot.sked.DataService
import ru.kot.sked.authPrefs
import ru.kot.sked.components.settings.CommonPrefs
import ru.kot.sked.contentDependentActionIconLive
import ru.kot.sked.contentDependentActionLive
import ru.kot.sked.demoScheduleDate
import ru.kot.sked.formatToDay
import ru.kot.sked.formatToLongHumanDay
import ru.kot.sked.formatToWeekday
import ru.kot.sked.get
import ru.kot.sked.getWeekday
import ru.kot.sked.isDemo
import ru.kot.sked.mainPrefs
import ru.kot.sked.network.gosuslugi.GosuslugiDataBridge
import ru.kot.sked.screens.navsections.daybook.CalendarBar
import ru.kot.sked.screens.navsections.daybook.daySelectedLive
import java.util.Calendar
import java.util.Date

val enabledSubjectsLive = MutableLiveData<List<Long>>(emptyList())
val updatedHomeworksLive = MutableLiveData(0L)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeworksScreen() {
    val context = LocalContext.current
    val isDemo = context.isDemo
    val isGosuslugi = context.authPrefs.get<String>("cookie") != null && !isDemo
    val hwUpdateTrigger = updatedHomeworksLive.observeAsState(0L)
    var hwList by remember(DataService.homeworks, hwUpdateTrigger.value) {
        mutableStateOf(DataService.homeworks)
    }
    var isLoadingWeek by remember { mutableStateOf(false) }
    val loadedWeeks = remember { mutableStateListOf<String>() }
    val weekStartsAlwaysMonday =
        context.mainPrefs.get(CommonPrefs.weekStartsAlwaysMonday.prefKey) ?: false

    LaunchedEffect(hwList) {
        val enable = { enabled: Boolean, id: Long ->
            if (enabledSubjectsLive.value != null) {
                if (enabled && enabledSubjectsLive.value?.contains(id) == false) {
                    enabledSubjectsLive.postValue(enabledSubjectsLive.value!! + listOf(id))
                } else if (!enabled && enabledSubjectsLive.value?.contains(id) == true) {
                    enabledSubjectsLive.postValue(enabledSubjectsLive.value!!.filter {
                        it != id
                    })
                }
            }
        }
        val currentEnabled = enabledSubjectsLive.value
        val allIds = hwList.map { it.subjectId }.distinct()
        if (currentEnabled.isNullOrEmpty()) {
            enabledSubjectsLive.postValue(allIds)
        } else {
            val missing = allIds.filter { it !in currentEnabled }
            if (missing.isNotEmpty()) {
                enabledSubjectsLive.postValue((currentEnabled + missing).distinct())
            }
        }
        contentDependentActionIconLive.postValue(Icons.Rounded.FilterAlt)
        contentDependentActionLive.postValue {
            hwList.map { it.subjectId to it.subjectName }.toSet().forEach {
                var checked by rememberSaveable(key = it.first.toString()) {
                    mutableStateOf(
                        enabledSubjectsLive.value?.any { it1 -> it1 == it.first } ?: true)
                }
                DropdownMenuItem(text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked,
                            { isChecked -> checked = isChecked; enable(isChecked, it.first) })
                        Text(
                            it.second,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }, onClick = {
                    checked = !checked
                    enable(checked, it.first)
                })
            }
        }
    }

    val defaultTargetDate = remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 1)
        if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        cal.time
    }

    val currentDay = daySelectedLive.observeAsState(
        if (!isDemo) (daySelectedLive.value ?: defaultTargetDate) else demoScheduleDate
    )

    LaunchedEffect(Unit) {
        if (daySelectedLive.value == null) {
            daySelectedLive.value = defaultTargetDate
        }
    }

    val weekdays = remember(currentDay.value) {
        val cal = Calendar.getInstance().apply { time = currentDay.value }
        val first = if (weekStartsAlwaysMonday) Calendar.MONDAY else cal.firstDayOfWeek
        (0..6).map { offset -> ((first + offset - 1) % 7) + 1 }
    }

    val dayPosition = rememberPagerState(
        initialPage = weekdays.indexOf(getWeekday(currentDay.value)).coerceAtLeast(0),
        pageCount = { 7 }
    )

    LaunchedEffect(dayPosition) {
        snapshotFlow { dayPosition.currentPage }.collect { page ->
            if (weekdays.indexOf(getWeekday(currentDay.value)) != page) {
                daySelectedLive.postValue(currentDay.value.let {
                    Calendar.getInstance().apply {
                        if (weekStartsAlwaysMonday)
                            firstDayOfWeek = Calendar.MONDAY
                        minimalDaysInFirstWeek = 4
                        time = it
                        get(Calendar.DAY_OF_WEEK)
                        set(Calendar.DAY_OF_WEEK, weekdays[page])
                    }.time
                })
            }
        }
    }
    LaunchedEffect(currentDay) {
        snapshotFlow { currentDay.value }.collect { date ->
            val targetPage = weekdays.indexOf(getWeekday(date)).coerceAtLeast(0)
            if (targetPage != dayPosition.currentPage) {
                dayPosition.scrollToPage(targetPage)
            }
            if (!isDemo) {
                if (isGosuslugi) {
                    if (!GosuslugiDataBridge.isWeekLoaded(date)) {
                        isLoadingWeek = true
                        GosuslugiDataBridge.fetchWeekData(context, date) {
                            hwList = DataService.homeworks
                            isLoadingWeek = false
                        }
                    } else {
                        hwList = DataService.homeworks
                    }
                } else {
                    val weekKey = GosuslugiDataBridge.getPrimaryIntervalForDate(date)
                    if (weekKey !in loadedWeeks) {
                        loadedWeeks.add(weekKey)
                        isLoadingWeek = true
                        DataService.getHomeworksWeek(date, weekStartsAlwaysMonday) { merged ->
                            hwList = merged
                            isLoadingWeek = false
                        }
                    }
                }
            }
        }
    }

    val hwByDate = remember(hwList) { hwList.groupBy { it.date } }

    Column(Modifier.fillMaxSize()) {
        CalendarBar()
        HorizontalPager(state = dayPosition, beyondBoundsPageCount = 1) { weekdayIndex ->
            val date = remember(currentDay.value, weekdayIndex, weekStartsAlwaysMonday) {
                Calendar.getInstance().apply {
                    if (weekStartsAlwaysMonday)
                        firstDayOfWeek = Calendar.MONDAY
                    minimalDaysInFirstWeek = 4
                    time = currentDay.value
                    get(Calendar.DAY_OF_WEEK)
                    set(Calendar.DAY_OF_WEEK, weekdays[weekdayIndex])
                }.time
            }
            val dateStr = remember(date) { date.formatToDay() }
            val dayHomeworks = remember(hwByDate, dateStr) { hwByDate[dateStr] ?: emptyList() }

            if (dayHomeworks.isNotEmpty()) {
                LazyColumn(
                    Modifier
                        .fillMaxSize()
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp)
                ) {
                    item(key = "hw_day_$dateStr") {
                        HomeworkDay(homeworks = dayHomeworks)
                    }
                }
            } else {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp)
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.85f),
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Row(Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) {
                            Text(
                                date.formatToLongHumanDay(),
                                Modifier.padding(end = 4.dp),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                date.formatToWeekday(),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (isLoadingWeek) {
                        Box(Modifier.fillMaxSize()) {
                            CircularProgressIndicator(Modifier.align(Alignment.Center))
                        }
                    } else {
                        Column(
                            Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.TaskAlt,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "Заданий нет",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "На этот день ничего не задано 🎉",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
