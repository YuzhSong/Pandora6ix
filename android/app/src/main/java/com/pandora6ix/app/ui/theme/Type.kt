package com.pandora6ix.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** 八级字体（规范第 3 节）：28/24/20/18/16/14/14/12sp，letterSpacing 统一 0 */
object PandoraTypography {
    private fun style(size: Int, weight: FontWeight, line: Int) = TextStyle(
        fontFamily = FontFamily.Default, fontSize = size.sp, fontWeight = weight,
        lineHeight = line.sp, letterSpacing = 0.sp
    )

    val display = style(28, FontWeight.Bold, 36)
    val titleLarge = style(24, FontWeight.SemiBold, 32)
    val titleMedium = style(20, FontWeight.SemiBold, 28)
    val titleSmall = style(18, FontWeight.Medium, 24)
    val bodyLarge = style(16, FontWeight.Normal, 24)
    val bodyMedium = style(14, FontWeight.Normal, 20)
    val labelLarge = style(14, FontWeight.Medium, 20)
    val caption = style(12, FontWeight.Normal, 16)

    /** 仅年概览日期与月任务条使用的日历例外 */
    val calendarMicro = style(10, FontWeight.Normal, 14)
}

/** Material3 每个槽位都引用八级之一，不新增字号 */
val PandoraMaterialTypography = Typography(
    displayLarge = PandoraTypography.display,
    displayMedium = PandoraTypography.display,
    displaySmall = PandoraTypography.display,
    headlineLarge = PandoraTypography.display,
    headlineMedium = PandoraTypography.display,
    headlineSmall = PandoraTypography.titleLarge,
    titleLarge = PandoraTypography.titleMedium,
    titleMedium = PandoraTypography.titleSmall,
    titleSmall = PandoraTypography.labelLarge,
    bodyLarge = PandoraTypography.bodyLarge,
    bodyMedium = PandoraTypography.bodyMedium,
    bodySmall = PandoraTypography.caption,
    labelLarge = PandoraTypography.labelLarge,
    labelMedium = PandoraTypography.caption,
    labelSmall = PandoraTypography.caption
)
