package com.pandora6ix.app.data

import java.net.URLEncoder

/** AI 探索馆：生成接口走长超时通道；所有结果只读，不修改业务数据 */
class AiRepository(private val api: ApiClient) {

    suspend fun fortune(zodiac: String?): AiResultDto<FortuneDto> =
        api.getSlow("/ai/fortune" + (zodiac?.let { "?zodiac=" + URLEncoder.encode(it, "UTF-8") } ?: ""),
            AiResultDto.serializer(FortuneDto.serializer()))

    suspend fun personalityQuestions(): PersonalityQuestionsDto =
        api.get("/ai/personality/questions", PersonalityQuestionsDto.serializer())

    suspend fun personality(optionIds: List<String>): AiResultDto<PersonalityDto> =
        api.postSlow("/ai/personality", api.json.encodeToString(PersonalityAnswersBody.serializer(), PersonalityAnswersBody(optionIds)),
            AiResultDto.serializer(PersonalityDto.serializer()))

    suspend fun tarot(question: String): AiResultDto<TarotDto> =
        api.postSlow("/ai/tarot", api.json.encodeToString(TarotDrawBody.serializer(), TarotDrawBody(question.trim().ifBlank { null })),
            AiResultDto.serializer(TarotDto.serializer()))

    suspend fun careerMap(): AiResultDto<CareerMapDto> =
        api.postSlow("/ai/career-map", null, AiResultDto.serializer(CareerMapDto.serializer()))

    suspend fun companyOutlook(): AiResultDto<CompanyOutlookDto> =
        api.postSlow("/ai/company-outlook", null, AiResultDto.serializer(CompanyOutlookDto.serializer()))

    fun encodePersonality(result: AiResultDto<PersonalityDto>): String =
        api.json.encodeToString(AiResultDto.serializer(PersonalityDto.serializer()), result)

    fun decodePersonality(text: String?): AiResultDto<PersonalityDto>? =
        text?.let { runCatching { api.json.decodeFromString(AiResultDto.serializer(PersonalityDto.serializer()), it) }.getOrNull() }
}

/**
 * 本机按账号保存：最近一次人格结果、职业星图/公司下一站的数据说明是否已确认。
 * 读写函数注入，便于单元测试；不同账号互不影响。
 */
class AiPreferences(private val read: (String) -> String?, private val write: (String, String?) -> Unit) {
    fun personality(owner: String): String? = read("personality_$owner")
    fun savePersonality(owner: String, value: String?) = write("personality_$owner", value)
    fun dataNoticeAccepted(owner: String): Boolean = read("data_notice_$owner") == "1"
    fun acceptDataNotice(owner: String) = write("data_notice_$owner", "1")
}
