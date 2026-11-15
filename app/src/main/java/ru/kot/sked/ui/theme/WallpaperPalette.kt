package ru.kot.sked.ui.theme

import android.graphics.Bitmap
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.ColorUtils

fun extractDominantColor(bitmap: Bitmap): Int {
    val sample = Bitmap.createScaledBitmap(bitmap, 48, 48, false)
    val width = sample.width
    val height = sample.height
    val pixels = IntArray(width * height)
    sample.getPixels(pixels, 0, width, 0, 0, width, height)

    val hsv = FloatArray(3)
    val hueBins = FloatArray(16)
    val binColors = IntArray(16)
    val binMaxWeights = FloatArray(16)

    var maxPixelSat = -1f
    var mostSaturatedPixel = pixels[pixels.size / 2]

    for (pixel in pixels) {
        val alpha = android.graphics.Color.alpha(pixel)
        if (alpha < 128) continue

        android.graphics.Color.colorToHSV(pixel, hsv)
        val s = hsv[1]
        val v = hsv[2]

        if (s > maxPixelSat) {
            maxPixelSat = s
            mostSaturatedPixel = pixel
        }

        // Ignore extreme blacks, whites, and washed-out greys (prefer colorful pixels)
        if (v < 0.22f || v > 0.95f || s < 0.25f) continue

        // Prioritize saturated colors near middle brightness
        val weight = (s * s) * (1f - kotlin.math.abs(v - 0.52f))
        val bin = ((hsv[0] / 360f) * 16).toInt().coerceIn(0, 15)
        hueBins[bin] += weight

        if (weight > binMaxWeights[bin]) {
            binMaxWeights[bin] = weight
            binColors[bin] = pixel
        }
    }

    var bestBin = 0
    var bestWeight = -1f
    for (i in 0 until 16) {
        if (hueBins[i] > bestWeight) {
            bestWeight = hueBins[i]
            bestBin = i
        }
    }

    return if (bestWeight > 0f) {
        binColors[bestBin]
    } else {
        mostSaturatedPixel
    }
}

private fun colorFromHsl(h: Float, s: Float, l: Float): Color {
    val hsl = floatArrayOf(
        (h % 360f + 360f) % 360f,
        s.coerceIn(0f, 1f),
        l.coerceIn(0f, 1f)
    )
    return Color(ColorUtils.HSLToColor(hsl))
}

fun dynamicColorSchemeFromSeed(seedColorInt: Int): Pair<ColorScheme, ColorScheme> {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(seedColorInt, hsl)
    val hue = hsl[0]
    // Always guarantee vivid, rich saturation for accents
    val sat = hsl[1].coerceIn(0.65f, 0.95f)

    // Dark Color Scheme - high vibrancy, excellent contrast against dark cards
    val darkPrimary = colorFromHsl(hue, (sat * 0.95f).coerceIn(0.65f, 0.95f), 0.82f)
    val darkOnPrimary = colorFromHsl(hue, 0.90f, 0.12f)
    val darkPrimaryContainer = colorFromHsl(hue, (sat * 0.85f).coerceIn(0.55f, 0.85f), 0.36f)
    val darkOnPrimaryContainer = colorFromHsl(hue, 0.35f, 0.96f)

    val darkSecondary = colorFromHsl(hue + 20f, (sat * 0.80f).coerceIn(0.50f, 0.80f), 0.80f)
    val darkOnSecondary = colorFromHsl(hue + 20f, 0.85f, 0.12f)
    val darkSecondaryContainer = colorFromHsl(hue + 20f, (sat * 0.70f).coerceIn(0.45f, 0.75f), 0.34f)
    val darkOnSecondaryContainer = colorFromHsl(hue + 20f, 0.30f, 0.96f)

    val darkTertiary = colorFromHsl(hue + 60f, (sat * 0.85f).coerceIn(0.55f, 0.85f), 0.82f)
    val darkOnTertiary = colorFromHsl(hue + 60f, 0.85f, 0.12f)
    val darkTertiaryContainer = colorFromHsl(hue + 60f, (sat * 0.75f).coerceIn(0.45f, 0.80f), 0.35f)
    val darkOnTertiaryContainer = colorFromHsl(hue + 60f, 0.30f, 0.96f)

    val darkScheme = darkColorScheme(
        primary = darkPrimary,
        onPrimary = darkOnPrimary,
        primaryContainer = darkPrimaryContainer,
        onPrimaryContainer = darkOnPrimaryContainer,
        secondary = darkSecondary,
        onSecondary = darkOnSecondary,
        secondaryContainer = darkSecondaryContainer,
        onSecondaryContainer = darkOnSecondaryContainer,
        tertiary = darkTertiary,
        onTertiary = darkOnTertiary,
        tertiaryContainer = darkTertiaryContainer,
        onTertiaryContainer = darkOnTertiaryContainer,
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        background = Color(0xFF131315),
        onBackground = Color(0xFFE5E2E6),
        surface = Color(0xFF131315),
        onSurface = Color(0xFFE5E2E6),
        surfaceVariant = Color(0xFF45464F),
        onSurfaceVariant = Color(0xFFCAC4D0),
        surfaceContainer = Color(0xFF1E1F23),
        surfaceContainerHigh = Color(0xFF282A2F),
        surfaceContainerHighest = Color(0xFF33353B),
        outline = colorFromHsl(hue, 0.30f, 0.65f),
        outlineVariant = Color(0xFF45464F)
    )

    // Light Color Scheme
    val lightPrimary = colorFromHsl(hue, sat.coerceIn(0.65f, 0.95f), 0.38f)
    val lightOnPrimary = Color.White
    val lightPrimaryContainer = colorFromHsl(hue, (sat * 0.40f).coerceIn(0.25f, 0.50f), 0.90f)
    val lightOnPrimaryContainer = colorFromHsl(hue, 0.90f, 0.12f)

    val lightSecondary = colorFromHsl(hue + 20f, (sat * 0.60f).coerceIn(0.35f, 0.65f), 0.42f)
    val lightOnSecondary = Color.White
    val lightSecondaryContainer = colorFromHsl(hue + 20f, (sat * 0.30f).coerceIn(0.20f, 0.40f), 0.91f)
    val lightOnSecondaryContainer = colorFromHsl(hue + 20f, 0.80f, 0.12f)

    val lightTertiary = colorFromHsl(hue + 60f, (sat * 0.70f).coerceIn(0.40f, 0.75f), 0.40f)
    val lightOnTertiary = Color.White
    val lightTertiaryContainer = colorFromHsl(hue + 60f, (sat * 0.35f).coerceIn(0.20f, 0.45f), 0.90f)
    val lightOnTertiaryContainer = colorFromHsl(hue + 60f, 0.80f, 0.12f)

    val lightScheme = lightColorScheme(
        primary = lightPrimary,
        onPrimary = lightOnPrimary,
        primaryContainer = lightPrimaryContainer,
        onPrimaryContainer = lightOnPrimaryContainer,
        secondary = lightSecondary,
        onSecondary = lightOnSecondary,
        secondaryContainer = lightSecondaryContainer,
        onSecondaryContainer = lightOnSecondaryContainer,
        tertiary = lightTertiary,
        onTertiary = lightOnTertiary,
        tertiaryContainer = lightTertiaryContainer,
        onTertiaryContainer = lightOnTertiaryContainer,
        error = Color(0xFFBA1A1A),
        onError = Color.White,
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        background = Color(0xFFFAF9FD),
        onBackground = Color(0xFF1B1B1F),
        surface = Color(0xFFFAF9FD),
        onSurface = Color(0xFF1B1B1F),
        surfaceVariant = Color(0xFFE2E2EC),
        onSurfaceVariant = Color(0xFF45464F),
        surfaceContainer = Color(0xFFF3F3F9),
        surfaceContainerHigh = Color(0xFFEDEDF4),
        surfaceContainerHighest = Color(0xFFE7E7EE),
        outline = colorFromHsl(hue, 0.25f, 0.48f),
        outlineVariant = Color(0xFFC5C6D0)
    )

    return Pair(lightScheme, darkScheme)
}
