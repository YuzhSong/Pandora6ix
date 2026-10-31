package com.pandora6ix.app.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.*
import org.junit.Test

class TaskResubmissionTest {
    private fun rejected() = TaskReviewDto(id = "review-old", submittedByUserId = "creator", decision = "REJECTED",
        submittedSnapshot = Json.parseToJsonElement("""{
            "kind":"DELEGATED","title":"Original title","description":"Original description",
            "priorityLevel":4,"timeKind":"TIMED","startDate":"2026-10-30","endDate":"2026-10-31",
            "startTime":"09:15","endTime":"17:30","recipientIds":["member-a","member-b"],
            "tagIds":["secondary","primary"],"primaryTagId":"primary","companyWide":false,
            "historicalField":"ignored"
        }""").jsonObject)

    @Test fun immutableSnapshotPrefillsEveryNewTaskFieldAndPrimaryTagOrder() {
        val tags = listOf(TagDto(id="secondary", name="Secondary"), TagDto(id="primary", name="Primary"))
        val review = rejected()
        val draft = rejectedTaskDraft(review,"creator",tags)
        assertEquals("Original title",draft.title)
        assertEquals("Original description",draft.note)
        assertEquals(4,draft.priority)
        assertEquals("2026-10-30",draft.startKey)
        assertEquals("2026-10-31",draft.endKey)
        assertEquals(555,draft.startMinute)
        assertEquals(1050,draft.endMinute)
        assertEquals(listOf("member-a","member-b"),draft.recipientIds)
        assertEquals(listOf("Primary","Secondary"),draft.tags)
        assertFalse(draft.allDay)
        assertEquals("REJECTED",review.decision)
        assertEquals("review-old",review.id)
    }

    @Test fun onlyOriginalRejectedCreationCanPrefill() {
        for ((review, user) in listOf(rejected() to "other", rejected().copy(decision="APPROVED") to "creator",
            rejected().copy(reviewKind="EDIT") to "creator", rejected().copy(submittedSnapshot=null) to "creator")) {
            assertTrue(runCatching { rejectedTaskDraft(review,user,emptyList()) }.isFailure)
        }
    }
}
