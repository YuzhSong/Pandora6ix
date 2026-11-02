package com.pandora6ix.app.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** 圆角 Token（规范第 5 节）；胶囊用百分比而不是 999dp */
object PandoraShapes {
    val small = RoundedCornerShape(8.dp)
    val medium = RoundedCornerShape(12.dp)
    val large = RoundedCornerShape(16.dp)
    val xLarge = RoundedCornerShape(20.dp)
    val dialog = RoundedCornerShape(24.dp)
    val sheet = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    val pill = RoundedCornerShape(percent = 50)
    val circle = CircleShape
}

val PandoraMaterialShapes = Shapes(
    extraSmall = PandoraShapes.small,
    small = PandoraShapes.small,
    medium = PandoraShapes.medium,
    large = PandoraShapes.large,
    extraLarge = PandoraShapes.dialog
)
