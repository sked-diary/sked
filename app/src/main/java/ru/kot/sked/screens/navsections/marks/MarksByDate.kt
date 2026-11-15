package ru.kot.sked.screens.navsections.marks

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.kot.sked.DataService
import ru.kot.sked.R
import ru.kot.sked.contentDependentActionLive
import ru.kot.sked.formatToDay
import ru.kot.sked.models.marklistdate.Mark
import ru.kot.sked.parseFromDay
import ru.kot.sked.parseSimpleLongDate


@Composable
fun MarksByDate() {
    val filterState = remember { mutableStateOf(DateMarkFilterType.ByUpdated) }
    contentDependentActionLive.postValue { DateMarkFilter(state = filterState) }

    val daySplitMarks = remember(DataService.marksDate, filterState.value) {
        val payload = DataService.marksDate.payload
        val sorted = when (filterState.value) {
            DateMarkFilterType.ByUpdated -> payload.sortedByDescending { it.updatedAt }
            DateMarkFilterType.ByLessonDate -> payload.sortedByDescending { it.lessonDate }
        }
        val result = mutableListOf<MutableList<Mark>>()
        for (item in sorted) {
            val dateKey = when (filterState.value) {
                DateMarkFilterType.ByUpdated -> item.updatedAt.substringBefore("T")
                DateMarkFilterType.ByLessonDate -> item.lessonDate
            }
            if (result.isEmpty()) {
                result.add(mutableListOf(item))
            } else {
                val lastKey = when (filterState.value) {
                    DateMarkFilterType.ByUpdated -> result.last().first().updatedAt.substringBefore("T")
                    DateMarkFilterType.ByLessonDate -> result.last().first().lessonDate
                }
                if (dateKey != lastKey) {
                    result.add(mutableListOf(item))
                } else {
                    result.last().add(item)
                }
            }
        }
        result
    }

    LazyColumn(Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp)) {
        items(
            items = daySplitMarks,
            key = { group -> group.firstOrNull()?.id ?: group.hashCode() }
        ) {
            MarkDay(marks = it, filterState)
        }
        item(key = "marks_last_week_notice") {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    stringResource(R.string.last_week_marks_are_shown),
                    Modifier.alpha(.8f),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}