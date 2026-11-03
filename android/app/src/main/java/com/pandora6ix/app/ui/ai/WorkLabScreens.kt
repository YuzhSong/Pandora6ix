package com.pandora6ix.app.ui.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.pandora6ix.app.data.AiPreferences
import com.pandora6ix.app.data.AiRepository
import com.pandora6ix.app.data.AiResultDto
import com.pandora6ix.app.data.CareerMapDto
import com.pandora6ix.app.data.CompanyOutlookDto
import com.pandora6ix.app.data.CompanyScenarioDto
import com.pandora6ix.app.data.NamedCountDto
import com.pandora6ix.app.data.abilityLevelLabel
import com.pandora6ix.app.data.abilityLevelRatio
import com.pandora6ix.app.ui.components.PandoraButton
import com.pandora6ix.app.ui.components.PandoraButtonVariant
import com.pandora6ix.app.ui.components.PandoraChoiceChip
import com.pandora6ix.app.ui.components.PandoraItemContainer
import com.pandora6ix.app.ui.components.PandoraTagChip
import com.pandora6ix.app.ui.theme.PandoraDimensions
import com.pandora6ix.app.ui.theme.PandoraSpacing
import com.pandora6ix.app.ui.theme.PandoraTheme
import com.pandora6ix.app.ui.theme.PandoraTypography
import kotlinx.coroutines.launch

/**
 * 工作实验室共用流程：首次使用先确认数据说明，再生成；结果可重新生成。
 * 生成只读取本人/可见范围的数据摘要，不修改任何业务数据。
 */
@Composable
private fun <T> WorkLab(
    prefs: AiPreferences,
    ownerId: String,
    intro: String,
    loadingText: String,
    request: suspend () -> AiResultDto<T>,
    content: @Composable (AiResultDto<T>) -> Unit
) {
    val scope = rememberCoroutineScope()
    var accepted by remember(ownerId) { mutableStateOf(prefs.dataNoticeAccepted(ownerId)) }
    var state by remember { mutableStateOf<AiLoad<T>>(AiLoad.Idle) }
    fun generate() {
        state = AiLoad.Loading
        scope.launch { state = runCatching { request() }.fold({ AiLoad.Done(it) }, { AiLoad.Failed(aiErrorMessage(it)) }) }
    }
    // 已确认过数据说明：进入即生成，不再多点一次
    LaunchedEffect(Unit) { if (accepted && state == AiLoad.Idle) generate() }
    AiPage {
        Text(intro, style = PandoraTypography.bodyMedium, color = PandoraTheme.colors.textSecondary)
        when (val s = state) {
            AiLoad.Idle -> if (!accepted) AiDataNotice {
                prefs.acceptDataNotice(ownerId); accepted = true; generate()
            } else AiLoading(loadingText)
            AiLoad.Loading -> AiLoading(loadingText)
            is AiLoad.Failed -> AiError(s.message) { generate() }
            is AiLoad.Done -> {
                content(s.result)
                AiResultFooter(s.result)
                PandoraButton("重新生成", { generate() }, Modifier.fillMaxWidth(), variant = PandoraButtonVariant.Secondary)
            }
        }
    }
}

@Composable
internal fun CareerMapScreen(repo: AiRepository, prefs: AiPreferences, ownerId: String) {
    WorkLab(prefs, ownerId, "根据你近 30 天的日志与任务，看看记录里体现出的工作方向。", "正在绘制你的职业星图…", { repo.careerMap() }) { result ->
        CareerMapResult(result.data)
    }
}

@Composable
internal fun CompanyOutlookScreen(repo: AiRepository, prefs: AiPreferences, ownerId: String) {
    WorkLab(prefs, ownerId, "根据你可见的公司重要事项与任务统计，推演三种可能的发展剧本。", "正在推演公司的下一站…", { repo.companyOutlook() }) { result ->
        CompanyOutlookResult(result.data)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagCloud(tags: List<NamedCountDto>) {
    if (tags.isEmpty()) return
    FlowRow(horizontalArrangement = Arrangement.spacedBy(PandoraSpacing.space8), verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space8)) {
        tags.forEach { PandoraTagChip("${it.name} ×${it.count}", null) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CareerMapResult(map: CareerMapDto) {
    val c = PandoraTheme.colors
    val stats = map.stats
    if (map.empty) {
        AiCard { Text(map.guidance ?: "记录还不够多，过段时间再来吧。", style = PandoraTypography.bodyLarge, color = c.textPrimary) }
        return
    }
    AiCard {
        Text("当前角色", style = PandoraTypography.caption, color = c.textSecondary)
        Text(map.label, style = PandoraTypography.titleLarge, color = c.primary)
        Text(map.summary, style = PandoraTypography.bodyLarge, color = c.textPrimary)
        Text("近 ${stats.windowDays} 天 · 日志 ${stats.journalCount} 篇 · 工作条目 ${stats.entryCount} 条 · 任务 ${stats.taskCount} 项（完成 ${stats.completedTaskCount}）",
            style = PandoraTypography.caption, color = c.textSecondary)
        TagCloud(map.topTags)
    }
    AiCard("能力方向（记录中的体现程度）") {
        map.abilities.forEach { ability ->
            AiMeter(ability.name, abilityLevelRatio(ability.level), abilityLevelLabel(ability.level))
            Text(ability.evidence, style = PandoraTypography.caption, color = c.textSecondary)
        }
    }
    if (map.routes.isNotEmpty()) {
        var selected by rememberSaveable { mutableStateOf(0) }
        val route = map.routes[selected.coerceIn(0, map.routes.lastIndex)]
        AiCard("三条发展路线") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(PandoraSpacing.space8), verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space8)) {
                map.routes.forEachIndexed { i, r -> PandoraChoiceChip("${r.key}. ${r.title}", i == selected, { selected = i }) }
            }
            Text(route.description, style = PandoraTypography.bodyLarge, color = c.textPrimary)
            Text("接下来可以试试", style = PandoraTypography.labelLarge, color = c.textSecondary)
            route.actions.forEach { AiBullet(it) }
        }
    }
}

@Composable
private fun CompanyOutlookResult(outlook: CompanyOutlookDto) {
    val c = PandoraTheme.colors
    if (outlook.empty) {
        AiCard { Text(outlook.guidance ?: "可见数据还不够多，过段时间再来吧。", style = PandoraTypography.bodyLarge, color = c.textPrimary) }
        return
    }
    val s = outlook.stats
    AiCard("公司现在在哪里") {
        Text(outlook.overview, style = PandoraTypography.bodyLarge, color = c.textPrimary)
        Text("近 ${s.windowDays} 天可见：重要事项 ${s.importantItemCount} 条 · 任务 ${s.taskCount} 项；执行人次：已完成 ${s.completedCount} · 进行中 ${s.inProgressCount} · 逾期 ${s.overdueCount}",
            style = PandoraTypography.caption, color = c.textSecondary)
        TagCloud(outlook.topTags)
    }
    outlook.scenarios.forEach { ScenarioCard(it) }
}

@Composable
private fun ScenarioCard(scenario: CompanyScenarioDto) {
    val c = PandoraTheme.colors
    var expanded by rememberSaveable(scenario.key) { mutableStateOf(false) }
    val (badge, tint) = when (scenario.key) {
        "RAPID" -> "🚀" to c.success
        "CHALLENGE" -> "⚡" to c.warning
        else -> "🌿" to c.info
    }
    PandoraItemContainer(Modifier.fillMaxWidth(), color = c.elevatedSurface, onClick = { expanded = !expanded }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("$badge ${scenario.title}", Modifier.weight(1f), style = PandoraTypography.titleSmall, color = tint)
            Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, if (expanded) "收起" else "展开",
                Modifier.size(PandoraDimensions.icon), tint = c.textSecondary)
        }
        Text(scenario.developments, style = PandoraTypography.bodyMedium, color = c.textPrimary)
        if (expanded) {
            Text("支持这一判断的信息", style = PandoraTypography.labelLarge, color = c.textSecondary)
            scenario.signals.forEach { AiBullet(it) }
            Text("公司可以提前做的准备", style = PandoraTypography.labelLarge, color = c.textSecondary)
            scenario.preparations.forEach { AiBullet(it) }
        }
    }
}
