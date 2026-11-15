package ru.kot.sked.screens.navsections.marks

import androidx.annotation.StringRes
import ru.kot.sked.R

enum class SubjectMarkFilterType(
    @StringRes val title: Int
) {
    ByAverage(R.string.mark_filter_by_average),
    ByRanking(R.string.mark_filter_by_ranking),
    ByUpdated(R.string.mark_filter_by_last_update),
    Alphabetical(R.string.mark_filter_alphabetical)
}