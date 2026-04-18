package com.error404.neunest.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.error404.neunest.R

val inter = FontFamily(
    Font(R.font.regular, FontWeight.Normal),
    Font(R.font.medium, FontWeight.Medium),
)

val defaultTypography = Typography()

val Typography = Typography(
    displayLarge = defaultTypography.displayLarge.copy(fontFamily = inter),
    displayMedium = defaultTypography.displayMedium.copy(fontFamily = inter),
    displaySmall = defaultTypography.displaySmall.copy(fontFamily = inter),

    headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = inter),
    headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = inter),
    headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = inter),

    titleLarge = defaultTypography.titleLarge.copy(fontFamily = inter),
    titleMedium = defaultTypography.titleMedium.copy(fontFamily = inter),
    titleSmall = defaultTypography.titleSmall.copy(fontFamily = inter),

    bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = inter),
    bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = inter),
    bodySmall = defaultTypography.bodySmall.copy(fontFamily = inter),

    labelLarge = defaultTypography.labelLarge.copy(fontFamily = inter),
    labelMedium = defaultTypography.labelMedium.copy(fontFamily = inter),
    labelSmall = defaultTypography.labelSmall.copy(fontFamily = inter),
)