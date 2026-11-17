package app.reelvocab.core.designsystem.theme

import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

private fun ReelColors.toMaterialScheme(dark: Boolean) = if (dark) {
    darkColorScheme(
        primary = gold,
        onPrimary = onAccent,
        primaryContainer = goldDim,
        onPrimaryContainer = textPrimary,
        secondary = teal,
        onSecondary = onAccent,
        secondaryContainer = elevated,
        onSecondaryContainer = textPrimary,
        tertiary = teal,
        onTertiary = onAccent,
        background = base,
        onBackground = textPrimary,
        surface = surface,
        onSurface = textPrimary,
        surfaceVariant = elevated,
        onSurfaceVariant = textSecondary,
        surfaceContainerLowest = base,
        surfaceContainerLow = surface,
        surfaceContainer = surface,
        surfaceContainerHigh = elevated,
        surfaceContainerHighest = elevated,
        outline = strokeSubtle,
        outlineVariant = strokeSubtle,
        error = rose,
        onError = onAccent,
        inverseSurface = textPrimary,
        inverseOnSurface = base,
        scrim = Color.Black,
    )
} else {
    lightColorScheme(
        primary = gold,
        onPrimary = onAccent,
        primaryContainer = goldDim,
        onPrimaryContainer = textPrimary,
        secondary = teal,
        onSecondary = Color.White,
        secondaryContainer = elevated,
        onSecondaryContainer = textPrimary,
        tertiary = teal,
        onTertiary = Color.White,
        background = base,
        onBackground = textPrimary,
        surface = surface,
        onSurface = textPrimary,
        surfaceVariant = elevated,
        onSurfaceVariant = textSecondary,
        surfaceContainerLowest = surface,
        surfaceContainerLow = surface,
        surfaceContainer = elevated,
        surfaceContainerHigh = elevated,
        surfaceContainerHighest = strokeSubtle,
        outline = strokeSubtle,
        outlineVariant = strokeSubtle,
        error = rose,
        onError = Color.White,
        inverseSurface = textPrimary,
        inverseOnSurface = surface,
        scrim = Color.Black,
    )
}

val LocalReelColors = staticCompositionLocalOf { ReelDarkColors }
val LocalReelMotion = staticCompositionLocalOf { ReelMotion.Standard }
val LocalReelHaptics = staticCompositionLocalOf<ReelHaptics> { NoopReelHaptics }

@Composable
fun ReelTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val reelColors = if (darkTheme) ReelDarkColors else ReelLightColors
    val scheme = remember(darkTheme) { reelColors.toMaterialScheme(darkTheme) }
    val motion = rememberReelMotion()
    val haptics = rememberReelHaptics()

    CompositionLocalProvider(
        LocalReelColors provides reelColors,
        LocalReelMotion provides motion,
        LocalReelHaptics provides haptics,
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = ReelTypography.toMaterialTypography(),
            shapes = ReelShapes,
            content = content,
        )
    }
}

object ReelTheme {
    val colors: ReelColors
        @Composable @ReadOnlyComposable get() = LocalReelColors.current

    val motion: ReelMotion
        @Composable @ReadOnlyComposable get() = LocalReelMotion.current

    val haptics: ReelHaptics
        @Composable @ReadOnlyComposable get() = LocalReelHaptics.current

    val spacing: ReelSpacing get() = DefaultReelSpacing

    val radius: ReelRadius get() = DefaultReelRadius

    val type: ReelType get() = ReelTypography
}

@Composable
fun rememberReelMotion(): ReelMotion {
    val context = LocalContext.current
    val reduced = remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) == 0f
        }.getOrDefault(false)
    }
    return if (reduced) ReelMotion.Reduced else ReelMotion.Standard
}

@Composable
fun rememberReelHaptics(): ReelHaptics {
    val view = LocalView.current
    return remember(view) { ReelHaptics(view) }
}

val NoopReelHaptics = ReelHaptics(null)
