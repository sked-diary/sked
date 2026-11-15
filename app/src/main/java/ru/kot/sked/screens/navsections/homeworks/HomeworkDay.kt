package ru.kot.sked.screens.navsections.homeworks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import ru.kot.sked.formatToLongHumanDay
import ru.kot.sked.formatToWeekday
import ru.kot.sked.models.homeworks.Homework
import ru.kot.sked.parseFromDay

@Composable
fun HomeworkDay(homeworks: List<ru.kot.sked.models.homeworks2.Homework>) {
    val enabledSubjects = enabledSubjectsLive.observeAsState()
    val enabled = enabledSubjects.value
    val isVisible = enabled == null || enabled.isEmpty() || homeworks.any { it.subjectId in enabled }
    AnimatedVisibility(visible = isVisible) {
        Column(Modifier.padding(bottom = 16.dp)) {
            val date = homeworks[0].date.parseFromDay()
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
            val subjectSplitHomeworks = remember(homeworks) {
                homeworks.groupBy { it.subjectId }.values.toList()
            }
            Column(
                Modifier.clip(MaterialTheme.shapes.large),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                subjectSplitHomeworks.forEach {
                    Card(
                        Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraSmall,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        val subjectVisible = enabled == null || enabled.isEmpty() || it.first().subjectId in enabled
                        AnimatedVisibility(visible = subjectVisible) {
                            HomeworkSubject(homeworks = it)
                        }
                    }
                }
            }
        }
    }
}