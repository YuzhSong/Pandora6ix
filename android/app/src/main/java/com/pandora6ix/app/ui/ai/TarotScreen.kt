package com.pandora6ix.app.ui.ai

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.pandora6ix.app.data.AiRepository
import com.pandora6ix.app.data.TarotCardDto
import com.pandora6ix.app.data.TarotDto
import com.pandora6ix.app.ui.components.PandoraButton
import com.pandora6ix.app.ui.components.PandoraButtonVariant
import com.pandora6ix.app.ui.components.PandoraChoiceChip
import com.pandora6ix.app.ui.components.PandoraTextField
import com.pandora6ix.app.ui.theme.PandoraDimensions
import com.pandora6ix.app.ui.theme.PandoraShapes
import com.pandora6ix.app.ui.theme.PandoraSpacing
import com.pandora6ix.app.ui.theme.PandoraTheme
import com.pandora6ix.app.ui.theme.PandoraTypography
import kotlinx.coroutines.launch

private val PresetQuestions = listOf("我今天的工作运势如何？", "最近适合尝试新的挑战吗？", "接下来一周会有什么惊喜？")

/** AI 塔罗屋：后端抽牌，逐张点击翻牌，三张都翻开后展示整体解读 */
@Composable
internal fun TarotScreen(repo: AiRepository) {
    val scope = rememberCoroutineScope()
    var question by rememberSaveable { mutableStateOf("") }
    var state by remember { mutableStateOf<AiLoad<TarotDto>>(AiLoad.Idle) }
    val flipped = remember { mutableStateListOf(false, false, false) }

    fun draw() {
        state = AiLoad.Loading
        for (i in flipped.indices) flipped[i] = false
        scope.launch { state = runCatching { repo.tarot(question) }.fold({ AiLoad.Done(it) }, { AiLoad.Failed(aiErrorMessage(it)) }) }
    }

    AiPage {
        val c = PandoraTheme.colors
        when (val s = state) {
            is AiLoad.Done -> {
                val tarot = s.result.data
                tarot.question?.let { Text("你的问题：$it", style = PandoraTypography.bodyMedium, color = c.textSecondary) }
                Text(if (flipped.all { it }) "三张牌都已翻开" else "依次点击卡牌翻开（过去 → 现在 → 未来）",
                    style = PandoraTypography.caption, color = c.textSecondary)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PandoraSpacing.space8)) {
                    tarot.cards.forEachIndexed { i, card ->
                        TarotCard(card, flipped.getOrElse(i) { false }, Modifier.weight(1f)) { if (i < flipped.size) flipped[i] = true }
                    }
                }
                tarot.cards.forEachIndexed { i, card ->
                    if (flipped.getOrElse(i) { false }) AiCard("${card.position} · ${card.name}${if (card.upright) "（正位）" else "（逆位）"}") {
                        Text(card.keywords.joinToString(" · "), style = PandoraTypography.caption, color = c.textSecondary)
                        Text(card.reading, style = PandoraTypography.bodyLarge, color = c.textPrimary)
                    }
                }
                if (flipped.all { it }) {
                    AiCard("整体解读") {
                        Text(tarot.summary, style = PandoraTypography.bodyLarge, color = c.textPrimary)
                        Text("今日小行动：${tarot.advice}", style = PandoraTypography.bodyMedium, color = c.primary)
                    }
                    AiResultFooter(s.result)
                    PandoraButton("再抽一次", { state = AiLoad.Idle }, Modifier.fillMaxWidth(), variant = PandoraButtonVariant.Secondary)
                }
            }
            else -> {
                Text("想问塔罗什么？", style = PandoraTypography.titleMedium, color = c.textPrimary)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(PandoraSpacing.space8)) {
                    PresetQuestions.forEach { preset -> PandoraChoiceChip(preset, question == preset, { question = preset }) }
                }
                PandoraTextField(question, { if (it.length <= 100) question = it }, Modifier.fillMaxWidth(),
                    placeholder = { Text("也可以自己写一个问题（可留空）") },
                    supportingText = { Text("${question.length}/100 · 问题会发送给 AI，请勿输入姓名、电话等敏感信息") },
                    minLines = 2)
                when (s) {
                    AiLoad.Loading -> AiLoading("正在洗牌、抽牌…")
                    is AiLoad.Failed -> AiError(s.message) { draw() }
                    else -> PandoraButton("洗牌并抽取三张牌", { draw() }, Modifier.fillMaxWidth())
                }
                Text("塔罗为娱乐内容，抽牌由程序随机完成，不代表真实的预测。", style = PandoraTypography.caption, color = c.textTertiary)
            }
        }
    }
}

/** 牌背用深色 Snackbar Token（神秘感），翻开后为白底牌面；rotationY 动画 */
@Composable
private fun TarotCard(card: TarotCardDto, faceUp: Boolean, modifier: Modifier, onFlip: () -> Unit) {
    val c = PandoraTheme.colors
    val rotation by animateFloatAsState(if (faceUp) 180f else 0f, tween(500), label = "tarot-flip")
    val showFace = rotation > 90f
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space4)) {
        Text(card.position, style = PandoraTypography.labelLarge, color = c.textSecondary)
        Box(
            Modifier.fillMaxWidth().aspectRatio(0.62f)
                .graphicsLayer { rotationY = rotation; cameraDistance = 12f * density }
                .background(if (showFace) c.elevatedSurface else c.snackbarContainer, PandoraShapes.medium)
                .border(PandoraDimensions.borderWidth, if (showFace) c.border else c.snackbarContainer, PandoraShapes.medium)
                .clickable(enabled = !faceUp, role = Role.Button, onClickLabel = "翻开${card.position}的牌", onClick = onFlip)
                .semantics { contentDescription = if (faceUp) "${card.position}：${card.name}" else "${card.position}，未翻开" },
            contentAlignment = Alignment.Center
        ) {
            if (showFace) Column(Modifier.fillMaxSize().graphicsLayer { rotationY = 180f }.padding(PandoraSpacing.space8),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(roman(card.number), style = PandoraTypography.caption, color = c.textSecondary)
                Text(card.name, style = PandoraTypography.titleSmall, color = c.textPrimary, textAlign = TextAlign.Center)
                Text(if (card.upright) "正位" else "逆位", style = PandoraTypography.caption, color = if (card.upright) c.success else c.error)
            } else Text("✦", style = PandoraTypography.display, color = c.snackbarContent)
        }
    }
}

private fun roman(n: Int): String {
    if (n == 0) return "0"
    val values = listOf(10 to "X", 9 to "IX", 5 to "V", 4 to "IV", 1 to "I")
    var rest = n
    return buildString { for ((v, s) in values) while (rest >= v) { append(s); rest -= v } }
}
