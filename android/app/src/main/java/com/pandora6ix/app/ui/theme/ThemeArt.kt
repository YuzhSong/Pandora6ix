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
    @DrawableRes val calendarBackdrop: Int? = null
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
