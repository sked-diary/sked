package ru.kot.sked.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.kot.sked.models.mark.ClassResults
import ru.kot.sked.models.mark.MarksDistribution
import ru.kot.sked.models.mark.MarkValue
import ru.kot.sked.network.gosuslugi.GosuslugiDataBridge
import java.util.Locale
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottomAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStartAxis
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.ColumnCartesianLayerModel
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.core.common.component.LineComponent
import com.patrykandpatrick.vico.core.common.data.ExtraStore
import com.patrykandpatrick.vico.core.common.shape.Shape
import ru.kot.sked.CloverShape
import ru.kot.sked.DataService
import ru.kot.sked.NavSection
import ru.kot.sked.R
import ru.kot.sked.components.settings.CommonPrefs
import ru.kot.sked.get
import ru.kot.sked.getDemoProperty
import ru.kot.sked.getMarkConfig
import ru.kot.sked.isDemo
import ru.kot.sked.mainPrefs
import ru.kot.sked.modalBottomSheetContentLive
import ru.kot.sked.modalBottomSheetStateLive
import ru.kot.sked.models.events.Mark
import ru.kot.sked.authPrefs
import ru.kot.sked.models.mark.Activity
import ru.kot.sked.models.mark.Teacher
import ru.kot.sked.models.mark.MarkInfo
import ru.kot.sked.models.marklistsubject.MarkListSubjectItem
import ru.kot.sked.navControllerLive
import ru.kot.sked.parseSimpleLongAndFormatToLong
import ru.kot.sked.rememberMarker
import ru.kot.sked.screens.navsections.marks.SubjectRatingBottomSheet
import ru.kot.sked.screens.navsections.marks.scrollToSubjectIdLive
import kotlin.math.roundToInt

data class MarkConfig(
    val hideDefaultWeight: Boolean,
    val markHighlighting: Boolean,
)

@Composable
fun MarkComp(
    mark: Mark,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    subjectId: Long,
    markConfig: MarkConfig,
    showPlus: Boolean = false,
    onClick: (Mark, Long) -> Unit = ::defaultMarkClick,
) {
    val (containerColor, contentColor) = MaterialTheme.colorScheme.run {
        if (markConfig.markHighlighting) {
            when (mark.value) {
                "5" -> Pair(primaryContainer, onPrimaryContainer)
                "4" -> Pair(secondaryContainer, onSecondaryContainer)
                "3" -> Pair(tertiaryContainer, onTertiaryContainer)
                "2", "1" -> Pair(errorContainer, onErrorContainer)
                else -> Pair(secondaryContainer, onSecondaryContainer)
            }
        } else Pair(secondaryContainer, onSecondaryContainer)
    }

    FilledTonalIconButton(
        onClick = { if (enabled) onClick(mark, subjectId) },
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(2.dp)
        ) {
            Text(
                mark.value ?: "",
                Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
            if ((!markConfig.hideDefaultWeight || mark.weight != 1) || showPlus) {
                Text(
                    if (showPlus) "+" else mark.weight.toString(),
                    Modifier.align(Alignment.BottomEnd),
                    style = MaterialTheme.typography.labelMedium,
                    color = contentColor.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Composable
fun MarkSimple(value: String, markConfig: MarkConfig, modifier: Modifier = Modifier) {
    val (containerColor, contentColor) = MaterialTheme.colorScheme.run {
        if (markConfig.markHighlighting) {
            when (value) {
                "5" -> Pair(primaryContainer, onPrimaryContainer)
                "4" -> Pair(secondaryContainer, onSecondaryContainer)
                "3" -> Pair(tertiaryContainer, onTertiaryContainer)
                "2", "1" -> Pair(errorContainer, onErrorContainer)
                else -> Pair(secondaryContainer, onSecondaryContainer)
            }
        } else Pair(secondaryContainer, onSecondaryContainer)
    }

    Box(
        modifier
            .size(40.dp)
            .background(containerColor, MaterialTheme.shapes.small)
    ) {
        Text(
            value,
            Modifier.align(Alignment.Center),
            color = contentColor,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun MarkSheetContent(mark: Mark, subjectId: Long) {
    var markInfo by remember { mutableStateOf<MarkInfo?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val isGosuslugi = context.authPrefs.get<String>("cookie") != null
    val subject = remember {
        if (DataService.hasMarksSubject) {
            DataService.marksSubject.firstOrNull { it.subjectId == subjectId }
        } else null
    }
    LaunchedEffect(Unit) {
        if (context.isDemo) {
            markInfo = context.getDemoProperty(R.raw.demo_mark_info)
        } else if (isGosuslugi) {
            val dateMark = DataService.marksDate.payload.firstOrNull { it.id == mark.id }
            val subjectName = dateMark?.subjectName ?: subject?.subjectName ?: ""
            val gMark = GosuslugiDataBridge.getMarkById(mark.id)
            val teachers = try { DataService.schoolInfo.teachers } catch (e: Exception) { emptyList() }
            val matchedTeacher = teachers.firstOrNull { t ->
                t.subjectNames.any { s ->
                    s.contains(subjectName, ignoreCase = true) || subjectName.contains(s, ignoreCase = true)
                }
            }
            val tLastName = matchedTeacher?.lastName ?: "Учитель"
            val tFirstName = matchedTeacher?.firstName ?: ""
            val tMiddleName = matchedTeacher?.middleName ?: ""
            val lDate = gMark?.date ?: dateMark?.lessonDate ?: "2026-11-10"
            val isoDate = "${lDate}T12:00:00"

            val classResults = gMark?.studentsTest?.let { testMarks ->
                if (testMarks.isNotEmpty()) {
                    val total = testMarks.size
                    val scores = listOf(5, 4, 3, 2, 1)
                    val distributions = scores.mapNotNull { score ->
                        val count = testMarks.count { tm ->
                            tm.value?.trim() == score.toString() || tm.value2?.trim() == score.toString()
                        }
                        if (count > 0) {
                            val pct = ((count.toDouble() / total) * 100).roundToInt()
                            MarksDistribution(
                                markValue = MarkValue(five = score, origin = null, originalGradeFrom = null, originalGradeTo = null),
                                numberOfStudents = count,
                                percentageOfStudents = pct
                            )
                        } else null
                    }
                    if (distributions.isNotEmpty()) {
                        ClassResults(
                            marksDistributions = distributions,
                            totalStudents = total
                        )
                    } else null
                } else null
            }

            val controlName = gMark?.workName ?: gMark?.workType ?: mark.controlFormName.ifEmpty { "Обычная отметка" }
            val commentText = gMark?.comment ?: mark.comment

            markInfo = MarkInfo(
                activity = Activity("", 0),
                classResults = classResults,
                comment = commentText,
                commentExists = !commentText.isNullOrEmpty(),
                controlFormName = controlName,
                createdAt = isoDate,
                criteria = null,
                date = lDate,
                history = emptyList(),
                id = mark.id,
                isExam = mark.isExam,
                isPoint = mark.isPoint,
                originalGradeSystemType = "five",
                pointDate = null,
                resultFiles = null,
                teacher = Teacher(null, tFirstName, tLastName, tMiddleName, null, null),
                updatedAt = isoDate,
                value = gMark?.value ?: mark.value,
                values = emptyList(),
                weight = gMark?.weight?.toIntOrNull() ?: mark.weight
            )
        } else {
            DataService.getMarkInfo(mark.id, { errorMessage = it }) { markInfo = it }
        }
    }

    Box(
        Modifier
            .heightIn(192.dp, Int.MAX_VALUE.dp)
            .fillMaxWidth()
    ) {
        if (markInfo != null) {
            MarkInfo(markInfo!!, subject, mark)
        } else if (errorMessage == null) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        } else {
            ErrorMessage(Modifier.align(Alignment.Center), errorMessage!!)
        }
    }
}

@Composable
fun BoxScope.MarkInfo(markInfo: MarkInfo, subject: MarkListSubjectItem?, mark: Mark) {
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        MarkDescription(subject, markInfo)
        if (markInfo.classResults != null) {
            ClassResults(markInfo)
        }
    }
    Column(
        Modifier
            .align(Alignment.TopEnd)
            .padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MarkComp(mark, enabled = false, subjectId = 0L, markConfig = getMarkConfig())
        if (subject != null) {
            RatingButton(subject)
            AverageChip(subject)
        }
    }
}

@Composable
fun MarkDescription(subject: MarkListSubjectItem?, markInfo: MarkInfo) {
    Column(Modifier.padding(end = 72.dp)) {
        if (subject != null) {
            Text(
                subject.subjectName,
                style = MaterialTheme.typography.titleMedium
            )
        } else {
            Text(
                stringResource(R.string.mark),
                style = MaterialTheme.typography.titleMedium
            )
        }
        with(markInfo) {
            Text(teacher.run { "$lastName $firstName $middleName" })
            Text(controlFormName)
            if (commentExists) Text(comment!!)
            Text(
                stringResource(
                    R.string.mark_created,
                    parseSimpleLongAndFormatToLong(
                        updatedAt,
                        stringResource(id = R.string.at_time)
                    )
                ),
                Modifier.padding(vertical = 8.dp)
            )
        }
    }
}

private fun pluralStudents(count: Int): String {
    val mod10 = count % 10
    val mod100 = count % 100
    return when {
        mod100 in 11..19 -> "$count учеников"
        mod10 == 1 -> "$count ученик"
        mod10 in 2..4 -> "$count ученика"
        else -> "$count учеников"
    }
}

@Composable
fun ClassResults(markInfo: MarkInfo) {
    val results = markInfo.classResults?.marksDistributions?.sortedByDescending { it.markValue.five }
        ?: return
    val totalStudents = markInfo.classResults.totalStudents.takeIf { it > 0 }
        ?: results.sumOf { it.numberOfStudents }
    val totalPoints = results.sumOf { it.markValue.five * it.numberOfStudents }
    val classAverage = if (totalStudents > 0) totalPoints.toDouble() / totalStudents else 0.0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Title + Average Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f, fill = false)) {
                    Text(
                        stringResource(R.string.class_results),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        pluralStudents(totalStudents),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.TrendingUp,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = String.format(Locale.US, "Ср. балл %.2f", classAverage),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Distribution list
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                results.forEach { dist ->
                    val score = dist.markValue.five
                    val count = dist.numberOfStudents
                    val percent = dist.percentageOfStudents
                    val isMyMark = score.toString() == markInfo.value

                    val rowContainerColor = if (isMyMark) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
                    }

                    val (badgeBg, badgeFg) = when (score) {
                        5 -> Pair(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary)
                        4 -> Pair(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.onSecondary)
                        3 -> Pair(MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.onTertiary)
                        2, 1 -> Pair(MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.onError)
                        else -> Pair(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = rowContainerColor,
                        border = if (isMyMark) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)) else null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(badgeBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = score.toString(),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = badgeFg
                                    )
                                }

                                Spacer(Modifier.width(12.dp))

                                Text(
                                    text = pluralStudents(count),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isMyMark) FontWeight.SemiBold else FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )

                                if (isMyMark) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(end = 8.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.Person,
                                            contentDescription = "Ваша отметка",
                                            modifier = Modifier
                                                .size(18.dp)
                                                .padding(2.dp),
                                            tint = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }

                                Text(
                                    text = "$percent%",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isMyMark) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Progress bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction = (percent / 100f).coerceIn(0.02f, 1f))
                                        .fillMaxHeight()
                                        .clip(CircleShape)
                                        .background(badgeBg)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RatingButton(subject: MarkListSubjectItem) {
    val context = LocalContext.current
    if (context.mainPrefs.get(CommonPrefs.subjectRating.prefKey) ?: true) {
        DataService.subjectRanking.firstOrNull { it.subjectId == subject.subjectId }
            ?.let {
                FilledIconButton(
                    {
                        modalBottomSheetContentLive.postValue {
                            SubjectRatingBottomSheet(
                                subject.subjectId,
                                subject.subjectName
                            )
                        }
                    },
                    Modifier.padding(top = 6.dp),
                    shape = CloverShape
                ) {
                    Text(
                        it.rank.rankPlace.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
    }
}

@Composable
fun AverageChip(subject: MarkListSubjectItem) {
    val navController = navControllerLive.observeAsState().value
    if (subject.currentPeriod != null) {
        Row(
            Modifier
                .padding(top = 8.dp)
                .clip(CircleShape)
                .let {
                    if (navController != null) {
                        it.clickable {
                            modalBottomSheetStateLive.postValue(false)
                            scrollToSubjectIdLive.value = subject.subjectId
                            navController.navigate(route = NavSection.Marks.route)
                        }
                    } else it
                }
                .background(MaterialTheme.colorScheme.tertiaryContainer, CircleShape)
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val textStyle = MaterialTheme.typography.labelLarge
            Icon(
                Icons.AutoMirrored.Rounded.TrendingUp,
                stringResource(R.string.average_mark),
                Modifier
                    .padding(horizontal = 4.dp)
                    .size(textStyle.fontSize.value.dp),
                MaterialTheme.colorScheme.tertiary
            )
            Text(
                subject.currentPeriod?.fixedValue ?: subject.currentPeriod?.value ?: "",
                Modifier.padding(end = 4.dp),
                color = MaterialTheme.colorScheme.tertiary,
                style = textStyle
            )
        }
    }
}

fun defaultMarkClick(mark: Mark, subjectId: Long) {
    modalBottomSheetStateLive.postValue(true)
    modalBottomSheetContentLive.postValue { MarkSheetContent(mark, subjectId) }
}