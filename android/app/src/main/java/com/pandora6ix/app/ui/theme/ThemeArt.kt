package com.pandora6ix.app.ui.theme

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.pandora6ix.app.R

/**
 * 卡通主题素材（课程设计演示用，裁切自番茄ToDo 截图，见 android/README.md）。
 * [navIcons] 依次对应 导图、视图、日志、AI地图、我的；[navBand] 为场景地面色，用作导航条背景。
 */
data class ThemeArt(
    @DrawableRes val scene: Int,
    val navIcons: List<Int>,
    @DrawableRes val fab: Int,
    val navBand: Color,
    @DrawableRes val calendarBackdrop: Int? = null,
    /** 深色导航条（如童话公主）上的文字色；为空时沿用主题的 primary/textSecondary */
    val navLabel: Color? = null
)

val ToonCreamArt = ThemeArt(
    scene = R.drawable.toon_cream_scene,
    navIcons = listOf(R.drawable.toon_cream_nav_0, R.drawable.toon_cream_nav_1, R.drawable.toon_cream_nav_2, R.drawable.toon_cream_nav_3, R.drawable.toon_cream_nav_4),
    fab = R.drawable.toon_cream_fab,
    navBand = Color(0xFFFFEFB4),
    calendarBackdrop = R.drawable.toon_cream_calendar
)
val ToonBreezeArt = ThemeArt(
    scene = R.drawable.toon_breeze_scene,
    navIcons = listOf(R.drawable.toon_breeze_nav_0, R.drawable.toon_breeze_nav_1, R.drawable.toon_breeze_nav_2, R.drawable.toon_breeze_nav_3, R.drawable.toon_breeze_nav_4),
    fab = R.drawable.toon_breeze_fab,
    navBand = Color(0xFFE6F1AD)
)
val ToonFarmArt = ThemeArt(
    scene = R.drawable.toon_farm_scene,
    navIcons = listOf(R.drawable.toon_farm_nav_0, R.drawable.toon_farm_nav_1, R.drawable.toon_farm_nav_2, R.drawable.toon_farm_nav_3, R.drawable.toon_farm_nav_4),
    fab = R.drawable.toon_farm_fab,
    navBand = Color(0xFFFEDE84)
)
val ToonSpringArt = ThemeArt(
    scene = R.drawable.toon_spring_scene,
    navIcons = listOf(R.drawable.toon_spring_nav_0, R.drawable.toon_spring_nav_1, R.drawable.toon_spring_nav_2, R.drawable.toon_spring_nav_3, R.drawable.toon_spring_nav_4),
    fab = R.drawable.toon_spring_fab,
    navBand = Color(0xFFD4EA94)
)
val ToonWinterArt = ThemeArt(
    scene = R.drawable.toon_winter_scene,
    navIcons = listOf(R.drawable.toon_winter_nav_0, R.drawable.toon_winter_nav_1, R.drawable.toon_winter_nav_2, R.drawable.toon_winter_nav_3, R.drawable.toon_winter_nav_4),
    fab = R.drawable.toon_winter_fab,
    navBand = Color(0xFFFFFFFF)
)
val ToonStudyArt = ThemeArt(
    scene = R.drawable.toon_study_scene,
    navIcons = listOf(R.drawable.toon_study_nav_0, R.drawable.toon_study_nav_1, R.drawable.toon_study_nav_2, R.drawable.toon_study_nav_3, R.drawable.toon_study_nav_4),
    fab = R.drawable.toon_study_fab,
    navBand = Color(0xFFFEDADA)
)
// 导航条比插画水面略深（#827FD8 → #6E6AC8），白字对比度 4.6:1
val ToonFairyArt = ThemeArt(
    scene = R.drawable.toon_fairy_scene,
    navIcons = listOf(R.drawable.toon_fairy_nav_0, R.drawable.toon_fairy_nav_1, R.drawable.toon_fairy_nav_2, R.drawable.toon_fairy_nav_3, R.drawable.toon_fairy_nav_4),
    fab = R.drawable.toon_fairy_fab,
    navBand = Color(0xFF6E6AC8),
    navLabel = Color(0xFFFFFFFF)
)
