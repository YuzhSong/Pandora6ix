package com.pandora6ix.app.ui.summary

import com.pandora6ix.app.data.*
import org.junit.Assert.*
import org.junit.Test

class SummaryTableTest {
    private val design = TagDto(id = "t1", name = "设计")
    private val build = TagDto(id = "t2", name = "施工")

    private fun task(id: String, primary: TagDto? = null, vararg extra: TagDto) =
        TaskDto(id = id, primaryTagId = primary?.id, tags = listOfNotNull(primary) + extra)

    @Test fun groupsFollowFirstAppearanceAndUngroupedGoLast() {
        val groups = summaryTableGroups(listOf(task("a"), task("b", build), task("c", design, build), task("d", build)))
        assertEquals(listOf("施工", "设计", "未分组"), groups.map { it.first })
        assertEquals(listOf("b", "d"), groups[0].second.map { it.id })
        assertEquals(listOf("a"), groups[2].second.map { it.id })
        assertTrue(summaryTableGroups(listOf(task("x", design))).none { it.first == "未分组" })
    }

    @Test fun nonPrimaryTagDoesNotGroup() {
        assertEquals("未分组", summaryTableGroups(listOf(task("a", null, design))).single().first)
    }

    @Test fun deadlineShowsTimeOnlyForTimedTasks() {
        assertEquals("2026/10/29" to "08:09", summaryDeadline(TaskDto(id = "a", endDate = "2026-10-29", timeKind = "TIMED", endTime = "08:09:00")))
        assertEquals("2026/10/29" to null, summaryDeadline(TaskDto(id = "a", endDate = "2026-10-29", timeKind = "ALL_DAY", endTime = "23:59")))
    }

    @Test fun progressPrefersNoteThenAverageThenDash() {
        val assignments = listOf(AssignmentDto("1", progressPercent = 40), AssignmentDto("2", progressPercent = 80))
        assertEquals("828 已到货 设计定稿", summaryProgressNote(TaskDto(id = "a", latestProgressNote = " 828 已到货\n设计定稿 ", authorizedAssignments = assignments)))
        assertEquals("进度 60%", summaryProgressNote(TaskDto(id = "a", latestProgressNote = "  ", authorizedAssignments = assignments)))
        assertEquals("—", summaryProgressNote(TaskDto(id = "a")))
    }

    @Test fun ownersUseRecipientNamesWithFallback() {
        val task = TaskDto(id = "a", authorizedAssignments = listOf(
            AssignmentDto("1", recipientUserId = "u1", recipientName = "李思思"),
            AssignmentDto("2", recipientUserId = "u2")))
        assertEquals("李思思、成员 u2", summaryOwners(task) { "成员 $it" })
        assertEquals("—", summaryOwners(TaskDto(id = "b")) { it })
    }

    @Test fun journalRangesAreInclusiveServerBounds() {
        val today = "2026-11-10"
        assertNull(journalRangeBounds(JournalRange.ALL, today, null))
        assertEquals(today to today, journalRangeBounds(JournalRange.TODAY, today, null))
        assertEquals("2026-11-04" to today, journalRangeBounds(JournalRange.WEEK, today, null))
        assertEquals("2026-11-01" to today, journalRangeBounds(JournalRange.MONTH, today, null))
        assertEquals("2026-11-05" to "2026-11-05", journalRangeBounds(JournalRange.DAY, today, "2026-11-05"))
        assertEquals("2026-03-01" to "2026-03-01", journalRangeBounds(JournalRange.WEEK, "2026-03-07", null).let { it!!.first to it.first })
    }
}
