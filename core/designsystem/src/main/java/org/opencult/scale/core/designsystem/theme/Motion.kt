package org.opencult.scale.core.designsystem.theme

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset

val LocalReducedMotion = staticCompositionLocalOf { false }

/**
 * Motion tokens locked to MOTIONENGINEERING.md §2 and §5 (reduced-motion parity).
 * No screen composable should declare inline spring() or durationMillis constants.
 */
object OpenCultMotion {
    const val SheetEnterMs = 260
    const val SheetExitMs = 190
    const val ScrimFadeMs = 200
    const val StageRiseMs = 220
    const val ListAppendMs = 200
    const val AmbientRadarCycleMs = 2600
    const val MaxStaggerCapMs = 160
    const val CardPressScale = 0.97f
    const val FabPressScale = 0.95f
    const val RowPressScale = 0.98f

    val SheetEnterEasing = CubicBezierEasing(0.20f, 0.80f, 0.20f, 1.00f)
    val SheetExitEasing = CubicBezierEasing(0.40f, 0.00f, 1.00f, 1.00f)

    fun <T> spatialDefault(reducedMotion: Boolean): FiniteAnimationSpec<T> =
        if (reducedMotion) snap() else spring(dampingRatio = 0.82f, stiffness = 420f)

    fun <T> pressFeedback(reducedMotion: Boolean): FiniteAnimationSpec<T> =
        if (reducedMotion) snap() else spring(dampingRatio = 0.78f, stiffness = 560f)

    fun <T> telemetryGlide(reducedMotion: Boolean): FiniteAnimationSpec<T> =
        if (reducedMotion) snap() else spring(dampingRatio = 0.90f, stiffness = 280f)

    fun <T> sheetEnter(reducedMotion: Boolean): FiniteAnimationSpec<T> =
        if (reducedMotion) snap() else tween(durationMillis = SheetEnterMs, easing = SheetEnterEasing)

    fun <T> sheetExit(reducedMotion: Boolean): FiniteAnimationSpec<T> =
        if (reducedMotion) snap() else tween(durationMillis = SheetExitMs, easing = SheetExitEasing)

    fun <T> scrimFade(reducedMotion: Boolean): FiniteAnimationSpec<T> =
        if (reducedMotion) snap() else tween(durationMillis = ScrimFadeMs)

    fun <T> stageRise(reducedMotion: Boolean): FiniteAnimationSpec<T> =
        if (reducedMotion) snap() else tween(durationMillis = StageRiseMs, easing = SheetEnterEasing)

    fun staggerDelay(index: Int, reducedMotion: Boolean): Int =
        if (reducedMotion) 0 else minOf(index * 40, MaxStaggerCapMs)

    fun springDp(reducedMotion: Boolean): FiniteAnimationSpec<Dp> = spatialDefault(reducedMotion)
    fun springOffset(reducedMotion: Boolean): FiniteAnimationSpec<IntOffset> = spatialDefault(reducedMotion)

    fun tabContentTransition(forward: Boolean, reducedMotion: Boolean): ContentTransform {
        if (reducedMotion) return EnterTransition.None togetherWith ExitTransition.None
        val dir = if (forward) 1 else -1
        return (slideInHorizontally(animationSpec = spatialDefault(false)) { fullWidth ->
            (fullWidth * 0.14f).toInt() * dir
        } + fadeIn(animationSpec = scrimFade(false))) togetherWith
            (slideOutHorizontally(animationSpec = spatialDefault(false)) { fullWidth ->
                -(fullWidth * 0.10f).toInt() * dir
            } + fadeOut(animationSpec = scrimFade(false)))
    }

    fun onboardingRootTransition(reducedMotion: Boolean): ContentTransform {
        if (reducedMotion) return EnterTransition.None togetherWith ExitTransition.None
        return (fadeIn(animationSpec = stageRise(false)) +
            slideInVertically(animationSpec = spatialDefault(false)) { h -> (h * 0.08f).toInt() }) togetherWith
            (fadeOut(animationSpec = scrimFade(false)))
    }

    fun sheetOverlayTransition(entering: Boolean, reducedMotion: Boolean): ContentTransform {
        if (reducedMotion) return EnterTransition.None togetherWith ExitTransition.None
        return if (entering) {
            (slideInVertically(animationSpec = spatialDefault(false)) { h -> h / 4 } +
                fadeIn(animationSpec = sheetEnter(false))) togetherWith
                fadeOut(animationSpec = scrimFade(false))
        } else {
            fadeIn(animationSpec = scrimFade(false)) togetherWith
                (slideOutVertically(animationSpec = sheetExit(false)) { h -> h / 4 } +
                    fadeOut(animationSpec = sheetExit(false)))
        }
    }

    fun detailOverlayTransition(entering: Boolean, reducedMotion: Boolean): ContentTransform {
        if (reducedMotion) return EnterTransition.None togetherWith ExitTransition.None
        return if (entering) {
            (slideInHorizontally(animationSpec = spatialDefault(false)) { w -> w / 3 } +
                fadeIn(animationSpec = stageRise(false))) togetherWith
                (slideOutHorizontally(animationSpec = spatialDefault(false)) { w -> -w / 6 } +
                    fadeOut(animationSpec = scrimFade(false)))
        } else {
            (slideInHorizontally(animationSpec = spatialDefault(false)) { w -> -w / 6 } +
                fadeIn(animationSpec = scrimFade(false))) togetherWith
                (slideOutHorizontally(animationSpec = sheetExit(false)) { w -> w / 3 } +
                    fadeOut(animationSpec = sheetExit(false)))
        }
    }

    fun isSystemReducedMotion(context: Context): Boolean = try {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1.0f,
        ) == 0.0f
    } catch (_: Throwable) {
        false
    }
}
