package com.raisoft.grabi.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.raisoft.grabi.R

private val MontserratDisplay = FontFamily(Font(R.font.montserrat_extrabold))
private val MontserratBody = FontFamily(Font(R.font.montserrat_medium))

private val Base = Typography()

val GrabiTypography = Typography(
    displayLarge = Base.displayLarge.copy(fontFamily = MontserratDisplay),
    displayMedium = Base.displayMedium.copy(fontFamily = MontserratDisplay),
    displaySmall = Base.displaySmall.copy(fontFamily = MontserratDisplay),
    headlineLarge = Base.headlineLarge.copy(fontFamily = MontserratDisplay),
    headlineMedium = Base.headlineMedium.copy(fontFamily = MontserratDisplay),
    headlineSmall = Base.headlineSmall.copy(fontFamily = MontserratDisplay),
    titleLarge = Base.titleLarge.copy(fontFamily = MontserratBody),
    titleMedium = Base.titleMedium.copy(fontFamily = MontserratBody),
    titleSmall = Base.titleSmall.copy(fontFamily = MontserratBody),
    bodyLarge = Base.bodyLarge.copy(fontFamily = MontserratBody),
    bodyMedium = Base.bodyMedium.copy(fontFamily = MontserratBody),
    bodySmall = Base.bodySmall.copy(fontFamily = MontserratBody),
    labelLarge = Base.labelLarge.copy(fontFamily = MontserratBody),
    labelMedium = Base.labelMedium.copy(fontFamily = MontserratBody),
    labelSmall = Base.labelSmall.copy(fontFamily = MontserratBody),
)