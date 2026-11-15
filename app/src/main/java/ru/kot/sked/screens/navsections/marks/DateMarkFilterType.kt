package ru.kot.sked.screens.navsections.marks

import androidx.annotation.StringRes
import ru.kot.sked.R

enum class DateMarkFilterType(
    @StringRes val title: Int
) {
    ByUpdated(R.string.mark_filter_by_updated),
    ByLessonDate(R.string.mark_filter_by_lesson_date)
}