package org.opencult.scale.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.opencult.scale.core.designsystem.R

val PlusJakartaSansFamily: FontFamily = FontFamily(
    Font(resId = R.font.plus_jakarta_sans_regular, weight = FontWeight.Normal),
    Font(resId = R.font.plus_jakarta_sans_medium, weight = FontWeight.Medium),
    Font(resId = R.font.plus_jakarta_sans_semibold, weight = FontWeight.SemiBold),
    Font(resId = R.font.plus_jakarta_sans_bold, weight = FontWeight.Bold),
    Font(resId = R.font.plus_jakarta_sans_extrabold, weight = FontWeight.ExtraBold),
)

val JetBrainsMonoFamily: FontFamily = FontFamily(
    Font(resId = R.font.jetbrains_mono_regular, weight = FontWeight.Normal),
    Font(resId = R.font.jetbrains_mono_medium, weight = FontWeight.Medium),
    Font(resId = R.font.jetbrains_mono_bold, weight = FontWeight.SemiBold),
    Font(resId = R.font.jetbrains_mono_bold, weight = FontWeight.Bold),
)

/**
 * Typography scale locked to DRD:T-13..T-18 using static multi-weight font assets.
 * All numeric styles enforce tabular lining numerals via `fontFeatureSettings = "tnum"`.
 */
@Immutable
data class OpenCultTypography(
    val heroMetric: TextStyle,
    val bentoStat: TextStyle,
    val pageTitle: TextStyle,
    val sectionTitle: TextStyle,
    val body: TextStyle,
    val microLabel: TextStyle,
    val monoHex: TextStyle,
    val buttonLabel: TextStyle,
    val numericInline: TextStyle,
)

val DefaultOpenCultTypography = OpenCultTypography(
    heroMetric = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 36.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.72).sp,
        fontFeatureSettings = "tnum",
    ),
    bentoStat = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.39).sp,
        fontFeatureSettings = "tnum",
    ),
    pageTitle = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 29.sp,
        letterSpacing = (-0.48).sp,
    ),
    sectionTitle = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.18).sp,
    ),
    body = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    ),
    microLabel = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.44.sp,
    ),
    monoHex = TextStyle(
        fontFamily = JetBrainsMonoFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.sp,
        fontFeatureSettings = "tnum",
    ),
    buttonLabel = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    ),
    numericInline = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
        fontFeatureSettings = "tnum",
    ),
)
