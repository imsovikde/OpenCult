package org.opencult.scale.core.designsystem.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.collections.immutable.ImmutableList
import org.opencult.scale.core.designsystem.theme.OpenCultMotion
import org.opencult.scale.core.designsystem.theme.OpenCultTheme

@Immutable
data class SparkBarEntry(
    val value: Float,
    val isSpike: Boolean = false,
)

/**
 * DRD:C-01 Hero Card 9-column vertical rounded capsule spark-bars (`#263238` normal, `#EF4444` spike).
 */
@Composable
fun HeroWeightSparkChart(
    entries: ImmutableList<SparkBarEntry>,
    modifier: Modifier = Modifier,
) {
    val colors = OpenCultTheme.colors
    val reducedMotion = OpenCultTheme.reducedMotion
    val animProgress = remember(entries) { Animatable(if (reducedMotion) 1f else 0.15f) }

    LaunchedEffect(entries, reducedMotion) {
        if (reducedMotion) {
            animProgress.snapTo(1f)
        } else {
            animProgress.animateTo(1f, animationSpec = OpenCultMotion.telemetryGlide(false))
        }
    }

    val bars = remember(entries) { entries.takeLast(9) }
    val minVal = remember(bars) { bars.minOfOrNull { it.value } ?: 0f }
    val maxVal = remember(bars) { bars.maxOfOrNull { it.value } ?: 1f }
    val range = remember(minVal, maxVal) { (maxVal - minVal).coerceAtLeast(0.5f) }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .semantics { contentDescription = "Recent weight spark-bars" },
    ) {
        if (bars.isEmpty()) return@Canvas
        val barMaxWidth = 8.dp.toPx()
        val gap = 6.dp.toPx()
        val count = bars.size
        val colWidth = minOf(barMaxWidth, ((size.width - gap * (count - 1)) / count).coerceAtLeast(4.dp.toPx()))
        val totalWidth = colWidth * count + gap * (count - 1)
        val startX = (size.width - totalWidth) / 2f
        val radius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        val progress = animProgress.value

        bars.forEachIndexed { idx, entry ->
            val normalized = 0.25f + 0.75f * ((entry.value - minVal) / range)
            val barHeight = (size.height * normalized * progress).coerceAtLeast(colWidth)
            val left = startX + idx * (colWidth + gap)
            val top = size.height - barHeight
            drawRoundRect(
                color = if (entry.isSpike) colors.coralAlert else colors.heroSparkNeutral,
                topLeft = Offset(left, top),
                size = Size(colWidth, barHeight),
                cornerRadius = radius,
            )
        }
    }
}

/**
 * DRD:C-02 Overview Bento wave sparkline (`bentoLavenderWave` cubic-Bezier + gradient fill).
 */
@Composable
fun OverviewWaveSparkline(
    values: ImmutableList<Float>,
    modifier: Modifier = Modifier,
) {
    val colors = OpenCultTheme.colors
    val reducedMotion = OpenCultTheme.reducedMotion
    val reveal = remember(values) { Animatable(if (reducedMotion) 1f else 0f) }

    LaunchedEffect(values, reducedMotion) {
        if (reducedMotion) {
            reveal.snapTo(1f)
        } else {
            reveal.animateTo(1f, animationSpec = OpenCultMotion.telemetryGlide(false))
        }
    }

    val curvePath = remember { Path() }
    val fillPath = remember { Path() }
    val segmentPath = remember { Path() }
    val measure = remember { PathMeasure() }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
            .semantics { contentDescription = "Body composition trend wave" },
    ) {
        if (values.size < 2) return@Canvas
        val min = values.minOrNull() ?: 0f
        val max = values.maxOrNull() ?: 1f
        val span = (max - min).coerceAtLeast(0.5f)
        val padY = 8.dp.toPx()
        val usableH = (size.height - padY * 2f).coerceAtLeast(4f)
        val stepX = size.width / (values.size - 1)

        curvePath.reset()
        fillPath.reset()
        segmentPath.reset()

        val points = List(values.size) { i ->
            val norm = (values[i] - min) / span
            Offset(
                x = i * stepX,
                y = padY + usableH * (1f - norm),
            )
        }

        curvePath.moveTo(points.first().x, points.first().y)
        fillPath.moveTo(points.first().x, size.height)
        fillPath.lineTo(points.first().x, points.first().y)

        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val cur = points[i]
            val midX = (prev.x + cur.x) * 0.5f
            curvePath.cubicTo(midX, prev.y, midX, cur.y, cur.x, cur.y)
            fillPath.cubicTo(midX, prev.y, midX, cur.y, cur.x, cur.y)
        }
        fillPath.lineTo(points.last().x, size.height)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    colors.bentoLavenderWave.copy(alpha = 0.36f),
                    colors.bentoLavenderWave.copy(alpha = 0.02f),
                ),
            ),
        )

        measure.setPath(curvePath, false)
        measure.getSegment(0f, measure.length * reveal.value, segmentPath, true)
        drawPath(
            path = segmentPath,
            color = colors.bentoLavenderWave,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

/**
 * DRD:C-03 Reports 180-degree 3-zone arc gauge (`#2185F7` Optimal, `#F4B428` Elevated, `#EF4444` High)
 * with exact 4-degree transparent gaps and polar-interpolated ringed circular thumb (INV-22 / SHAME-006).
 */
@Composable
fun BodyFatArcGauge(
    bodyFatPct: Float,
    minPct: Float = 8f,
    optimalUpperPct: Float = 16f,
    elevatedUpperPct: Float = 25f,
    maxPct: Float = 40f,
    hasData: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val colors = OpenCultTheme.colors
    val reducedMotion = OpenCultTheme.reducedMotion
    val fractionTarget = ((bodyFatPct - minPct) / (maxPct - minPct)).coerceIn(0f, 1f)
    val animatedFraction by animateFloatAsState(
        targetValue = fractionTarget,
        animationSpec = OpenCultMotion.telemetryGlide(reducedMotion),
        label = "arc_gauge_fraction",
    )

    val textMeasurer = rememberTextMeasurer()
    val labelStyle = OpenCultTheme.typography.microLabel.copy(color = colors.textMuted)
    val density = LocalDensity.current

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(176.dp)
            .semantics { contentDescription = "Body fat classification arc gauge at $bodyFatPct percent" },
    ) {
        val strokePx = with(density) { 18.dp.toPx() }
        val thumbOuterR = with(density) { 12.dp.toPx() }
        val thumbInnerR = with(density) { 6.dp.toPx() }
        val pad = strokePx + thumbOuterR + with(density) { 8.dp.toPx() }
        val diameter = minOf(size.width - pad * 2f, (size.height - pad) * 1.85f).coerceAtLeast(80f)
        val radius = diameter / 2f
        val cx = size.width / 2f
        val cy = size.height - with(density) { 26.dp.toPx() }
        val topLeft = Offset(cx - radius, cy - radius)
        val arcSize = Size(diameter, diameter)
        val clearGapDeg = 4f
        val capExtendDeg = ((strokePx / 2f) / radius) * (180f / PI.toFloat())
        val effectiveGapDeg = clearGapDeg + 2f * capExtendDeg

        val pOpt = ((optimalUpperPct - minPct) / (maxPct - minPct)).coerceIn(0.12f, 0.78f)
        val pElev = ((elevatedUpperPct - minPct) / (maxPct - minPct)).coerceIn(pOpt + 0.12f, 0.90f)
        val totalSweep = (180f - effectiveGapDeg * 2f).coerceAtLeast(30f)
        val sweep1 = totalSweep * pOpt
        val sweep2 = totalSweep * (pElev - pOpt)
        val sweep3 = (totalSweep - sweep1 - sweep2).coerceAtLeast(10f)

        val start1 = 180f
        val start2 = start1 + sweep1 + effectiveGapDeg
        val start3 = start2 + sweep2 + effectiveGapDeg

        val zoneAlpha = if (hasData) 1f else 0.24f
        val strokeStyle = Stroke(width = strokePx, cap = StrokeCap.Round)
        drawArc(
            color = colors.gaugeOptimalBlue.copy(alpha = zoneAlpha),
            startAngle = start1,
            sweepAngle = sweep1,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = strokeStyle,
        )
        drawArc(
            color = colors.gaugeElevatedAmber.copy(alpha = zoneAlpha),
            startAngle = start2,
            sweepAngle = sweep2,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = strokeStyle,
        )
        drawArc(
            color = colors.gaugeHighCoral.copy(alpha = zoneAlpha),
            startAngle = start3,
            sweepAngle = sweep3,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = strokeStyle,
        )

        if (hasData) {
            // Polar thumb glide along exact arc circumference (never chord-cutting)
            val thumbAngleDeg = 180f + 180f * animatedFraction
            val thumbRad = thumbAngleDeg * PI / 180.0
            val thumbCenter = Offset(
                x = cx + radius * cos(thumbRad).toFloat(),
                y = cy + radius * sin(thumbRad).toFloat(),
            )
            val thumbAccent = when {
                bodyFatPct <= optimalUpperPct -> colors.gaugeOptimalBlue
                bodyFatPct <= elevatedUpperPct -> colors.gaugeElevatedAmber
                else -> colors.gaugeHighCoral
            }
            drawCircle(
                color = colors.canvas,
                radius = thumbOuterR + 2.dp.toPx(),
                center = thumbCenter,
            )
            drawCircle(
                color = thumbAccent,
                radius = thumbInnerR,
                center = thumbCenter,
            )
        }

        val lowText = textMeasurer.measure("${optimalUpperPct.toInt()}%", labelStyle)
        val highText = textMeasurer.measure("${elevatedUpperPct.toInt()}%", labelStyle)
        drawText(
            textLayoutResult = lowText,
            topLeft = Offset(cx - radius - lowText.size.width * 0.25f, cy + 6.dp.toPx()),
        )
        drawText(
            textLayoutResult = highText,
            topLeft = Offset(cx + radius - highText.size.width * 0.75f, cy + 6.dp.toPx()),
        )
    }
}

@Immutable
data class StippledBarEntry(
    val id: String,
    val label: String,
    val value: Float,
    val badgeText: String,
)

/**
 * DRD:C-04 Staggered Stippled Dot-Matrix Bar Chart (`16.dp` radius, active solid + floating pill,
 * unselected clipped to odd-row staggered `6.dp` pitch / `3.dp` horizontal offset `2.2.dp` dot radius).
 */
@Composable
fun StippledBarChart(
    entries: ImmutableList<StippledBarEntry>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OpenCultTheme.colors
    val reducedMotion = OpenCultTheme.reducedMotion
    val progress = remember(entries) { Animatable(if (reducedMotion) 1f else 0.2f) }
    val textMeasurer = rememberTextMeasurer()

    LaunchedEffect(entries, reducedMotion) {
        if (reducedMotion) {
            progress.snapTo(1f)
        } else {
            progress.animateTo(1f, animationSpec = OpenCultMotion.telemetryGlide(false))
        }
    }

    val safeSelected = if (entries.isEmpty()) 0 else selectedIndex.coerceIn(0, entries.lastIndex)
    val badgeStyle: TextStyle = OpenCultTheme.typography.microLabel.copy(color = colors.dockActiveIcon)
    val labelStyle: TextStyle = OpenCultTheme.typography.microLabel.copy(color = colors.textMuted)
    val activeLabelStyle: TextStyle = OpenCultTheme.typography.microLabel.copy(color = colors.textPrimary)

    Box(modifier = modifier.fillMaxWidth().height(210.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(entries) {
                    detectTapGestures { offset ->
                        if (entries.isEmpty()) return@detectTapGestures
                        val slot = size.width.toFloat() / entries.size
                        val tapped = (offset.x / slot).toInt().coerceIn(0, entries.lastIndex)
                        onSelectIndex(tapped)
                    }
                }
                .semantics { contentDescription = "Trend stippled bar chart" },
        ) {
            if (entries.isEmpty()) return@Canvas
            val minV = (entries.minOf { it.value } * 0.85f).coerceAtLeast(0f)
            val maxV = entries.maxOf { it.value }.coerceAtLeast(minV + 1f)
            val span = (maxV - minV).coerceAtLeast(1f)
            val pillSpaceTop = 36.dp.toPx()
            val labelSpaceBottom = 26.dp.toPx()
            val chartBottom = size.height - labelSpaceBottom
            val maxBarH = (chartBottom - pillSpaceTop).coerceAtLeast(24.dp.toPx())
            val count = entries.size
            val gap = 10.dp.toPx()
            val barW = (((size.width - gap * (count - 1)) / count)).coerceIn(22.dp.toPx(), 48.dp.toPx())
            val totalW = barW * count + gap * (count - 1)
            val startX = (size.width - totalW) / 2f
            val cornerR = CornerRadius(16.dp.toPx(), 16.dp.toPx())
            val clipPathObj = Path()

            val pitch = 6.dp.toPx()
            val oddShift = 3.dp.toPx()
            val dotRadius = 2.2.dp.toPx()

            entries.forEachIndexed { idx, entry ->
                val norm = 0.28f + 0.72f * ((entry.value - minV) / span)
                val barH = (maxBarH * norm * progress.value).coerceAtLeast(24.dp.toPx())
                val left = startX + idx * (barW + gap)
                val top = chartBottom - barH
                val barRect = Rect(left, top, left + barW, chartBottom)

                if (idx == safeSelected) {
                    drawRoundRect(
                        color = colors.textPrimary,
                        topLeft = Offset(left, top),
                        size = Size(barW, barH),
                        cornerRadius = cornerR,
                    )
                    drawCircle(
                        color = colors.canvas,
                        radius = 3.5.dp.toPx(),
                        center = Offset(left + barW / 2f, top + 10.dp.toPx()),
                    )
                    val measuredBadge = textMeasurer.measure(entry.badgeText, badgeStyle)
                    val pillW = measuredBadge.size.width + 16.dp.toPx()
                    val pillH = measuredBadge.size.height + 8.dp.toPx()
                    val pillLeft = (left + barW / 2f - pillW / 2f).coerceIn(0f, size.width - pillW)
                    val pillTop = (top - pillH - 6.dp.toPx()).coerceAtLeast(0f)
                    drawRoundRect(
                        color = colors.dockBackground,
                        topLeft = Offset(pillLeft, pillTop),
                        size = Size(pillW, pillH),
                        cornerRadius = CornerRadius(pillH / 2f, pillH / 2f),
                    )
                    drawText(
                        textLayoutResult = measuredBadge,
                        topLeft = Offset(pillLeft + 8.dp.toPx(), pillTop + 4.dp.toPx()),
                    )
                } else {
                    drawRoundRect(
                        color = colors.cardSurface,
                        topLeft = Offset(left, top),
                        size = Size(barW, barH),
                        cornerRadius = cornerR,
                    )
                    clipPathObj.reset()
                    clipPathObj.addRoundRect(RoundRect(barRect, cornerR))
                    clipPath(clipPathObj) {
                        var rowIdx = 0
                        var y = top + dotRadius + 2.dp.toPx()
                        while (y <= chartBottom - dotRadius) {
                            val rowShift = if (rowIdx % 2 == 1) oddShift else 0f
                            var x = left + dotRadius + rowShift
                            while (x <= left + barW - dotRadius) {
                                drawCircle(
                                    color = colors.borderStrong.copy(alpha = 0.85f),
                                    radius = dotRadius,
                                    center = Offset(x, y),
                                )
                                x += pitch
                            }
                            y += pitch
                            rowIdx++
                        }
                    }
                }

                val lblLayout = textMeasurer.measure(
                    text = entry.label,
                    style = if (idx == safeSelected) activeLabelStyle else labelStyle,
                )
                drawText(
                    textLayoutResult = lblLayout,
                    topLeft = Offset(
                        x = (left + barW / 2f - lblLayout.size.width / 2f).coerceIn(0f, size.width - lblLayout.size.width),
                        y = chartBottom + 6.dp.toPx(),
                    ),
                )
            }
        }
    }
}

enum class AnatomicalSegment(val displayName: String) {
    LEFT_ARM("Left Arm"),
    RIGHT_ARM("Right Arm"),
    TRUNK("Trunk"),
    LEFT_LEG("Left Leg"),
    RIGHT_LEG("Right Leg"),
}

enum class SegmentTier {
    OPTIMAL,
    BALANCED,
    ELEVATED,
    LOCKED,
}

/**
 * DRD:C-06 8-Electrode 5-Segment Anatomical Body Map with interactive tap zones (>= 48.dp hit areas)
 * and honest gating when 4-electrode or Feet-Only hardware cannot measure limb segments.
 */
@Composable
fun AnatomicalBodyMapCanvas(
    selectedSegment: AnatomicalSegment,
    segmentTiers: Map<AnatomicalSegment, SegmentTier>,
    supports8Electrode: Boolean,
    onSelectSegment: (AnatomicalSegment) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OpenCultTheme.colors
    val hitboxes = remember { mutableMapOf<AnatomicalSegment, Rect>() }

    fun colorForTier(tier: SegmentTier): Color = when (tier) {
        SegmentTier.OPTIMAL -> colors.emerald
        SegmentTier.BALANCED -> colors.brandCobalt
        SegmentTier.ELEVATED -> colors.goldAmber
        SegmentTier.LOCKED -> colors.borderStrong
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(272.dp)
            .pointerInput(supports8Electrode) {
                detectTapGestures { pos ->
                    val hit = hitboxes.entries.firstOrNull { (_, rect) -> rect.contains(pos) }?.key
                    if (hit != null) {
                        onSelectSegment(hit)
                    }
                }
            }
            .semantics { contentDescription = "Five-segment anatomical body composition map" },
    ) {
        val cx = size.width / 2f
        val topY = 12.dp.toPx()

        // Head cap (decorative reference landmark)
        drawCircle(
            color = colors.borderStrong.copy(alpha = 0.6f),
            radius = 20.dp.toPx(),
            center = Offset(cx, topY + 20.dp.toPx()),
        )

        val trunkRect = Rect(cx - 34.dp.toPx(), topY + 52.dp.toPx(), cx + 34.dp.toPx(), topY + 148.dp.toPx())
        val rightArmRect = Rect(cx - 88.dp.toPx(), topY + 54.dp.toPx(), cx - 40.dp.toPx(), topY + 148.dp.toPx())
        val leftArmRect = Rect(cx + 40.dp.toPx(), topY + 54.dp.toPx(), cx + 88.dp.toPx(), topY + 148.dp.toPx())
        val rightLegRect = Rect(cx - 44.dp.toPx(), topY + 156.dp.toPx(), cx - 4.dp.toPx(), topY + 254.dp.toPx())
        val leftLegRect = Rect(cx + 4.dp.toPx(), topY + 156.dp.toPx(), cx + 44.dp.toPx(), topY + 254.dp.toPx())

        val segments = listOf(
            AnatomicalSegment.RIGHT_ARM to rightArmRect,
            AnatomicalSegment.LEFT_ARM to leftArmRect,
            AnatomicalSegment.TRUNK to trunkRect,
            AnatomicalSegment.RIGHT_LEG to rightLegRect,
            AnatomicalSegment.LEFT_LEG to leftLegRect,
        )

        val minHitExpand = 8.dp.toPx()
        segments.forEach { (seg, rect) ->
            hitboxes[seg] = rect.inflate(minHitExpand)
            val tier = if (!supports8Electrode) {
                SegmentTier.LOCKED
            } else {
                segmentTiers[seg] ?: SegmentTier.BALANCED
            }
            val fill = colorForTier(tier).copy(alpha = if (seg == selectedSegment) 0.92f else 0.48f)
            val radius = CornerRadius(14.dp.toPx(), 14.dp.toPx())
            drawRoundRect(
                color = fill,
                topLeft = rect.topLeft,
                size = rect.size,
                cornerRadius = radius,
            )
            if (seg == selectedSegment) {
                drawRoundRect(
                    color = colors.brandCobalt,
                    topLeft = rect.topLeft,
                    size = rect.size,
                    cornerRadius = radius,
                    style = Stroke(width = 3.dp.toPx()),
                )
            }
        }
    }
}

@Immutable
data class MetricTrendPoint(
    val label: String,
    val value: Float,
    val badgeText: String,
)

/**
 * Clickable multi-point Canvas trend explorer with bezier interpolation, soft gradient fill,
 * interactive point selection, and floating value indicator pill.
 */
@Composable
fun MetricTrendLineChart(
    points: ImmutableList<MetricTrendPoint>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    modifier: Modifier = Modifier,
    lineColor: Color = OpenCultTheme.colors.brandCobalt,
) {
    val colors = OpenCultTheme.colors
    val reducedMotion = OpenCultTheme.reducedMotion
    val reveal = remember(points) { Animatable(if (reducedMotion) 1f else 0f) }
    val textMeasurer = rememberTextMeasurer()

    LaunchedEffect(points, reducedMotion) {
        if (reducedMotion) reveal.snapTo(1f)
        else reveal.animateTo(1f, animationSpec = OpenCultMotion.telemetryGlide(false))
    }

    val safeSelected = if (points.isEmpty()) 0 else selectedIndex.coerceIn(0, points.lastIndex)
    val curvePath = remember { Path() }
    val fillPath = remember { Path() }
    val segPath = remember { Path() }
    val pathMeasure = remember { PathMeasure() }
    val badgeStyle = OpenCultTheme.typography.microLabel.copy(color = colors.dockActiveIcon)
    val labelStyle = OpenCultTheme.typography.microLabel.copy(color = colors.textMuted)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .pointerInput(points) {
                detectTapGestures { pos ->
                    if (points.size < 2) return@detectTapGestures
                    val padX = 16.dp.toPx()
                    val usableW = (size.width - padX * 2f).coerceAtLeast(1f)
                    val idx = (((pos.x - padX) / usableW) * (points.size - 1))
                        .let { kotlin.math.round(it.toDouble()).toInt() }
                        .coerceIn(0, points.lastIndex)
                    onSelectIndex(idx)
                }
            }
            .semantics { contentDescription = "Interactive metric trend explorer" },
    ) {
        if (points.isEmpty()) return@Canvas
        val minV = points.minOf { it.value }
        val maxV = points.maxOf { it.value }
        val span = (maxV - minV).coerceAtLeast(0.5f)
        val padTop = 34.dp.toPx()
        val padBottom = 26.dp.toPx()
        val padX = 16.dp.toPx()
        val usableH = (size.height - padTop - padBottom).coerceAtLeast(24f)
        val usableW = (size.width - padX * 2f).coerceAtLeast(1f)

        // Subtle baseline reference lines
        listOf(0f, 0.5f, 1f).forEach { frac ->
            val y = padTop + usableH * frac
            drawLine(
                color = colors.borderSubtle,
                start = Offset(padX, y),
                end = Offset(size.width - padX, y),
                strokeWidth = 1.dp.toPx(),
            )
        }

        val coords = if (points.size == 1) {
            listOf(Offset(size.width / 2f, padTop + usableH * 0.5f))
        } else {
            val stepX = usableW / (points.size - 1)
            points.mapIndexed { i, p ->
                val norm = (p.value - minV) / span
                Offset(padX + i * stepX, padTop + usableH * (1f - norm))
            }
        }

        if (coords.size >= 2) {
            curvePath.reset()
            fillPath.reset()
            segPath.reset()
            curvePath.moveTo(coords.first().x, coords.first().y)
            fillPath.moveTo(coords.first().x, padTop + usableH)
            fillPath.lineTo(coords.first().x, coords.first().y)
            for (i in 1 until coords.size) {
                val p0 = coords[i - 1]
                val p1 = coords[i]
                val midX = (p0.x + p1.x) * 0.5f
                curvePath.cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                fillPath.cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
            }
            fillPath.lineTo(coords.last().x, padTop + usableH)
            fillPath.close()

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(lineColor.copy(alpha = 0.28f), lineColor.copy(alpha = 0.01f)),
                ),
            )
            pathMeasure.setPath(curvePath, false)
            pathMeasure.getSegment(0f, pathMeasure.length * reveal.value, segPath, true)
            drawPath(
                path = segPath,
                color = lineColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }

        coords.forEachIndexed { idx, pt ->
            val isSel = idx == safeSelected
            if (isSel) {
                drawCircle(color = colors.canvas, radius = 8.dp.toPx(), center = pt)
                drawCircle(color = lineColor, radius = 5.5.dp.toPx(), center = pt)

                val measured = textMeasurer.measure(points[idx].badgeText, badgeStyle)
                val pillW = measured.size.width + 14.dp.toPx()
                val pillH = measured.size.height + 6.dp.toPx()
                val pLeft = (pt.x - pillW / 2f).coerceIn(0f, size.width - pillW)
                val pTop = (pt.y - pillH - 10.dp.toPx()).coerceAtLeast(0f)
                drawRoundRect(
                    color = colors.dockBackground,
                    topLeft = Offset(pLeft, pTop),
                    size = Size(pillW, pillH),
                    cornerRadius = CornerRadius(pillH / 2f, pillH / 2f),
                )
                drawText(
                    textLayoutResult = measured,
                    topLeft = Offset(pLeft + 7.dp.toPx(), pTop + 3.dp.toPx()),
                )
            } else {
                drawCircle(color = lineColor.copy(alpha = 0.55f), radius = 3.dp.toPx(), center = pt)
            }

            if (idx == 0 || idx == coords.lastIndex || idx == safeSelected) {
                val lbl = textMeasurer.measure(points[idx].label, labelStyle)
                drawText(
                    textLayoutResult = lbl,
                    topLeft = Offset(
                        x = (pt.x - lbl.size.width / 2f).coerceIn(0f, size.width - lbl.size.width),
                        y = padTop + usableH + 6.dp.toPx(),
                    ),
                )
            }
        }
    }
}
