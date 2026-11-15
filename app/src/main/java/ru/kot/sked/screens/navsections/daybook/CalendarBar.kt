package ru.kot.sked.screens.navsections.daybook

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ru.kot.sked.components.settings.CommonPrefs
import ru.kot.sked.demoScheduleDate
import ru.kot.sked.formatToHumanDay
import ru.kot.sked.get
import ru.kot.sked.isDemo
import ru.kot.sked.mainPrefs
import ru.kot.sked.modalDialogContentLive
import ru.kot.sked.modalDialogStateLive
import java.util.Calendar
import java.util.Date

@Composable
fun CalendarBar() {
    val isDemo = LocalContext.current.isDemo
    val daySelected = daySelectedLive.observeAsState(if (!isDemo) Date() else demoScheduleDate)
    val weekStartsAlwaysMonday =
        LocalContext.current.mainPrefs.get(CommonPrefs.weekStartsAlwaysMonday.prefKey) ?: false

    var calendar by remember {
        mutableStateOf(
            Calendar.getInstance().apply {
                if (weekStartsAlwaysMonday) firstDayOfWeek = Calendar.MONDAY
                minimalDaysInFirstWeek = 4
                time = daySelected.value
                get(Calendar.DAY_OF_WEEK)
            }
        )
    }
    LaunchedEffect(daySelected.value, weekStartsAlwaysMonday) {
        val day = daySelected.value
        calendar = Calendar.getInstance().apply {
            if (weekStartsAlwaysMonday) firstDayOfWeek = Calendar.MONDAY
            minimalDaysInFirstWeek = 4
            time = day
            get(Calendar.DAY_OF_WEEK)
        }
    }

    val firstDay = if (weekStartsAlwaysMonday) Calendar.MONDAY else calendar.firstDayOfWeek
    val weekStartDate = remember(calendar, firstDay) {
        (calendar.clone() as Calendar).apply {
            this.firstDayOfWeek = firstDay
            get(Calendar.DAY_OF_WEEK)
            set(Calendar.DAY_OF_WEEK, firstDay)
        }.time
    }
    val weekEndDate = remember(calendar, firstDay) {
        (calendar.clone() as Calendar).apply {
            this.firstDayOfWeek = firstDay
            get(Calendar.DAY_OF_WEEK)
            set(Calendar.DAY_OF_WEEK, ((firstDay + 5) % 7) + 1)
        }.time
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.background(
            MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
            MaterialTheme.shapes.medium.copy(
                topStart = CornerSize(0.dp), topEnd = CornerSize(0.dp)
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = {
                    val prevWeek = Calendar.getInstance().apply {
                        time = daySelected.value
                        add(Calendar.DAY_OF_YEAR, -7)
                    }.time
                    daySelectedLive.postValue(prevWeek)
                }
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    contentDescription = "Предыдущая неделя"
                )
            }

            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .clickable {
                        modalDialogContentLive.value = { DayChooser() }
                        modalDialogStateLive.postValue(true)
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.CalendarMonth,
                        contentDescription = "Выбрать дату",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "${weekStartDate.formatToHumanDay()} – ${weekEndDate.formatToHumanDay()}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            IconButton(
                onClick = {
                    val nextWeek = Calendar.getInstance().apply {
                        time = daySelected.value
                        add(Calendar.DAY_OF_YEAR, 7)
                    }.time
                    daySelectedLive.postValue(nextWeek)
                }
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = "Следующая неделя"
                )
            }
        }

        CalendarRow(
            date = calendar,
            daySelected,
            { daySelectedLive.postValue(it) },
            Modifier
                .padding(bottom = 12.dp)
        )
    }
}