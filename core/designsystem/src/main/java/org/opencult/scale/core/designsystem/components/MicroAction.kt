package org.opencult.scale.core.designsystem.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.opencult.scale.core.designsystem.theme.OpenCultMotion
import org.opencult.scale.core.designsystem.theme.OpenCultTheme

/**
 * 4-stage micro-action press container (`Rest` -> `Touchdown` -> `Tracking` -> `Payoff`)
 * using GPU-only [graphicsLayer] transforms and >= 48.dp touch target floor.
 */
@Composable
fun Modifier.openCultMicroPress(
    enabled: Boolean = true,
    pressedScale: Float = OpenCultMotion.CardPressScale,
    triggerHaptic: Boolean = false,
    role: Role? = null,
    onClick: () -> Unit,
): Modifier {
    val reducedMotion = OpenCultTheme.reducedMotion
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) pressedScale else 1f,
        animationSpec = OpenCultMotion.pressFeedback(reducedMotion),
        label = "micro_press_scale",
    )

    return this
        .defaultMinSize(
            minWidth = OpenCultTheme.spacing.minTouchTarget,
            minHeight = OpenCultTheme.spacing.minTouchTarget,
        )
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            enabled = enabled,
            interactionSource = interactionSource,
            indication = null,
            role = role,
            onClick = {
                if (triggerHaptic) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                onClick()
            },
        )
}

/** Bottom inset spacer ensuring scroll containers clear [DetachedObsidianDock]. */
@Composable
fun Modifier.dockScrollClearance(): Modifier =
    this
        .navigationBarsPadding()
        .padding(bottom = OpenCultTheme.spacing.dockScrollClearance)

/** Custom soft-squircle card obeying Daylight flat + 1.dp hairline contrast rules. */
@Composable
fun OpenCultBentoCard(
    modifier: Modifier = Modifier,
    containerColor: Color = OpenCultTheme.colors.cardSurface,
    borderColor: Color = OpenCultTheme.colors.borderSubtle,
    shape: Shape = OpenCultTheme.shapes.bentoSquircle,
    contentPadding: Dp = OpenCultTheme.spacing.cardPadding,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val baseModifier = modifier
        .clip(shape)
        .background(containerColor, shape)
        .border(BorderStroke(1.dp, borderColor), shape)
    val interactiveModifier = if (onClick != null) {
        baseModifier.openCultMicroPress(
            pressedScale = OpenCultMotion.CardPressScale,
            role = Role.Button,
            onClick = onClick,
        )
    } else {
        baseModifier
    }

    Column(
        modifier = interactiveModifier.padding(contentPadding),
        content = content,
    )
}

/** Custom DRD squircle input replacing stock Material OutlinedTextField. */
@Composable
fun OpenCultTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    val colors = OpenCultTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val borderColor = if (focused) colors.brandCobalt else colors.borderSubtle

    Column(modifier = modifier) {
        Text(
            text = label,
            style = OpenCultTheme.typography.microLabel,
            color = if (focused) colors.brandCobalt else colors.textSecondary,
        )
        Spacer(Modifier.height(6.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = OpenCultTheme.typography.body.copy(color = colors.textPrimary),
            cursorBrush = SolidColor(colors.brandCobalt),
            keyboardOptions = keyboardOptions,
            visualTransformation = visualTransformation,
            interactionSource = interactionSource,
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 50.dp)
                        .clip(OpenCultTheme.shapes.inputSquircle)
                        .background(colors.subtlePillSurface, OpenCultTheme.shapes.inputSquircle)
                        .border(1.5.dp, borderColor, OpenCultTheme.shapes.inputSquircle)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        Text(
                            text = placeholder,
                            style = OpenCultTheme.typography.body,
                            color = colors.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    innerTextField()
                }
            },
        )
    }
}

/** Brand cobalt squircle CTA (`DRD:T-07`, `>= 52.dp` min height). */
@Composable
fun OpenCultPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = OpenCultTheme.colors.brandCobalt,
    contentColor: Color = OpenCultTheme.colors.dockActiveIcon,
) {
    val colors = OpenCultTheme.colors
    val shape = OpenCultTheme.shapes.buttonSquircle
    val bg = if (enabled) containerColor else colors.borderStrong
    val fg = if (enabled) contentColor else colors.textMuted

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 52.dp)
            .clip(shape)
            .background(bg, shape)
            .openCultMicroPress(
                enabled = enabled,
                pressedScale = OpenCultMotion.CardPressScale,
                triggerHaptic = true,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 18.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = OpenCultTheme.typography.buttonLabel,
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Outlined secondary squircle action button (`>= 48.dp` single-line symmetric height). */
@Composable
fun OpenCultSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = OpenCultTheme.colors
    val shape = OpenCultTheme.shapes.buttonSquircle

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 50.dp)
            .clip(shape)
            .background(colors.elevatedSurface, shape)
            .border(1.dp, colors.borderStrong, shape)
            .openCultMicroPress(
                enabled = enabled,
                pressedScale = OpenCultMotion.RowPressScale,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 10.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = OpenCultTheme.typography.buttonLabel,
            color = if (enabled) colors.textPrimary else colors.textMuted,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Compact pill badge used for provenance labels, deltas, and metric states. */
@Composable
fun OpenCultStatusChip(
    label: String,
    containerColor: Color = OpenCultTheme.colors.cobaltSubtle,
    contentColor: Color = OpenCultTheme.colors.brandCobalt,
    modifier: Modifier = Modifier,
) {
    val shape: RoundedCornerShape = OpenCultTheme.shapes.pill
    Box(
        modifier = modifier
            .clip(shape)
            .background(containerColor, shape)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = OpenCultTheme.typography.microLabel,
            color = contentColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun OpenCultSectionHeader(
    title: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val colors = OpenCultTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(contentPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = OpenCultTheme.typography.sectionTitle,
                color = colors.textPrimary,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = OpenCultTheme.typography.body,
                    color = colors.textSecondary,
                )
            }
        }
        if (trailing != null) {
            trailing()
        }
    }
}
