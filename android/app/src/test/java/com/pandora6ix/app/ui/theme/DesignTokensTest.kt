package com.pandora6ix.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DesignTokensTest {
    private val themes = appThemes.associate { it.key to it.colors }

    @Test
    fun originalThemesKeepTheirKeysAndCartoonThemesFollow() {
        assertEquals(listOf("warm", "mint", "sky", "sakura", "lilac", "toon_cream", "toon_breeze", "toon_farm", "toon_spring",
            "toon_winter", "toon_study", "toon_fairy"), appThemes.map { it.key })
        assertEquals(listOf("暖米", "薄荷", "晴空", "樱花", "紫藤", "奶油单车", "凉风习习", "农场乐园", "春日郊游",
            "冬日乐园", "专心致志", "童话公主"), appThemes.map { it.label })
        // 原五套没有卡通素材，渲染路径保持不变
        assertTrue(appThemes.take(5).all { it.art == null && it.colors.cardBorder == it.colors.border })
    }

    @Test
    fun cartoonThemesProvideCompleteArt() {
        appThemes.drop(5).forEach { theme ->
            val art = requireNotNull(theme.art) { theme.key }
            assertEquals("${theme.key} 导航图标数量", 5, art.navIcons.size)
            assertEquals("${theme.key} 导航图标不重复", 5, art.navIcons.toSet().size)
            assertTrue("${theme.key} 卡片描边对比度", contrastRatio(theme.colors.cardBorder, theme.colors.surface) >= 4.5)
            // 导航文字落在 navBand 上：自定义 navLabel 或默认的次要文字色都要可读
            val label = art.navLabel ?: theme.colors.textSecondary
            assertTrue("${theme.key} 导航文字对比度", contrastRatio(label, art.navBand) >= 4.5)
        }
    }

    @Test
    fun whiteTextOnPrimaryMeetsNormalTextContrast() {
        themes.forEach { (key, c) ->
            val ratio = contrastRatio(c.textOnPrimary, c.primary)
            assertTrue("$key primary 白字对比度 $ratio", ratio >= 4.5)
            val pressed = contrastRatio(c.textOnPrimary, c.primaryPressed)
            assertTrue("$key pressed 白字对比度 $pressed", pressed >= 4.5)
        }
    }

    @Test
    fun readableTextMeetsContrastOnEveryBackgroundLayer() {
        themes.forEach { (key, c) ->
            listOf(c.appBackground, c.surface, c.elevatedSurface, c.primaryContainer).forEach { bg ->
                assertTrue("$key textPrimary", contrastRatio(c.textPrimary, bg) >= 4.5)
                assertTrue("$key textSecondary", contrastRatio(c.textSecondary, bg) >= 4.5)
            }
            listOf(c.appBackground, c.surface, c.elevatedSurface).forEach { bg ->
                assertTrue("$key textTertiary ${contrastRatio(c.textTertiary, bg)}", contrastRatio(c.textTertiary, bg) >= 4.5)
                assertTrue("$key primary as link", contrastRatio(c.primary, bg) >= 4.5)
            }
        }
    }

    @Test
    fun selectedContainerUsesTextPrimary() {
        // 选中淡底上的文字统一用 TextPrimary；Primary 只作图标/指示（非文字 ≥3:1）
        themes.forEach { (key, c) ->
            assertTrue("$key textPrimary on primaryContainer", contrastRatio(c.textPrimary, c.primaryContainer) >= 4.5)
            assertTrue("$key primary icon on primaryContainer", contrastRatio(c.primary, c.primaryContainer) >= 3.0)
        }
    }

    @Test
    fun statusTextIsReadableOnItsContainer() {
        val c = WarmColors
        StatusTone.entries.forEach { tone ->
            val colors = c.statusColors(tone)
            assertTrue("$tone ${contrastRatio(colors.content, colors.container)}", contrastRatio(colors.content, colors.container) >= 4.5)
        }
    }

    @Test
    fun inputBorderIsDistinguishableFromSurfaces() {
        themes.forEach { (key, c) ->
            assertTrue("$key inputBorder", contrastRatio(c.inputBorder, c.surface) >= 3.0)
        }
    }

    @Test
    fun elevatedSurfaceIsWhiteInEveryTheme() {
        themes.values.forEach { assertEquals(WarmColors.elevatedSurface, it.elevatedSurface) }
    }

    @Test
    fun materialSchemeIsGeneratedFromSameTheme() {
        appThemes.forEach { theme ->
            val scheme = theme.colors.toMaterialColorScheme()
            assertEquals(theme.colors.primary, scheme.primary)
            assertEquals(theme.colors.appBackground, scheme.background)
            assertEquals(theme.colors.elevatedSurface, scheme.surface)
            assertEquals(theme.colors.elevatedSurface, scheme.surfaceContainerHigh)
            assertEquals(theme.colors.border, scheme.outlineVariant)
        }
    }

    @Test
    fun statusMappingFollowsDesignSystem() {
        assertEquals(StatusTone.WARNING, reviewStatusTone("PENDING"))
        assertEquals(StatusTone.SUCCESS, reviewStatusTone("APPROVED"))
        assertEquals(StatusTone.ERROR, reviewStatusTone("REJECTED"))
        assertEquals(StatusTone.NEUTRAL, reviewStatusTone("SOMETHING_NEW"))
        assertEquals(StatusTone.NEUTRAL, executionStatusTone("ACTIVE", "NOT_STARTED"))
        assertEquals(StatusTone.INFO, executionStatusTone("ACTIVE", "IN_PROGRESS"))
        assertEquals(StatusTone.WARNING, executionStatusTone("AWAITING_ACCEPTANCE"))
        assertEquals(StatusTone.SUCCESS, executionStatusTone("COMPLETED"))
        assertEquals(StatusTone.NEUTRAL, executionStatusTone(null))
    }

    @Test
    fun priorityMappingFollowsDesignSystem() {
        assertEquals(listOf("很低", "一般", "普通", "重要", "紧急"), (1..5).map(::priorityLabel))
        assertEquals(StatusTone.NEUTRAL, priorityTone(3))
        assertEquals(StatusTone.WARNING, priorityTone(4))
        assertEquals(StatusTone.ERROR, priorityTone(5))
        assertEquals(StatusTone.ERROR, priorityTone(9))
    }

    @Test
    fun gridColumnsMatchDocumentedWidths() {
        assertEquals(76f, PandoraGrid.columnWidth(androidx.compose.ui.unit.Dp(360f)).value, 0.01f)
        assertEquals(84.25f, PandoraGrid.columnWidth(androidx.compose.ui.unit.Dp(393f)).value, 0.01f)
    }

    @Test
    fun typographyUsesEightDocumentedLevels() {
        val sizes = listOf(PandoraTypography.display, PandoraTypography.titleLarge, PandoraTypography.titleMedium,
            PandoraTypography.titleSmall, PandoraTypography.bodyLarge, PandoraTypography.bodyMedium,
            PandoraTypography.labelLarge, PandoraTypography.caption).map { it.fontSize.value.toInt() }
        assertEquals(listOf(28, 24, 20, 18, 16, 14, 14, 12), sizes)
    }
}
