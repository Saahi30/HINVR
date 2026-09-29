@file:OptIn(ExperimentalTextApi::class)

package com.hinvr.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.hinvr.app.R

val Fraunces = FontFamily(
    Font(R.font.fraunces_medium, FontWeight.Medium),
    Font(R.font.fraunces_semibold, FontWeight.SemiBold),
)

// Fraunces, tuned warm and steady: soft terminals, no quirky wonk, heavy enough
// to read over the manuscript. Closer to Canela than a cold fashion Didone.
private val WarmDisplay = FontVariation.Settings(
    FontVariation.weight(680),
    FontVariation.Setting("SOFT", 50f),
    FontVariation.Setting("WONK", 0f),
    FontVariation.Setting("opsz", 72f),
)

private val EnglishDisplay = FontFamily(
    Font(R.font.fraunces_roman, FontWeight.Bold, FontStyle.Normal, variationSettings = WarmDisplay),
    Font(R.font.fraunces_italic, FontWeight.Bold, FontStyle.Italic, variationSettings = WarmDisplay),
)

var HinvrDisplay: FontFamily = EnglishDisplay
    private set

val Figtree = FontFamily(
    Font(R.font.figtree_regular, FontWeight.Normal),
    Font(R.font.figtree_medium, FontWeight.Medium),
    Font(R.font.figtree_semibold, FontWeight.SemiBold),
)

private fun notoFace(resId: Int, weight: FontWeight, axis: Int) = Font(
    resId,
    weight,
    FontStyle.Normal,
    variationSettings = FontVariation.Settings(
        FontVariation.weight(axis),
        FontVariation.Setting("wdth", 100f),
    ),
)

private val HindiDisplay = FontFamily(
    notoFace(R.font.noto_serif_devanagari, FontWeight.Bold, 680),
)

private val HindiText = FontFamily(
    notoFace(R.font.noto_sans_devanagari, FontWeight.Normal, 400),
    notoFace(R.font.noto_sans_devanagari, FontWeight.Medium, 500),
    notoFace(R.font.noto_sans_devanagari, FontWeight.SemiBold, 600),
    notoFace(R.font.noto_sans_devanagari, FontWeight.Bold, 700),
)

private fun buildTypography(display: FontFamily, text: FontFamily, tracking: Boolean, italicDisplay: Boolean) = Typography(
    displayLarge = TextStyle(
        fontFamily = display,
        fontWeight = FontWeight.Bold,
        fontStyle = if (italicDisplay) FontStyle.Italic else FontStyle.Normal,
        fontSize = 52.sp,
        letterSpacing = if (tracking) (-0.8).sp else 0.sp,
        lineHeight = 56.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = display,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        letterSpacing = 0.sp,
        lineHeight = 42.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = display,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = display,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = text,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        letterSpacing = if (tracking) 0.1.sp else 0.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = text,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = text,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = text,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        letterSpacing = if (tracking) 0.2.sp else 0.sp,
        lineHeight = 20.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = text,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        letterSpacing = if (tracking) 1.2.sp else 0.sp,
        lineHeight = 16.sp,
    ),
)

private val EnglishTypography = buildTypography(EnglishDisplay, Figtree, tracking = true, italicDisplay = true)
private val HindiTypography = buildTypography(HindiDisplay, HindiText, tracking = false, italicDisplay = false)

var HinvrTypography: Typography = EnglishTypography
    private set

private val EnglishCardTitle = TextStyle(
    fontFamily = Figtree,
    fontWeight = FontWeight.SemiBold,
    fontSize = 18.sp,
    lineHeight = 22.sp,
)

private val EnglishBenefit = TextStyle(
    fontFamily = Figtree,
    fontWeight = FontWeight.SemiBold,
    fontSize = 13.sp,
    lineHeight = 16.sp,
)

var CardTitleOnPhoto: TextStyle = EnglishCardTitle
    private set

var BenefitLine: TextStyle = EnglishBenefit
    private set

fun applyHinvrType(hindi: Boolean) {
    if (hindi) {
        HinvrDisplay = HindiDisplay
        HinvrTypography = HindiTypography
        CardTitleOnPhoto = EnglishCardTitle.copy(fontFamily = HindiText)
        BenefitLine = EnglishBenefit.copy(fontFamily = HindiText)
    } else {
        HinvrDisplay = EnglishDisplay
        HinvrTypography = EnglishTypography
        CardTitleOnPhoto = EnglishCardTitle
        BenefitLine = EnglishBenefit
    }
}
