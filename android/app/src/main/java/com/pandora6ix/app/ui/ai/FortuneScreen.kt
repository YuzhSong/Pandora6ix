package com.pandora6ix.app.ui.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.pandora6ix.app.data.AiRepository
import com.pandora6ix.app.data.FortuneDto
import com.pandora6ix.app.data.Zodiacs
import com.pandora6ix.app.ui.components.PandoraChoiceChip
import com.pandora6ix.app.ui.components.parseTagColor
import com.pandora6ix.app.ui.theme.PandoraDimensions
import com.pandora6ix.app.ui.theme.PandoraShapes
import com.pandora6ix.app.ui.theme.PandoraSpacing
import com.pandora6ix.app.ui.theme.PandoraTheme
import com.pandora6ix.app.ui.theme.PandoraTypography

/** 今日幸运签：进入即生成；可选星座（只用于本次生成，不保存） */
@Composable
internal fun FortuneScreen(repo: AiRepository) {
    var zodiac by rememberSaveable { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    var state by remember { mutableStateOf<AiLoad<FortuneDto>>(AiLoad.Idle) }
    LaunchedEffect(zodiac, retry) {
        state = AiLoad.Loading
        state = runCatching { repo.fortune(zodiac) }.fold({ AiLoad.Done(it) }, { AiLoad.Failed(aiErrorMessage(it)) })
    }
    AiPage {
        Text("选择星座（可选）", style = PandoraTypography.labelLarge, color = PandoraTheme.colors.textSecondary)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(PandoraSpacing.space8)) {
            PandoraChoiceChip("不选", zodiac == null, { zodiac = null })
            Zodiacs.forEach { (key, name) -> PandoraChoiceChip(name, zodiac == key, { zodiac = key }) }
        }
        when (val s = state) {
            AiLoad.Idle, AiLoad.Loading -> AiLoading("正在为你抽取今日幸运签…")
            is AiLoad.Failed -> AiError(s.message) { retry++ }
            is AiLoad.Done -> {
                FortuneCard(s.result.data)
                AiResultFooter(s.result)
            }
        }
    }
}

@Composable
private fun FortuneCard(f: FortuneDto) {
    val c = PandoraTheme.colors
    AiCard {
        Text(f.date + (f.zodiacName?.let { " · $it" } ?: ""), style = PandoraTypography.caption, color = c.textSecondary)
        Text("今日关键词：${f.keyword}", style = PandoraTypography.titleMedium, color = c.textPrimary)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("今日运势", Modifier.padding(end = PandoraSpacing.space8), style = PandoraTypography.bodyMedium, color = c.textSecondary)
            AiStars(f.stars)
        }
        Text("“${f.message}”", Modifier.fillMaxWidth().background(c.primaryContainer, PandoraShapes.medium).padding(PandoraSpacing.space16),
            style = PandoraTypography.bodyLarge, color = c.textPrimary, textAlign = TextAlign.Center)
    }
    AiCard("幸运小物") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val swatch = parseTagColor(f.luckyColor.hex) ?: c.neutralContainer
            Box(Modifier.size(PandoraDimensions.avatarMedium).background(swatch, PandoraShapes.circle).border(PandoraDimensions.borderWidth, c.border, PandoraShapes.circle))
            Text("幸运色：${f.luckyColor.name}", Modifier.padding(start = PandoraSpacing.space12), style = PandoraTypography.bodyLarge, color = c.textPrimary)
        }
        Text("幸运行动：${f.luckyAction}", style = PandoraTypography.bodyLarge, color = c.textPrimary)
    }
    AiCard("分项运势") {
        FortuneLine("工作运", f.workStars, f.workTip)
        FortuneLine("社交运", f.socialStars, f.socialTip)
        FortuneLine("灵感运", f.inspirationStars, f.inspirationTip)
        AiMeter("今日摸鱼指数", f.slackIndex / 100f, "${f.slackIndex}%")
    }
}

@Composable
private fun FortuneLine(title: String, stars: Int, tip: String) {
    Column(verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space4)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, Modifier.padding(end = PandoraSpacing.space8), style = PandoraTypography.labelLarge, color = PandoraTheme.colors.textPrimary)
            AiStars(stars)
        }
        if (tip.isNotBlank()) Text(tip, style = PandoraTypography.bodyMedium, color = PandoraTheme.colors.textSecondary)
    }
}
