package org.opencult.scale.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

val LocalOpenCultColors = staticCompositionLocalOf { DaylightColors }
val LocalOpenCultTypography = staticCompositionLocalOf { DefaultOpenCultTypography }
val LocalOpenCultShapes = staticCompositionLocalOf { OpenCultShapes() }
val LocalOpenCultSpacing = staticCompositionLocalOf { OpenCultSpacing() }

enum class OpenCultThemeMode {
    SYSTEM,
    DAYLIGHT,
    OBSIDIAN,
}

object OpenCultTheme {
    val colors: OpenCultColors
        @Composable
        @ReadOnlyComposable
        get() = LocalOpenCultColors.current

    val typography: OpenCultTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalOpenCultTypography.current

    val shapes: OpenCultShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalOpenCultShapes.current

    val spacing: OpenCultSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalOpenCultSpacing.current

    val reducedMotion: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalReducedMotion.current
}

@Composable
fun OpenCultTheme(
    mode: OpenCultThemeMode = OpenCultThemeMode.DAYLIGHT,
    reducedMotionOverride: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (mode) {
        OpenCultThemeMode.DAYLIGHT -> false
        OpenCultThemeMode.OBSIDIAN -> true
        OpenCultThemeMode.SYSTEM -> systemDark
    }
    val colors = if (useDark) ObsidianColors else DaylightColors
    val context = LocalContext.current
    val reducedMotion = reducedMotionOverride ?: remember(context) {
        OpenCultMotion.isSystemReducedMotion(context)
    }

    val m3Scheme = remember(colors) {
        if (colors.isDark) {
            darkColorScheme(
                primary = colors.brandCobalt,
                onPrimary = colors.dockActiveIcon,
                background = colors.canvas,
                onBackground = colors.textPrimary,
                surface = colors.cardSurface,
                onSurface = colors.textPrimary,
                surfaceVariant = colors.elevatedSurface,
                onSurfaceVariant = colors.textSecondary,
                outline = colors.borderSubtle,
                error = colors.coralAlert,
            )
        } else {
            lightColorScheme(
                primary = colors.brandCobalt,
                onPrimary = colors.dockActiveIcon,
                background = colors.canvas,
                onBackground = colors.textPrimary,
                surface = colors.cardSurface,
                onSurface = colors.textPrimary,
                surfaceVariant = colors.elevatedSurface,
                onSurfaceVariant = colors.textSecondary,
                outline = colors.borderSubtle,
                error = colors.coralAlert,
            )
        }
    }

    CompositionLocalProvider(
        LocalOpenCultColors provides colors,
        LocalOpenCultTypography provides DefaultOpenCultTypography,
        LocalOpenCultShapes provides OpenCultShapes(),
        LocalOpenCultSpacing provides OpenCultSpacing(),
        LocalReducedMotion provides reducedMotion,
    ) {
        MaterialTheme(
            colorScheme = m3Scheme,
            content = content,
        )
    }
}
