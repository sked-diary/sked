package ru.kot.sked.components.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ru.kot.sked.R

object CommonPrefs {
    val breaks = SwitchPreferenceSpec(
        titleRes = R.string.show_breaks,
        prefKey = "breaks",
        defaultValue = true
    )
    val showLessonNumbers = SwitchPreferenceSpec(
        titleRes = R.string.show_lesson_numbers,
        prefKey = "show_lesson_numbers",
        defaultValue = true
    )
    val showOnlyPlan = SwitchPreferenceSpec(
        titleRes = R.string.show_only_plan,
        descriptionRes = R.string.show_only_plan_desc,
        prefKey = "show_only_plan",
        defaultValue = false
    )
    val weekStartsAlwaysMonday = SwitchPreferenceSpec(
        titleRes = R.string.week_starts_always_on_monday,
        descriptionRes = R.string.week_starts_always_on_monday_desc,
        prefKey = "week_starts_always_on_monday",
        defaultValue = false
    )
    val mainRating = SwitchPreferenceSpec(
        titleRes = R.string.main_rating,
        prefKey = "main_rating",
        defaultValue = true
    )
    val subjectRating = SwitchPreferenceSpec(
        titleRes = R.string.subject_rating,
        prefKey = "subject_rating",
        defaultValue = true
    )
    val hideDefaultWeight = SwitchPreferenceSpec(
        titleRes = R.string.hide_default_weight,
        prefKey = "hide_default_weight",
        defaultValue = true
    )
    val markHighlighting = SwitchPreferenceSpec(
        titleRes = R.string.mark_highlighting,
        descriptionRes = R.string.mark_highlighting_desc,
        prefKey = "mark_highlighting",
        defaultValue = true
    )
}

@Composable
fun Common() {
    val isMesSubsystem = ru.kot.sked.DataService.subsystem == ru.kot.sked.Diary.MES &&
            ru.kot.sked.DataService.token != "gosuslugi"

    with(CommonPrefs) {
        Category(stringResource(R.string.diary)) {
            breaks.BasicSwitchPreference()
            showLessonNumbers.BasicSwitchPreference()
            if (isMesSubsystem) {
                showOnlyPlan.BasicSwitchPreference()
            }
            weekStartsAlwaysMonday.BasicSwitchPreference()
        }
        if (isMesSubsystem) {
            Category(stringResource(R.string.ratings)) {
                mainRating.BasicSwitchPreference()
                subjectRating.BasicSwitchPreference()
            }
        }
        Category(stringResource(R.string.marks)) {
            hideDefaultWeight.BasicSwitchPreference()
            markHighlighting.BasicSwitchPreference()
        }
    }
}