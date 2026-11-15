package ru.kot.sked.ui.theme

import androidx.compose.material3.ColorScheme

enum class CustomColorScheme(
    val lightColorScheme: ColorScheme,
    val darkColorScheme: ColorScheme
) {
    Blue(
        ru.kot.sked.ui.theme.blue.LightColorScheme,
        ru.kot.sked.ui.theme.blue.DarkColorScheme
    ),
    Brown(
        ru.kot.sked.ui.theme.brown.LightColorScheme,
        ru.kot.sked.ui.theme.brown.DarkColorScheme
    ),
    Green(
        ru.kot.sked.ui.theme.green.LightColorScheme,
        ru.kot.sked.ui.theme.green.DarkColorScheme
    ),
    Pink(
        ru.kot.sked.ui.theme.pink.LightColorScheme,
        ru.kot.sked.ui.theme.pink.DarkColorScheme
    ),
    Purple(
        ru.kot.sked.ui.theme.purple.LightColorScheme,
        ru.kot.sked.ui.theme.purple.DarkColorScheme
    ),
    Teal(
        ru.kot.sked.ui.theme.teal.LightColorScheme,
        ru.kot.sked.ui.theme.teal.DarkColorScheme
    ),
    Yellow(
        ru.kot.sked.ui.theme.yellow.LightColorScheme,
        ru.kot.sked.ui.theme.yellow.DarkColorScheme
    )
}