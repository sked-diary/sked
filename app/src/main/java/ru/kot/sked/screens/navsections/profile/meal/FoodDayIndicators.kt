package ru.kot.sked.screens.navsections.profile.meal

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BakeryDining
import androidx.compose.material.icons.outlined.LocalDining
import androidx.compose.ui.graphics.vector.ImageVector
import ru.kot.sked.models.daysbalanceinfo.Day

enum class FoodDayIndicators(
    val icon: ImageVector,
    @StringRes val descriptionRes: Int,
    val condition: (Day) -> Boolean,
) {
    DINING(
        icon = Icons.Outlined.LocalDining,
        descriptionRes = ru.kot.sked.R.string.dining,
        condition = { it.transactions.any { it.type == "DINING"} }
    ),
    BUFFET(
        icon = Icons.Outlined.BakeryDining,
        descriptionRes = ru.kot.sked.R.string.buffet,
        condition = { it.transactions.any { it.type == "BUFFET"} }
    )
}