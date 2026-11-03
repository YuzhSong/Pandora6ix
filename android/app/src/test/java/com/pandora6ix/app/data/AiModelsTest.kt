package com.pandora6ix.app.data

import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class AiModelsTest {
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false; encodeDefaults = false }

    @Test fun decodesBackendEnvelopeAndToleratesMissingModelFields() {
        val text = """
            {"feature":"career-map","aiGenerated":false,"quotaExceeded":true,"disclaimer":"仅供参考","generatedAt":"2026-11-03T03:00:00Z",
             "data":{"empty":false,"stats":{"windowDays":30,"journalCount":3,"entryCount":5,"taskCount":2,"completedTaskCount":1},
                     "topTags":[{"name":"Android开发","count":3}],"label":"成长探索者","futureField":1,
                     "abilities":[{"name":"开发","level":"MEDIUM","evidence":"日志"}],
                     "routes":[{"key":"A","title":"专业深耕","description":"d","actions":["一"]}]}}
        """.trimIndent()
        val result = json.decodeFromString(AiResultDto.serializer(CareerMapDto.serializer()), text)
        assertTrue(result.quotaExceeded)
        assertFalse(result.aiGenerated)
        assertEquals("成长探索者", result.data.label)
        assertEquals(3L, result.data.topTags.single().count)
        assertEquals("", result.data.summary)
        assertEquals("有所体现", abilityLevelLabel(result.data.abilities.single().level))
    }

    @Test fun tarotBodyOmitsBlankQuestion() {
        assertEquals("{}", json.encodeToString(TarotDrawBody.serializer(), TarotDrawBody(null)))
        assertEquals("""{"question":"好运吗"}""", json.encodeToString(TarotDrawBody.serializer(), TarotDrawBody("好运吗")))
    }

    @Test fun personalityResultRoundTripsForLocalCache() {
        val result = AiResultDto("personality", true, false, "d", "t",
            PersonalityDto(ArchetypeDto("DEADLINE", "DDL 战神", "DDL", "🔥", "截止日期就是我的超能力"), listOf(DimensionDto("focus", "专注力", 80))))
        val serializer = AiResultDto.serializer(PersonalityDto.serializer())
        assertEquals(result, json.decodeFromString(serializer, json.encodeToString(serializer, result)))
    }

    @Test fun abilityLevelsMapToOrderedRatios() {
        val ratios = listOf("HIGH", "MEDIUM", "LOW", "INSUFFICIENT", "unknown").map(::abilityLevelRatio)
        assertEquals(ratios.sortedDescending(), ratios)
        assertEquals("暂无充分记录", abilityLevelLabel("unknown"))
        assertEquals(12, Zodiacs.size)
    }

    @Test fun preferencesAreScopedPerAccount() {
        val disk = mutableMapOf<String, String?>()
        val prefs = AiPreferences({ disk[it] }, { key, value -> disk[key] = value })
        assertFalse(prefs.dataNoticeAccepted("a"))
        prefs.acceptDataNotice("a")
        prefs.savePersonality("a", "{}")
        assertTrue(prefs.dataNoticeAccepted("a"))
        assertFalse(prefs.dataNoticeAccepted("b"))
        assertEquals("{}", prefs.personality("a"))
        assertNull(prefs.personality("b"))
    }
}
