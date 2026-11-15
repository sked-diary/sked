package ru.kot.sked.screens.navsections.daybook

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import ru.kot.sked.R
import ru.kot.sked.models.events.Event
import ru.kot.sked.parseLongDate

fun getBreakMinutesFast(endStr: String, nextStartStr: String): Int {
    return try {
        if (endStr.length >= 16 && nextStartStr.length >= 16 &&
            endStr[10] == 'T' && nextStartStr[10] == 'T') {
            val endH = endStr.substring(11, 13).toInt()
            val endM = endStr.substring(14, 16).toInt()
            val startH = nextStartStr.substring(11, 13).toInt()
            val startM = nextStartStr.substring(14, 16).toInt()
            (startH * 60 + startM) - (endH * 60 + endM)
        } else {
            val currentEndDate = endStr.parseLongDate()
            val nextStartDate = nextStartStr.parseLongDate()
            ((nextStartDate.time - currentEndDate.time) / 60000).toInt()
        }
    } catch (_: Exception) {
        0
    }
}

fun LazyListScope.DayItem(
    day: List<Event>,
    showLessonNumbers: Boolean,
    showBreaks: Boolean,
    reversed: Boolean = false,
    addBelow: @Composable () -> Unit = {},
) {
    val sortedDay = day.sortedBy { it.startAt }
    val targetList = if (reversed) sortedDay.asReversed() else sortedDay
    val listSize = targetList.size
    val lastIdx = listSize - 1

    val lessonNumbers = IntArray(listSize)
    var planCount = 0
    for (i in 0 until listSize) {
        if (targetList[i].source == "PLAN") {
            lessonNumbers[i] = planCount++
        } else {
            lessonNumbers[i] = -1
        }
    }

    itemsIndexed(
        items = targetList,
        key = { index, item -> item.id ?: "${item.startAt}_$index" }
    ) { index, it ->
        val cardShape = when {
            listSize == 1 -> MaterialTheme.shapes.large
            index == 0 -> MaterialTheme.shapes.extraSmall.copy(
                topStart = MaterialTheme.shapes.large.topStart,
                topEnd = MaterialTheme.shapes.large.topEnd
            )
            index == lastIdx -> MaterialTheme.shapes.extraSmall.copy(
                bottomStart = MaterialTheme.shapes.large.bottomStart,
                bottomEnd = MaterialTheme.shapes.large.bottomEnd
            )
            else -> MaterialTheme.shapes.extraSmall
        }
        val cardColor = MaterialTheme.colorScheme.run {
            when (it.source) {
                "AE" -> secondaryContainer
                "EC" -> primaryContainer
                "EVENTS" -> tertiaryContainer
                else -> surfaceContainer
            }
        }
        Card(
            Modifier
                .padding(bottom = 2.dp)
                .fillMaxWidth(),
            shape = cardShape,
            colors = CardDefaults.cardColors(containerColor = cardColor)
        ) {
            EventItem(
                event = it,
                index = lessonNumbers[index],
                showLessonNumbers = showLessonNumbers
            )
        }
        if ((index != lastIdx) && showBreaks && !reversed) {
            val breakDuration = getBreakMinutesFast(it.finishAt, targetList[index + 1].startAt)
            if (breakDuration > 0) {
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.85f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 2.dp)
                ) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            pluralStringResource(R.plurals.minute_break, breakDuration, breakDuration),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
    item(key = "day_item_add_below") {
        addBelow()
    }
}