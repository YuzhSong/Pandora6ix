package com.pandora6ix.app.ui.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.pandora6ix.app.data.AiPreferences
import com.pandora6ix.app.data.AiRepository
import com.pandora6ix.app.data.AiResultDto
import com.pandora6ix.app.data.PersonalityDto
import com.pandora6ix.app.data.PersonalityQuestionsDto
import com.pandora6ix.app.ui.components.PandoraButton
import com.pandora6ix.app.ui.components.PandoraButtonVariant
import com.pandora6ix.app.ui.components.PandoraItemContainer
import com.pandora6ix.app.ui.theme.PandoraSpacing
import com.pandora6ix.app.ui.theme.PandoraTheme
import com.pandora6ix.app.ui.theme.PandoraTypography
import kotlinx.coroutines.launch

/** 职场人格盲盒：逐题作答 → 程序计分 → AI 解读；最近一次结果按账号保存在本机 */
@Composable
internal fun PersonalityScreen(repo: AiRepository, prefs: AiPreferences, ownerId: String) {
    val scope = rememberCoroutineScope()
    var saved by remember(ownerId) { mutableStateOf(repo.decodePersonality(prefs.personality(ownerId))) }
    var testing by remember { mutableStateOf(saved == null) }
    var questions by remember { mutableStateOf<PersonalityQuestionsDto?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    val answers = remember { mutableStateListOf<String>() }
    var submit by remember { mutableStateOf<AiLoad<PersonalityDto>>(AiLoad.Idle) }

    fun send() {
        submit = AiLoad.Loading
        scope.launch {
            submit = runCatching { repo.personality(answers.toList()) }.fold({ result ->
                prefs.savePersonality(ownerId, repo.encodePersonality(result))
                saved = result
                testing = false
                AiLoad.Done(result)
            }, { AiLoad.Failed(aiErrorMessage(it)) })
        }
    }

    LaunchedEffect(testing, reload) {
        if (testing && questions == null) {
            loadError = null
            runCatching { repo.personalityQuestions() }.onSuccess { questions = it }.onFailure { loadError = aiErrorMessage(it) }
        }
    }

    AiPage {
        val result = saved
        when {
            !testing && result != null -> {
                PersonalityResult(result)
                PandoraButton("重新测试", {
                    answers.clear(); submit = AiLoad.Idle; testing = true
                }, Modifier.fillMaxWidth(), variant = PandoraButtonVariant.Secondary)
            }
            submit is AiLoad.Loading -> AiLoading("正在打开你的人格盲盒…")
            submit is AiLoad.Failed -> AiError((submit as AiLoad.Failed).message) { send() }
            loadError != null -> AiError(loadError!!) { reload++ }
            questions == null -> AiLoading("正在准备题目…")
            else -> {
                val list = questions!!.questions
                val index = answers.size.coerceAtMost(list.size - 1)
                val question = list[index]
                Text("第 ${index + 1} / ${list.size} 题", style = PandoraTypography.caption, color = PandoraTheme.colors.textSecondary)
                LinearProgressIndicator(progress = { (answers.size) / list.size.toFloat() }, Modifier.fillMaxWidth(),
                    color = PandoraTheme.colors.primary, trackColor = PandoraTheme.colors.neutralContainer)
                Text(question.text, style = PandoraTypography.titleMedium, color = PandoraTheme.colors.textPrimary)
                question.options.forEach { option ->
                    PandoraItemContainer(Modifier.fillMaxWidth(), onClick = {
                        answers.add(option.id)
                        if (answers.size == list.size) send()
                    }) {
                        Text(option.text, style = PandoraTypography.bodyLarge, color = PandoraTheme.colors.textPrimary)
                    }
                }
                if (answers.isNotEmpty()) PandoraButton("上一题", { answers.removeAt(answers.lastIndex) }, variant = PandoraButtonVariant.Text)
                Text("这是 Pandora 原创的娱乐测试，不是专业心理测评，结果不代表能力评价。",
                    style = PandoraTypography.caption, color = PandoraTheme.colors.textTertiary)
            }
        }
    }
}

@Composable
private fun PersonalityResult(result: AiResultDto<PersonalityDto>) {
    val p = result.data
    val c = PandoraTheme.colors
    AiCard {
        Text(p.archetype.emoji + "  " + p.archetype.code, style = PandoraTypography.caption, color = c.textSecondary)
        Text("你是：${p.archetype.name}", style = PandoraTypography.titleLarge, color = c.textPrimary)
        Text(p.archetype.tagline, style = PandoraTypography.bodyMedium, color = c.textSecondary)
        Text(p.headline, style = PandoraTypography.titleSmall, color = c.primary)
        Text(p.portrait, style = PandoraTypography.bodyLarge, color = c.textPrimary)
    }
    AiCard("隐藏属性") {
        p.dimensions.forEach { AiMeter(it.name, it.score / 100f, "${it.score}") }
        Row(horizontalArrangement = Arrangement.spacedBy(PandoraSpacing.space8)) {
            Text("摸鱼天赋", style = PandoraTypography.bodyMedium, color = c.textPrimary)
            AiStars(p.slackTalent)
        }
    }
    AiCard("你的闪光点") { p.strengths.forEach { AiBullet(it) } }
    AiCard("小心这些坑") { p.watchouts.forEach { AiBullet(it) } }
    AiCard("工作小建议") { Text(p.workStyleTip, style = PandoraTypography.bodyLarge, color = c.textPrimary) }
    AiResultFooter(result)
}
