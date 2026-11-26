package org.opencult.scale.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Shape tokens locked to DRD:T-19..T-21 and MICROENGINEERING §4. */
@Immutable
data class OpenCultShapes(
    val heroSquircle: RoundedCornerShape = RoundedCornerShape(28.dp),
    val bentoSquircle: RoundedCornerShape = RoundedCornerShape(24.dp),
    val sheetSquircle: RoundedCornerShape = RoundedCornerShape(28.dp),
    val dockPill: RoundedCornerShape = RoundedCornerShape(32.dp),
    val buttonSquircle: RoundedCornerShape = RoundedCornerShape(18.dp),
    val inputSquircle: RoundedCornerShape = RoundedCornerShape(16.dp),
    val chipSquircle: RoundedCornerShape = RoundedCornerShape(14.dp),
    val barColumn: RoundedCornerShape = RoundedCornerShape(16.dp),
    val sparkBar: RoundedCornerShape = RoundedCornerShape(4.dp),
    val pill: RoundedCornerShape = RoundedCornerShape(999.dp),
)

/** Spacing and sizing tokens locked to DRD:T-22..T-25, DRD:C-05, and MICROENGINEERING §3. */
@Immutable
data class OpenCultSpacing(
    val screenHorizontal: Dp = 20.dp,
    val bentoGap: Dp = 12.dp,
    val sectionGap: Dp = 20.dp,
    val cardPadding: Dp = 20.dp,
    val cardLargePadding: Dp = 24.dp,
    val dockHeight: Dp = 64.dp,
    val dockHorizontalInset: Dp = 20.dp,
    val dockBottomMargin: Dp = 16.dp,
    val fabDiameter: Dp = 64.dp,
    val minTouchTarget: Dp = 48.dp,
    val dockScrollClearance: Dp = 120.dp,
)
