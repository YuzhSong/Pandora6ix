package com.pandora6ix.app.data

import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class JournalCommentModelsTest {
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false; encodeDefaults = false }

    @Test fun journalDetailCarriesAuthorDepartment() {
        val journal = json.decodeFromString(JournalDto.serializer(), """
            {"id":"j1","authorUserId":"u1","author":{"id":"u1","displayName":"王小明","jobTitle":"Android 工程师","organizationName":"安卓组","departmentName":"研发部"}}
        """.trimIndent())
        assertEquals("研发部", journal.author?.departmentName)
        assertEquals("王小明", journal.author?.displayName)
        // 列表接口不返回 author，仍可解析
        assertNull(json.decodeFromString(JournalDto.serializer(), """{"id":"j2"}""").author)
    }

    @Test fun commentPageAndBody() {
        val page = json.decodeFromString(JournalCommentPageDto.serializer(), """
            {"items":[{"id":"c1","journalId":"j1","authorUserId":"lead","authorName":"陈组长","content":"不错","createdAt":"2026-11-03T08:00:00Z","allowedActions":["DELETE"]}],"canComment":true}
        """.trimIndent())
        assertTrue(page.canComment)
        assertEquals(listOf("DELETE"), page.items.single().allowedActions)
        assertEquals("""{"content":"加油"}""", json.encodeToString(JournalCommentBody.serializer(), JournalCommentBody("加油")))
    }
}
