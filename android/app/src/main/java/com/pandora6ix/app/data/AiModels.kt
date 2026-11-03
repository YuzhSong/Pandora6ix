package com.pandora6ix.app.data

import kotlinx.serialization.Serializable

/**
 * AI 探索馆接口模型（/api/v1/ai 下各接口）。统一包装：aiGenerated=false 表示基础版兜底内容，
 * quotaExceeded=true 表示今日 AI 次数已用完；disclaimer 必须在结果页展示。
 */
@Serializable
data class AiResultDto<T>(
    val feature: String = "",
    val aiGenerated: Boolean = false,
    val quotaExceeded: Boolean = false,
    val disclaimer: String = "",
    val generatedAt: String = "",
    val data: T
)

@Serializable
data class NamedCountDto(val name: String = "", val count: Long = 0)

// ---------- 今日幸运签 ----------

@Serializable
data class LuckyColorDto(val name: String = "", val hex: String = "")

@Serializable
data class FortuneDto(
    val date: String = "",
    val zodiac: String? = null,
    val zodiacName: String? = null,
    val stars: Int = 3,
    val luckyColor: LuckyColorDto = LuckyColorDto(),
    val keyword: String = "",
    val luckyAction: String = "",
    val workStars: Int = 3,
    val socialStars: Int = 3,
    val inspirationStars: Int = 3,
    val slackIndex: Int = 0,
    val message: String = "",
    val workTip: String = "",
    val socialTip: String = "",
    val inspirationTip: String = ""
)

// ---------- 职场人格盲盒 ----------

@Serializable
data class PersonalityOptionDto(val id: String = "", val text: String = "")

@Serializable
data class PersonalityQuestionDto(val id: String = "", val text: String = "", val options: List<PersonalityOptionDto> = emptyList())

@Serializable
data class PersonalityQuestionsDto(val version: String = "", val questions: List<PersonalityQuestionDto> = emptyList())

@Serializable
data class PersonalityAnswersBody(val optionIds: List<String>)

@Serializable
data class ArchetypeDto(val key: String = "", val name: String = "", val code: String = "", val emoji: String = "", val tagline: String = "")

@Serializable
data class DimensionDto(val key: String = "", val name: String = "", val score: Int = 0)

@Serializable
data class PersonalityDto(
    val archetype: ArchetypeDto = ArchetypeDto(),
    val dimensions: List<DimensionDto> = emptyList(),
    val slackTalent: Int = 3,
    val headline: String = "",
    val portrait: String = "",
    val strengths: List<String> = emptyList(),
    val watchouts: List<String> = emptyList(),
    val workStyleTip: String = ""
)

// ---------- AI 塔罗屋 ----------

@Serializable
data class TarotDrawBody(val question: String? = null)

@Serializable
data class TarotCardDto(
    val position: String = "",
    val number: Int = 0,
    val name: String = "",
    val upright: Boolean = true,
    val keywords: List<String> = emptyList(),
    val reading: String = ""
)

@Serializable
data class TarotDto(val question: String? = null, val cards: List<TarotCardDto> = emptyList(), val summary: String = "", val advice: String = "")

// ---------- 我的职业星图 ----------

@Serializable
data class CareerStatsDto(val windowDays: Int = 30, val journalCount: Long = 0, val entryCount: Long = 0, val taskCount: Long = 0, val completedTaskCount: Long = 0)

@Serializable
data class AbilityDto(val name: String = "", val level: String = "INSUFFICIENT", val evidence: String = "")

@Serializable
data class CareerRouteDto(val key: String = "", val title: String = "", val description: String = "", val actions: List<String> = emptyList())

@Serializable
data class CareerMapDto(
    val empty: Boolean = false,
    val guidance: String? = null,
    val stats: CareerStatsDto = CareerStatsDto(),
    val topTags: List<NamedCountDto> = emptyList(),
    val label: String = "",
    val summary: String = "",
    val abilities: List<AbilityDto> = emptyList(),
    val routes: List<CareerRouteDto> = emptyList()
)

// ---------- 公司下一站 ----------

@Serializable
data class CompanyStatsDto(
    val windowDays: Int = 90,
    val importantItemCount: Long = 0,
    val taskCount: Long = 0,
    val completedCount: Long = 0,
    val inProgressCount: Long = 0,
    val overdueCount: Long = 0
)

@Serializable
data class CompanyScenarioDto(
    val key: String = "",
    val title: String = "",
    val developments: String = "",
    val signals: List<String> = emptyList(),
    val preparations: List<String> = emptyList()
)

@Serializable
data class CompanyOutlookDto(
    val empty: Boolean = false,
    val guidance: String? = null,
    val stats: CompanyStatsDto = CompanyStatsDto(),
    val topTags: List<NamedCountDto> = emptyList(),
    val overview: String = "",
    val scenarios: List<CompanyScenarioDto> = emptyList()
)

/** 能力方向体现程度的中文标签与条形比例（0–1） */
fun abilityLevelLabel(level: String): String = when (level) {
    "HIGH" -> "体现充分"
    "MEDIUM" -> "有所体现"
    "LOW" -> "少量体现"
    else -> "暂无充分记录"
}

fun abilityLevelRatio(level: String): Float = when (level) {
    "HIGH" -> 1f
    "MEDIUM" -> 0.66f
    "LOW" -> 0.33f
    else -> 0.08f
}

/** 十二星座 key 与中文名，与后端 fortune.json 一致 */
val Zodiacs: List<Pair<String, String>> = listOf(
    "ARIES" to "白羊座", "TAURUS" to "金牛座", "GEMINI" to "双子座", "CANCER" to "巨蟹座",
    "LEO" to "狮子座", "VIRGO" to "处女座", "LIBRA" to "天秤座", "SCORPIO" to "天蝎座",
    "SAGITTARIUS" to "射手座", "CAPRICORN" to "摩羯座", "AQUARIUS" to "水瓶座", "PISCES" to "双鱼座"
)
