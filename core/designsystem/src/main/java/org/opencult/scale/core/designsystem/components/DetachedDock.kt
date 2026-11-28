package org.opencult.scale.core.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import org.opencult.scale.core.designsystem.theme.OpenCultMotion
import org.opencult.scale.core.designsystem.theme.OpenCultTheme

enum class OpenCultDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    HOME("home", "Home", Icons.Rounded.Home),
    BODY_MAP("bodymap", "Body Map", Icons.Rounded.AccessibilityNew),
    REPORTS("reports", "Reports", Icons.Rounded.BarChart),
    PROFILES("profiles", "Profiles", Icons.Rounded.PersonOutline),
}

/**
 * DRD:C-05 Detached Floating Action Dock:
 * Separated `4`-tab `#0F1115` capsule (`64.dp`, `weight(1f)`) + `12.dp` gap + `64.dp` Cobalt `+` FAB
 * resting above a translucent canvas fade scrim so scrolling cards dissolve smoothly behind it.
 */
@Composable
fun DetachedObsidianDock(
    selectedDestination: OpenCultDestination,
    onSelectDestination: (OpenCultDestination) -> Unit,
    onMeasureClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OpenCultTheme.colors
    val spacing = OpenCultTheme.spacing
    val reducedMotion = OpenCultTheme.reducedMotion

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        colors.canvas.copy(alpha = 0f),
                        colors.canvas.copy(alpha = 0.85f),
                        colors.canvas,
                    ),
                ),
            )
            .navigationBarsPadding()
            .padding(
                start = spacing.dockHorizontalInset,
                end = spacing.dockHorizontalInset,
                top = 20.dp,
                bottom = spacing.dockBottomMargin,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .height(spacing.dockHeight)
                    .shadow(
                        elevation = 16.dp,
                        shape = OpenCultTheme.shapes.dockPill,
                        ambientColor = colors.dockBackground.copy(alpha = 0.28f),
                        spotColor = colors.dockBackground.copy(alpha = 0.28f),
                    )
                    .clip(OpenCultTheme.shapes.dockPill)
                    .background(colors.dockBackground, OpenCultTheme.shapes.dockPill)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                val destinations = OpenCultDestination.entries
                val selectedIndex = destinations.indexOf(selectedDestination).coerceAtLeast(0)
                val slotWidth = maxWidth / destinations.size
                val pillWidth = min(slotWidth - 2.dp, 54.dp)
                val targetX = slotWidth * selectedIndex + (slotWidth - pillWidth) / 2f
                val animatedX by animateDpAsState(
                    targetValue = targetX,
                    animationSpec = OpenCultMotion.spatialDefault(reducedMotion),
                    label = "dock_active_pill_x",
                )
                val density = LocalDensity.current

                // Single GPU-accelerated sliding white active pill indicator
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            translationX = with(density) { animatedX.toPx() }
                        }
                        .size(width = pillWidth, height = 48.dp)
                        .clip(OpenCultTheme.shapes.pill)
                        .background(colors.dockActivePill, OpenCultTheme.shapes.pill),
                )

                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    destinations.forEach { destination ->
                        DockNavItem(
                            destination = destination,
                            selected = selectedDestination == destination,
                            reducedMotion = reducedMotion,
                            onClick = { onSelectDestination(destination) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            CobaltMeasureFab(onClick = onMeasureClick)
        }
    }
}

@Composable
private fun DockNavItem(
    destination: OpenCultDestination,
    selected: Boolean,
    reducedMotion: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OpenCultTheme.colors
    val iconColor by animateColorAsState(
        targetValue = if (selected) colors.dockActiveIcon else colors.dockInactiveIcon,
        animationSpec = OpenCultMotion.pressFeedback(reducedMotion),
        label = "dock_icon_${destination.route}",
    )

    Box(
        modifier = modifier
            .height(48.dp)
            .clip(OpenCultTheme.shapes.pill)
            .openCultMicroPress(
                pressedScale = OpenCultMotion.RowPressScale,
                role = Role.Tab,
                onClick = onClick,
            )
            .semantics { contentDescription = destination.label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = destination.icon,
            contentDescription = destination.label,
            tint = iconColor,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
fun CobaltMeasureFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OpenCultTheme.colors
    val spacing = OpenCultTheme.spacing
    Box(
        modifier = modifier
            .size(spacing.fabDiameter)
            .shadow(
                elevation = 12.dp,
                shape = CircleShape,
                ambientColor = colors.brandCobalt.copy(alpha = 0.36f),
                spotColor = colors.brandCobalt.copy(alpha = 0.36f),
            )
            .clip(CircleShape)
            .background(colors.brandCobalt, CircleShape)
            .openCultMicroPress(
                pressedScale = OpenCultMotion.FabPressScale,
                triggerHaptic = true,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics { contentDescription = "Start weigh-in" },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = "Start weigh-in",
            tint = colors.dockActiveIcon,
            modifier = Modifier.size(28.dp),
        )
    }
}
