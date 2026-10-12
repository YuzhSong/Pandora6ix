package com.pandora6ix.app.mock

import org.junit.Assert.*
import org.junit.Test

class PersonalImportantItemsTest {
    private val today = DemoDate(2026, 10, 11)
    private fun log(id: String, date: DemoDate = today, text: String = "Report") =
        WorkLog(id, text, MockData.userName, date, "18:00")

    @Test fun editingKeepsSourceExcludedAndCannotSelectItAgain() {
        val source = log("one")
        val items = mutableListOf(PersonalImportantItem(source.id, source.content))
        val drafts = mutableMapOf<String, String>()
        editPersonalItem(items, drafts, 0, "Edited")
        assertTrue(personalItemCandidates(listOf(source), items, today).isEmpty())
        assertFalse(selectPersonalItem(items, drafts, source))
        assertEquals(1, items.size)
    }

    @Test fun removingAndReselectingRestoresEditedTextBySourceId() {
        val source = log("one")
        val items = mutableListOf(PersonalImportantItem(source.id, source.content))
        val drafts = mutableMapOf<String, String>()
        editPersonalItem(items, drafts, 0, " Edited ")
        items.removeAt(0)
        val candidate = personalItemCandidates(listOf(source), items, today).single()
        assertTrue(selectPersonalItem(items, drafts, candidate))
        assertEquals(PersonalImportantItem(source.id, "Edited"), items.single())
        assertEquals("Report", source.content)
    }

    @Test fun equalContentDoesNotMergeDistinctSourceLogs() {
        val sources = listOf(log("one"), log("two"))
        val items = listOf(PersonalImportantItem("one", "Report"))
        assertEquals(listOf("two"), personalItemCandidates(sources, items, today).map { it.id })
    }

    @Test fun candidatesUseTenInclusiveNaturalDaysAndOnlyCurrentUser() {
        val sources = listOf(log("future", today.plusDays(1)), log("today"), log("oldest", today.plusDays(-9)),
            log("expired", today.plusDays(-10)), log("other").copy(user = "Other"))
        assertEquals(listOf("today", "oldest"), personalItemCandidates(sources, emptyList(), today).map { it.id })
    }

    @Test fun selectionEnforcesTenItemLimitAndReorderingRetainsIdentity() {
        val items = mutableListOf<PersonalImportantItem>()
        repeat(10) { assertTrue(selectPersonalItem(items, emptyMap(), log("$it"))) }
        assertFalse(selectPersonalItem(items, emptyMap(), log("extra")))
        items.add(9, items.removeAt(0))
        assertEquals("0", items.last().sourceLogId)
        assertTrue(personalItemCandidates((0..9).map { log("$it") }, items, today).isEmpty())
    }
}
