package com.pandora6ix.app.ui.summary

import com.pandora6ix.app.data.*
import org.junit.Assert.*
import org.junit.Test

class SummaryTableTest {
    private val design = TagDto(id = "t1", name = "设计", colorHex = "#DAE9F9")
    private val build = TagDto(id = "t2", name = "施工", colorHex = "#EEE4F7")

    private fun task(id: String, primary: TagDto? = null, vararg extra: TagDto) =
        TaskDto(id = id, primaryTagId = primary?.id, tags = listOfNotNull(primary) + extra)

    @Test fun tagsIncludeSecondaryTagsAndShowPrimaryFirst() {
        val task = task("a", design, build).copy(tags = listOf(build, design))
        assertEquals(listOf(design, build), summaryTags(task))
        assertEquals(listOf(design), summaryTags(task("b", null, design)))
        assertEquals(listOf("无标签"), SummaryTableRow(task("c"), { it }, null).values(SummaryColumn.TAG))
    }

    @Test fun combinedFiltersMatchSecondaryTagAndDisplayedApprovalStatus() {
        val row = SummaryTableRow(task("a", design, build).copy(reviewStatus = "PENDING"), { it }, null)
        val filters = mapOf(SummaryColumn.TAG to "施工", SummaryColumn.STATUS to "待审批")
        assertTrue(row.matches(filters))
        assertFalse(row.matches(filters + (SummaryColumn.STATUS to "已完成")))
        assertTrue(row.matches(emptyMap()))
    }

    @Test fun deadlineShowsTimeOnlyForTimedTasks() {
        assertEquals("2026/10/29" to "08:09", summaryDeadline(TaskDto(id = "a", endDate = "2026-10-29", timeKind = "TIMED", endTime = "08:09:00")))
        assertEquals("2026/10/29" to null, summaryDeadline(TaskDto(id = "a", endDate = "2026-10-29", timeKind = "ALL_DAY", endTime = "23:59")))
    }

    @Test fun ownersUseRecipientNamesWithFallback() {
        val task = TaskDto(id = "a", authorizedAssignments = listOf(
            AssignmentDto("1", recipientUserId = "u1", recipientName = "李思思"),
            AssignmentDto("2", recipientUserId = "u2")))
        assertEquals(listOf("李思思", "成员 u2"), summaryOwners(task) { "成员 $it" })
        assertTrue(summaryOwners(TaskDto(id = "b")) { it }.isEmpty())
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
