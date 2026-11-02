package com.pandora6ix.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/** 任务重要程度的统一视觉元数据：文字颜色与文案（集中映射见 StatusColors.kt） */
@Composable
@ReadOnlyComposable
fun priorityMeta(level: Int): Pair<Color, String> =
    PandoraTheme.colors.statusColors(priorityTone(level)).content to priorityLabel(level)

/** 应用主题：key/名称/按账号保存方式不变，颜色统一由 [PandoraColorTokens] 提供 */
data class AppTheme(val key: String, val label: String, val colors: PandoraColorTokens)

val appThemes = listOf(
    AppTheme("warm", "暖米", WarmColors),
    AppTheme("mint", "薄荷", MintColors),
    AppTheme("sky", "晴空", SkyColors),
    AppTheme("sakura", "樱花", SakuraColors),
    AppTheme("lilac", "紫藤", LilacColors)
)

fun appThemeFor(key: String?): AppTheme = appThemes.firstOrNull { it.key == key } ?: appThemes.first()

val LocalAppTheme = staticCompositionLocalOf { appThemes.first() }

/** 统一取 Token 的入口 */
object PandoraTheme {
    val colors: PandoraColorTokens
        @Composable @ReadOnlyComposable get() = LocalPandoraColors.current
    val typography: PandoraTypography get() = PandoraTypography
    val spacing: PandoraSpacing get() = PandoraSpacing
    val shapes: PandoraShapes get() = PandoraShapes
}

/** 按规范第 10 节由同一套 Token 生成 Material3 配色 */
fun PandoraColorTokens.toMaterialColorScheme(): ColorScheme = lightColorScheme(
    primary = primary,
    onPrimary = textOnPrimary,
    primaryContainer = primaryContainer,
    onPrimaryContainer = textPrimary,
    secondary = secondary,
    onSecondary = textOnPrimary,
    secondaryContainer = primaryContainer,
    onSecondaryContainer = textPrimary,
    tertiary = info,
    onTertiary = textOnPrimary,
    background = appBackground,
    onBackground = textPrimary,
    surface = elevatedSurface,
    onSurface = textPrimary,
    surfaceVariant = neutralContainer,
    onSurfaceVariant = textSecondary,
    surfaceDim = elevatedSurface,
    surfaceBright = elevatedSurface,
    surfaceContainerLowest = elevatedSurface,
    surfaceContainerLow = elevatedSurface,
    surfaceContainer = elevatedSurface,
    surfaceContainerHigh = elevatedSurface,
    surfaceContainerHighest = elevatedSurface,
    surfaceTint = Color.Transparent,
    inverseSurface = snackbarContainer,
    inverseOnSurface = snackbarContent,
    inversePrimary = primaryContainer,
    error = error,
    onError = textOnPrimary,
    errorContainer = errorContainer,
    onErrorContainer = error,
    outline = inputBorder,
    outlineVariant = border,
    scrim = Color.Black
)

/**
 * 主题入口：同一个 [theme] 同时提供 LocalAppTheme、语义 Token 与 Material3，
 * 不再固定另一套 Coral 配色。
 */
@Composable
fun PandoraTheme(theme: AppTheme = appThemes.first(), content: @Composable () -> Unit) {
    // 五套主题都是浅色背景：系统栏图标统一深色（targetSdk 35 强制 edge-to-edge，默认图标为白色）
    val view = LocalView.current
    if (!view.isInEditMode) SideEffect {
        view.context.findActivity()?.window?.let { window ->
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = true
                isAppearanceLightNavigationBars = true
            }
        }
    }
    CompositionLocalProvider(LocalAppTheme provides theme, LocalPandoraColors provides theme.colors) {
        MaterialTheme(
            colorScheme = theme.colors.toMaterialColorScheme(),
            typography = PandoraMaterialTypography,
            shapes = PandoraMaterialShapes,
            content = content
        )
    }
}

@Composable
fun Pandora6ixTheme(content: @Composable () -> Unit) = PandoraTheme(content = content)

/**
 * 全局开关配色：开启用 primary 轨道，关闭用中性边圈，避免关闭态像错误或禁用。
 */
@Composable
fun pandoraSwitchColors() = PandoraTheme.colors.let { c ->
    androidx.compose.material3.SwitchDefaults.colors(
        checkedThumbColor = c.elevatedSurface,
        checkedTrackColor = c.primary,
        uncheckedThumbColor = c.inputBorder,
        uncheckedTrackColor = c.neutralContainer,
        uncheckedBorderColor = c.inputBorder
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
