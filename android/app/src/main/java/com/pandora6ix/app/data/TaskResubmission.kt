package com.pandora6ix.app.data

import java.time.LocalDate
import java.time.LocalTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement

private val resubmissionJson = Json { ignoreUnknownKeys = true }

/** Builds a new command draft from the immutable rejected publication, never the live task. */
fun rejectedTaskDraft(review: TaskReviewDto, userId: String, tags: List<TagDto>): TaskDraftDto {
    require(review.decision == "REJECTED" && review.reviewKind == "CREATE" && review.submittedByUserId == userId) {
        "只能重新发布本人被驳回的任务申请"
    }
    val snapshot = requireNotNull(review.submittedSnapshot) { "原申请内容不完整，无法预填" }
    val command = resubmissionJson.decodeFromJsonElement<TaskCreateDto>(snapshot)
    require(command.kind in listOf("PERSONAL", "DELEGATED")) { "原任务类型不可用" }
    val start = LocalDate.parse(command.startDate)
    val end = LocalDate.parse(command.endDate)
    require(!end.isBefore(start)) { "原任务日期不完整" }
    val allDay = command.timeKind == "ALL_DAY"
    fun minute(value: String?) = LocalTime.parse(requireNotNull(value)).let { it.hour * 60 + it.minute }
    val orderedTags = command.tagIds.orEmpty().sortedBy { if (it == command.primaryTagId) 0 else 1 }
    return TaskDraftDto(command.kind, command.title, command.description.orEmpty(), allDay,
        command.startDate, command.endDate, if (allDay) 0 else minute(command.startTime),
        if (allDay) 60 else minute(command.endTime), command.priorityLevel,
        orderedTags.mapNotNull { id -> tags.firstOrNull { it.id == id }?.name },
        command.recipientIds.orEmpty(), command.recurrence, companyWide = command.companyWide)
}
