package com.pandora6ix.app.ui.logs

import com.pandora6ix.app.data.DailyItem
import com.pandora6ix.app.data.JournalDraftEntryDto
import org.junit.Assert.assertEquals
import org.junit.Test

class JournalDefaultsTest {
    private val date = "2026-11-06"
    private val todos = listOf(
        DailyItem("one", date, "整理资料", done = true),
        DailyItem("other", "2026-11-05", "昨天的事项"),
        DailyItem("two", date, "联系客户")
    )

    @Test fun includesBothCompletionStatesInStoredOrderForSelectedDate() {
        val result = initialJournalEntries(date, todos)
        assertEquals(listOf(JournalDraftEntryDto(null, "- 整理资料\n- 联系客户\n\n")), result)
        assertEquals(true, todos.first().done)
        assertEquals(false, todos.last().done)
    }

    @Test fun importsAllTodosBeyondTheCalendarPreviewLimit() {
        val items = (1..5).map { DailyItem("$it", date, "事项$it", done = it % 2 == 0) }
        assertEquals("- 事项1\n- 事项2\n- 事项3\n- 事项4\n- 事项5\n\n", initialJournalEntries(date, items).single().content)
    }

    @Test fun noMatchingTodosKeepsOrdinaryEmptyBody() {
        assertEquals(listOf(JournalDraftEntryDto(null, "")), initialJournalEntries("2026-11-07", todos))
    }

    @Test fun multilineTodoKeepsMarkdownContinuationInsideItsBullet() {
        val items = listOf(DailyItem("multi", date, "首行\r\n续行\r末行"))
        assertEquals("- 首行\n  续行\n  末行\n\n", initialJournalEntries(date, items).single().content)
    }

    @Test fun editedDraftPreservesDeletedBulletsAndAdditionalPersonalNotes() {
        val edited = listOf(JournalDraftEntryDto(null, "- 联系客户\n\n另外完成了临时会议记录。"))
        val existing = listOf(JournalDraftEntryDto("published", "已发布正文"))
        assertEquals(edited, initialJournalEntries(date, todos, draftEntries = edited, existingEntries = existing))
        assertEquals(edited, initialJournalEntries(date, todos, draftEntries = edited))
    }

    @Test fun intentionallyClearedDraftDoesNotRegenerateTemplate() {
        val cleared = listOf(JournalDraftEntryDto(null, ""))
        assertEquals(cleared, initialJournalEntries(date, todos, draftEntries = cleared))
        assertEquals(emptyList<JournalDraftEntryDto>(), initialJournalEntries(date, todos, draftEntries = emptyList()))
    }

    @Test fun publishedJournalKeepsEntryIdsAndBodyWithoutTodoInjection() {
        val existing = listOf(JournalDraftEntryDto("entry-one", "原有正文"), JournalDraftEntryDto("entry-two", "补充内容"))
        assertEquals(existing, initialJournalEntries(date, todos, existingEntries = existing))
    }
}
