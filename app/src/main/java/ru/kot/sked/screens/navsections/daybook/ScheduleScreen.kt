package ru.kot.sked.screens.navsections.daybook

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.MutableLiveData
import ru.kot.sked.DataService
import ru.kot.sked.R
import ru.kot.sked.areBreaksShown
import ru.kot.sked.authPrefs
import ru.kot.sked.components.settings.CommonPrefs
import ru.kot.sked.demoScheduleDate
import ru.kot.sked.formatToDay
import ru.kot.sked.get
import ru.kot.sked.getWeekday
import ru.kot.sked.isDateBetween
import ru.kot.sked.isDemo
import ru.kot.sked.mainPrefs
import ru.kot.sked.models.events.Event
import ru.kot.sked.network.gosuslugi.GosuslugiDataBridge
import ru.kot.sked.parseLongDate
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

val customScheduleRefreshListenerLive = MutableLiveData<() -> Unit>(null)
val updatedScheduleLive = MutableLiveData(true)
val daySelectedLive = MutableLiveData<Date>()

@Composable
fun ScheduleScreen() {
    key(updatedScheduleLive.observeAsState().value) {
        val eventCalendar = DataService.eventCalendar.let { calendar ->
            if (LocalContext.current.mainPrefs.get(CommonPrefs.showOnlyPlan.prefKey) ?: false) {
                calendar.filter { it.source == "PLAN" }
            } else calendar
        }
        Column {
            CalendarBar()
            WeekPager(eventCalendar)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WeekPager(eventsLoaded: List<Event>) {
    val context = LocalContext.current
    val isDemo = context.isDemo
    val isGosuslugi = context.authPrefs.get<String>("cookie") != null
    var events by remember(eventsLoaded) { mutableStateOf(eventsLoaded) }
    LaunchedEffect(eventsLoaded) {
        events = eventsLoaded
    }
    var isLoadingNewEvents by remember { mutableStateOf(false) }
    var currentDateRange by remember(DataService.eventsRange) { mutableStateOf(DataService.eventsRange) }
    val showNumbers =
        context.mainPrefs.get(CommonPrefs.showLessonNumbers.prefKey) ?: true
    val showBreaks = areBreaksShown()
    val currentDay = daySelectedLive.observeAsState(if (!isDemo) Date() else demoScheduleDate)
    val weekStartsAlwaysMonday =
        context.mainPrefs.get(CommonPrefs.weekStartsAlwaysMonday.prefKey) ?: false

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
                dayPosition.animateScrollToPage(targetPage)
            }
            if (date.isDateBetween(DataService.eventsRange) && currentDateRange != DataService.eventsRange) {
                events = eventsLoaded
                currentDateRange = DataService.eventsRange
            }
            if (!isDemo) {
                if (isGosuslugi) {
                    if (!GosuslugiDataBridge.isWeekLoaded(date)) {
                        isLoadingNewEvents = true
                        GosuslugiDataBridge.fetchWeekData(context, date) {
                            events = DataService.eventCalendar
                            isLoadingNewEvents = false
                        }
                    } else {
                        events = DataService.eventCalendar
                    }
                } else if (!date.isDateBetween(currentDateRange)) {
                    isLoadingNewEvents = true
                    DataService.getEventWeek(date, weekStartsAlwaysMonday) { eventsResponse, range ->
                        currentDateRange = range
                        events = eventsResponse
                        isLoadingNewEvents = false
                    }
                }
            }
        }
    }
    val eventsByDate = remember(events) {
        events.groupBy { it.startAt.substringBefore("T") }
    }
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
        val dayEvents = remember(eventsByDate, dateStr) {
            eventsByDate[dateStr]?.sortedBy { it.startAt } ?: emptyList()
        }
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.85f),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text(
                    text = date.let {
                        SimpleDateFormat(
                            "d MMMM, EEEE", LocalConfiguration.current.locales[0]
                        ).format(it)
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            AnimatedVisibility(visible = isLoadingNewEvents) {
                Box(Modifier.fillMaxSize()) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
            }
            AnimatedVisibility(visible = !isLoadingNewEvents) {
                if (dayEvents.isNotEmpty()) {
                    LazyColumn(Modifier.fillMaxSize()) {
                        DayItem(dayEvents, showNumbers, showBreaks)
                    }
                } else {
                    Column(
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = stringResource(id = R.string.free_day))
                    }
                }
            }
        }
    }
}