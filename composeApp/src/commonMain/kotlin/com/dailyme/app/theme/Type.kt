package com.dailyme.app.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dailyme.composeapp.generated.resources.Res
import dailyme.composeapp.generated.resources.inter_medium
import dailyme.composeapp.generated.resources.inter_regular
import dailyme.composeapp.generated.resources.montserrat_bold
import dailyme.composeapp.generated.resources.montserrat_semibold
import org.jetbrains.compose.resources.Font

/**
 * Typography built from DESIGN.md's scale: Montserrat for display/navigational
 * text, Inter for body/functional text and the editor surface.
 */
@Composable
fun cyanicTypography(): Typography {
    val montserrat = FontFamily(
        Font(Res.font.montserrat_semibold, FontWeight.SemiBold),
        Font(Res.font.montserrat_bold, FontWeight.Bold),
    )
    val inter = FontFamily(
        Font(Res.font.inter_regular, FontWeight.Normal),
        Font(Res.font.inter_medium, FontWeight.Medium),
    )

    val base = Typography()
    return base.copy(
        displayLarge = base.displayLarge.copy(fontFamily = montserrat),
        displayMedium = base.displayMedium.copy(fontFamily = montserrat),
        displaySmall = base.displaySmall.copy(fontFamily = montserrat),
        headlineLarge = base.headlineLarge.copy(
            fontFamily = montserrat,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 34.sp,
            letterSpacing = (-0.56).sp,
        ),
        headlineMedium = base.headlineMedium.copy(
            fontFamily = montserrat,
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp,
            lineHeight = 28.sp,
        ),
        headlineSmall = base.headlineSmall.copy(fontFamily = montserrat),
        titleLarge = base.titleLarge.copy(fontFamily = montserrat),
        titleMedium = base.titleMedium.copy(fontFamily = montserrat),
        titleSmall = base.titleSmall.copy(fontFamily = montserrat),
        // "code-sm": Inter medium at a slightly smaller scale, used for the editor surface.
        bodySmall = base.bodySmall.copy(
            fontFamily = inter,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            letterSpacing = 0.13.sp,
        ),
        bodyMedium = base.bodyMedium.copy(
            fontFamily = inter,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        ),
        bodyLarge = base.bodyLarge.copy(
            fontFamily = inter,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
        ),
        // "label-caps": uppercase Montserrat labels with wide letter-spacing.
        // Callers are responsible for uppercasing the label text itself.
        labelLarge = base.labelLarge.copy(
            fontFamily = montserrat,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.88.sp,
        ),
        labelMedium = base.labelMedium.copy(
            fontFamily = montserrat,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 0.96.sp,
        ),
        labelSmall = base.labelSmall.copy(
            fontFamily = montserrat,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 0.88.sp,
        ),
    )
}
