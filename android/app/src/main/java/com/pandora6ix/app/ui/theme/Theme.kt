package com.pandora6ix.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Cream = Color(0xFFFFFBF2)
val CreamDeep = Color(0xFFFFF2C9)
val Coral = Color(0xFFFF8377)
val CoralDark = Color(0xFFB34E4A)
val Ink = Color(0xFF302B2A)
val WarmDashboard = Color(0xFFFFF1C7)
val WarmCard = Color(0xFFFFFCF4)
val WarmOrange = Color(0xFFE3A083)
val Peach = Color(0xFFFFE0D8)
val Mint = Color(0xFFDDEEDC)
val Lilac = Color(0xFFE7DDF4)
val Sky = Color(0xFFDCECF2)

private val PandoraColors = lightColorScheme(
    primary = Coral,
    onPrimary = Color.White,
    secondary = CreamDeep,
    background = Cream,
    surface = Color.White,
    onBackground = Ink,
    onSurface = Ink,
    outline = Coral.copy(alpha = 0.65f)
)

@Composable
fun PandoraTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = PandoraColors, content = content)
}

@Composable
fun Pandora6ixTheme(content: @Composable () -> Unit) = PandoraTheme(content)
