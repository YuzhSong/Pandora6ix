package com.pandora6ix.app.ui.ai

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pandora6ix.app.data.AiPreferences
import com.pandora6ix.app.data.AiRepository
import com.pandora6ix.app.data.AiResultDto
import com.pandora6ix.app.data.ApiException
import com.pandora6ix.app.data.SessionExpiredException
import com.pandora6ix.app.ui.components.PandoraButton
import com.pandora6ix.app.ui.components.PandoraButtonSize
import com.pandora6ix.app.ui.components.PandoraButtonVariant
import com.pandora6ix.app.ui.components.PandoraCard
import com.pandora6ix.app.ui.components.PandoraPageTitle
import com.pandora6ix.app.ui.components.PandoraPanel
import com.pandora6ix.app.ui.components.PandoraPanelTone
import com.pandora6ix.app.ui.components.PandoraTopBar
import com.pandora6ix.app.ui.theme.LocalAppTheme
import com.pandora6ix.app.ui.theme.PandoraColorTokens
import com.pandora6ix.app.ui.theme.PandoraDimensions
import com.pandora6ix.app.ui.theme.PandoraShapes
import com.pandora6ix.app.ui.theme.PandoraSpacing
import com.pandora6ix.app.ui.theme.PandoraTheme
import com.pandora6ix.app.ui.theme.PandoraTypography
import java.io.IOException

/** AI 探索馆的五个功能入口 */
enum class AiFeature(val title: String, val subtitle: String) {
    FORTUNE("今日幸运签", "今天会有什么惊喜？"),
    CAREER("我的职业星图", "从工作记录看发展方向"),
    COMPANY("公司下一站", "三种发展情景剧本"),
    PERSONALITY("职场人格盲盒", "12 道情景题测测自己"),
    TAROT("AI 塔罗屋", "抽三张牌看看近况")
}

/**
 * 第四个一级页面：保留“AI地图”标题与底部导航，内容为 AI 探索馆首页；
 * 功能页在页内切换，系统返回回到首页。
 */
@Composable
fun AiExplorerScreen(repo: AiRepository, ownerId: String) {
    val context = LocalContext.current
    val prefs = remember {
        val sp = context.getSharedPreferences("ai_explorer", Context.MODE_PRIVATE)
        AiPreferences({ sp.getString(it, null) }, { key, value -> sp.edit().putString(key, value).apply() })
    }
    var featureName by rememberSaveable(ownerId) { mutableStateOf<String?>(null) }
    val feature = featureName?.let(AiFeature::valueOf)
    if (feature != null) {
        BackHandler { featureName = null }
        val back = { featureName = null }
        Column(Modifier.fillMaxSize()) {
            PandoraTopBar(feature.title, subtitle = "AI 探索馆", onBack = back)
            when (feature) {
                AiFeature.FORTUNE -> FortuneScreen(repo)
                AiFeature.PERSONALITY -> PersonalityScreen(repo, prefs, ownerId)
                AiFeature.TAROT -> TarotScreen(repo)
                AiFeature.CAREER -> CareerMapScreen(repo, prefs, ownerId)
                AiFeature.COMPANY -> CompanyOutlookScreen(repo, prefs, ownerId)
            }
        }
        return
    }
    AiHome { featureName = it.name }
}

@Composable
private fun AiHome(onOpen: (AiFeature) -> Unit) {
    val c = PandoraTheme.colors
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = PandoraSpacing.pageHorizontal)) {
        PandoraPageTitle("AI地图")
        Text("AI 探索馆 · 让工作与未来多一点想象", style = PandoraTypography.bodyMedium, color = c.textSecondary)
        Spacer(Modifier.height(PandoraSpacing.space16))
        // 今日幸运签：首页顶部的大入口
        PandoraPanel(Modifier.fillMaxWidth(), onClick = { onOpen(AiFeature.FORTUNE) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FeatureBadge(Icons.Default.AutoAwesome, c.warningContainer, c.warning)
                Column(Modifier.weight(1f).padding(horizontal = PandoraSpacing.space12)) {
                    Text(AiFeature.FORTUNE.title, style = PandoraTypography.titleSmall, color = c.textPrimary)
                    Text(AiFeature.FORTUNE.subtitle, style = PandoraTypography.bodyMedium, color = c.textSecondary)
                }
                PandoraButton("立即抽签", { onOpen(AiFeature.FORTUNE) }, size = PandoraButtonSize.Compact)
            }
        }
        SectionTitle("工作实验室", "基于你的工作记录，仅供参考")
        FeatureRow(AiFeature.CAREER, Icons.Default.Explore, c.infoContainer, c.info, AiFeature.COMPANY, Icons.Default.Business, c.successContainer, c.success, onOpen)
        SectionTitle("灵感游乐场", "轻松一下，不读取工作数据")
        FeatureRow(AiFeature.PERSONALITY, Icons.Default.Face, c.primaryContainer, c.primary, AiFeature.TAROT, Icons.Default.Style, c.errorContainer, c.error, onOpen)
        Spacer(Modifier.height(PandoraSpacing.space16))
        Text("所有内容由 AI 或预设规则生成，仅供娱乐与参考，不构成任何评价、预测或建议。",
            style = PandoraTypography.caption, color = c.textTertiary)
        // 卡通主题底部有场景插画：留出空间，滚到底时文字不压在插画上
        Spacer(Modifier.height(if (LocalAppTheme.current.art != null) 160.dp else PandoraSpacing.space24))
    }
}

@Composable
private fun SectionTitle(title: String, caption: String) {
    Spacer(Modifier.height(PandoraSpacing.section))
    Text(title, Modifier.semantics { heading() }, style = PandoraTypography.titleMedium, color = PandoraTheme.colors.textPrimary)
    Text(caption, style = PandoraTypography.caption, color = PandoraTheme.colors.textSecondary)
    Spacer(Modifier.height(PandoraSpacing.titleToContent))
}

@Composable
private fun FeatureRow(
    left: AiFeature, leftIcon: ImageVector, leftBg: Color, leftTint: Color,
    right: AiFeature, rightIcon: ImageVector, rightBg: Color, rightTint: Color,
    onOpen: (AiFeature) -> Unit
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PandoraSpacing.space12)) {
        FeatureCard(left, leftIcon, leftBg, leftTint, Modifier.weight(1f)) { onOpen(left) }
        FeatureCard(right, rightIcon, rightBg, rightTint, Modifier.weight(1f)) { onOpen(right) }
    }
}

@Composable
private fun FeatureCard(feature: AiFeature, icon: ImageVector, background: Color, tint: Color, modifier: Modifier, onClick: () -> Unit) {
    val c = PandoraTheme.colors
    PandoraCard(modifier, onClick = onClick) {
        FeatureBadge(icon, background, tint)
        Spacer(Modifier.height(PandoraSpacing.space12))
        Text(feature.title, style = PandoraTypography.titleSmall, color = c.textPrimary, maxLines = 1)
        Text(feature.subtitle, style = PandoraTypography.caption, color = c.textSecondary, maxLines = 2)
        Spacer(Modifier.height(PandoraSpacing.space8))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("进入", style = PandoraTypography.labelLarge, color = c.primary)
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, Modifier.size(PandoraDimensions.iconSmall + 4.dp), tint = c.primary)
        }
    }
}

/** 功能图标底：语义容器色 + 对应深色图标 */
@Composable
internal fun FeatureBadge(icon: ImageVector, background: Color, tint: Color) {
    Box(Modifier.size(PandoraDimensions.avatarLarge).background(background, PandoraShapes.medium), contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(PandoraDimensions.icon), tint = tint)
    }
}

// ---------- 功能页通用 ----------

/** 单次生成请求的界面状态；输入由各页面自行保存，失败重试不丢失 */
sealed interface AiLoad<out T> {
    data object Idle : AiLoad<Nothing>
    data object Loading : AiLoad<Nothing>
    data class Failed(val message: String) : AiLoad<Nothing>
    data class Done<T>(val result: AiResultDto<T>) : AiLoad<T>
}

/** 中文错误提示：业务错误用统一文案，网络错误提示检查网络 */
fun aiErrorMessage(error: Throwable): String = when (error) {
    is SessionExpiredException -> "登录已过期，请重新登录"
    is ApiException -> error.friendlyMessage()
    is IOException -> "网络不可用或响应超时，请检查网络后重试"
    else -> "生成失败，请稍后重试"
}

/** 功能页滚动容器：页面左右 16dp，卡片间距 12dp */
@Composable
internal fun AiPage(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = PandoraSpacing.pageHorizontal, vertical = PandoraSpacing.space8),
        verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space12),
        content = {
            content()
            Spacer(Modifier.height(PandoraSpacing.space24))
        }
    )
}

@Composable
internal fun AiLoading(text: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = PandoraSpacing.space48), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space16)) {
        CircularProgressIndicator(color = PandoraTheme.colors.primary)
        Text(text, style = PandoraTypography.bodyMedium, color = PandoraTheme.colors.textSecondary, textAlign = TextAlign.Center)
    }
}

@Composable
internal fun AiError(message: String, onRetry: () -> Unit) {
    PandoraPanel(Modifier.fillMaxWidth()) {
        Text(message, style = PandoraTypography.bodyMedium, color = PandoraTheme.colors.error)
        Spacer(Modifier.height(PandoraSpacing.space12))
        PandoraButton("重试", onRetry, variant = PandoraButtonVariant.Outline)
    }
}

/** 结果页底部：兜底/限额提示 + 免责声明 */
@Composable
internal fun AiResultFooter(result: AiResultDto<*>) {
    val c = PandoraTheme.colors
    if (!result.aiGenerated) {
        val hint = if (result.quotaExceeded) "今日 AI 生成次数已用完，以下为基础版内容，明天再来看看吧。"
        else "AI 暂时不可用，以下为基础版内容。"
        Row(Modifier.fillMaxWidth().background(c.infoContainer, PandoraShapes.medium).padding(PandoraSpacing.space12),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Info, null, Modifier.size(PandoraDimensions.iconSmall), tint = c.info)
            Text(hint, Modifier.padding(start = PandoraSpacing.space8), style = PandoraTypography.caption, color = c.info)
        }
    }
    Text(result.disclaimer, style = PandoraTypography.caption, color = c.textTertiary)
}

/** 1–5 星；读屏读出“几星” */
@Composable
internal fun AiStars(count: Int, tint: Color = PandoraTheme.colors.warning) {
    val value = count.coerceIn(0, 5)
    Row(Modifier.semantics { contentDescription = "${value} 星" }) {
        repeat(5) { index ->
            Icon(if (index < value) Icons.Default.Star else Icons.Default.StarBorder, null, Modifier.size(PandoraDimensions.iconSmall + 2.dp), tint = tint)
        }
    }
}

/** 白色结果卡片 */
@Composable
internal fun AiCard(title: String? = null, content: @Composable ColumnScope.() -> Unit) {
    PandoraPanel(Modifier.fillMaxWidth(), tone = PandoraPanelTone.White) {
        if (title != null) {
            Text(title, Modifier.semantics { heading() }, style = PandoraTypography.titleSmall, color = PandoraTheme.colors.textPrimary)
            Spacer(Modifier.height(PandoraSpacing.space8))
        }
        Column(verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space8), content = content)
    }
}

/** 圆点列表项 */
@Composable
internal fun AiBullet(text: String, color: Color = PandoraTheme.colors.textPrimary) {
    Row {
        Text("•", Modifier.padding(end = PandoraSpacing.space8), style = PandoraTypography.bodyMedium, color = PandoraTheme.colors.primary)
        Text(text, style = PandoraTypography.bodyMedium, color = color)
    }
}

/** 0–100 横条：主题 Primary 填充、NeutralContainer 轨道 */
@Composable
internal fun AiMeter(label: String, ratio: Float, trailing: String) {
    val c = PandoraTheme.colors
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            Text(label, Modifier.weight(1f), style = PandoraTypography.bodyMedium, color = c.textPrimary)
            Text(trailing, style = PandoraTypography.caption, color = c.textSecondary)
        }
        Spacer(Modifier.height(PandoraSpacing.space4))
        Box(Modifier.fillMaxWidth().height(PandoraSpacing.space8).background(c.neutralContainer, PandoraShapes.pill)) {
            Box(Modifier.fillMaxWidth(ratio.coerceIn(0.02f, 1f)).height(PandoraSpacing.space8).background(c.primary, PandoraShapes.pill))
        }
    }
}

/** 首次使用工作数据功能前的说明；确认后才开始生成 */
@Composable
internal fun AiDataNotice(onAccept: () -> Unit) {
    val c: PandoraColorTokens = PandoraTheme.colors
    AiCard("使用前说明") {
        Text("生成时会把你可见的工作记录整理成摘要和关键词，发送给第三方 AI 服务（DeepSeek）。",
            style = PandoraTypography.bodyMedium, color = c.textPrimary)
        AiBullet("同事姓名会替换为“同事”，手机号、邮箱、网址等会被隐藏")
        AiBullet("只发送统计数字、标签和截断后的少量摘录，不发送全文")
        AiBullet("结果仅供参考，不保存到服务器，也不会展示给其他同事")
        Spacer(Modifier.height(PandoraSpacing.space4))
        PandoraButton("我知道了，开始生成", onAccept, Modifier.fillMaxWidth())
    }
}
