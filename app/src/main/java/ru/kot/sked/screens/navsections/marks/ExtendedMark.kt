package ru.kot.sked.screens.navsections.marks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.kot.sked.components.MarkComp
import ru.kot.sked.components.defaultMarkClick
import ru.kot.sked.getMarkConfig
import ru.kot.sked.models.marklistdate.Mark

@Composable
fun ExtendedMark(mark: Mark) {
    val eventMark = ru.kot.sked.models.events.Mark.fromMarkListDate(mark)
    Column(Modifier.clickable { defaultMarkClick(eventMark, mark.subjectId) }) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text(mark.subjectName ?: "", style = MaterialTheme.typography.titleMedium)
                Text(mark.controlFormName ?: "")
            }
            MarkComp(eventMark, subjectId = mark.subjectId, markConfig = getMarkConfig())
        }
    }
}