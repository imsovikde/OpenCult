package org.opencult.scale.core.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
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
            Row(
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
                    .padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OpenCultDestination.entries.forEach { destination ->
                    DockNavItem(
                        destination = destination,
                        selected = selectedDestination == destination,
                        reducedMotion = reducedMotion,
                        onClick = { onSelectDestination(destination) },
                    )
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
) {
    val colors = OpenCultTheme.colors
    val iconColor by animateColorAsState(
        targetValue = if (selected) colors.dockActiveIcon else colors.dockInactiveIcon,
        animationSpec = OpenCultMotion.pressFeedback(reducedMotion),
        label = "dock_icon_${destination.route}",
    )
    val pillBg by animateColorAsState(
        targetValue = if (selected) colors.dockActivePill else colors.dockBackground.copy(alpha = 0f),
        animationSpec = OpenCultMotion.pressFeedback(reducedMotion),
        label = "dock_pill_${destination.route}",
    )

    Box(
        modifier = Modifier
            .size(width = 50.dp, height = 48.dp)
            .clip(OpenCultTheme.shapes.pill)
            .background(pillBg, OpenCultTheme.shapes.pill)
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
