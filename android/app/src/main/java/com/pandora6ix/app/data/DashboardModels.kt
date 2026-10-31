package com.pandora6ix.app.data

import kotlinx.serialization.Serializable

/*
 * 导图/任务/日报读侧模型：字段与 backend/openapi.json 逐一对应。
 * 所有可选字段给默认值，配合 Json.ignoreUnknownKeys 保证前向兼容。
 */

@Serializable
data class TagDto(
    val id: String,
    val version: Long = 0,
    val name: String = "",
    val colorHex: String = "",
    val creatorUserId: String = ""
)

@Serializable
data class CompanyItemDto(
    val id: String,
    val version: Long = 0,
    val itemId: String = "",
    val publisherUserId: String = "",
    val title: String = "",
    val content: String = "",
    val displayOrder: Long = 0,
    val createdAt: String? = null
)

@Serializable
data class AssignmentDto(
    val id: String,
    val version: Long = 0,
    val taskId: String = "",
    val recipientUserId: String = "",
    val executionStatus: String = "",
    /** Optional current backend field; executionStatus continues to drive completion authorization. */
    val workStatus: String? = null,
    val progressPercent: Int = 0,
    val activatedAt: String? = null,
    val completionSource: String? = null,
    val completedByUserId: String? = null,
    val completedAt: String? = null,
    val allowedActions: List<String> = emptyList()
)

@Serializable
data class ProgressRecordDto(
    val id: String,
    val assignmentId: String = "",
    val actorUserId: String = "",
    val progressPercent: Int = 0,
    val note: String? = null,
    val recordedAt: String? = null
)

@Serializable
data class CompletionRequestDto(
    val id: String,
    val version: Long = 0,
    val assignmentId: String = "",
    val requestedByUserId: String = "",
    val submittedNote: String? = null,
    val requestedAt: String? = null,
    val decision: String = "",
    val decidedByUserId: String? = null,
    val decisionNote: String? = null,
    val decidedAt: String? = null
)

@Serializable
data class TaskDto(
    val id: String,
    val version: Long = 0,
    val taskId: String = "",
    val creatorUserId: String = "",
    val kind: String = "",
    val title: String = "",
    val description: String? = null,
    val priorityLevel: Int = 3,
    val startDate: String = "",
    val endDate: String = "",
    val timeKind: String = "ALL_DAY",
    val startTime: String? = null,
    val endTime: String? = null,
    val reviewStatus: String = "",
    val primaryTagId: String? = null,
    val tags: List<TagDto> = emptyList(),
    val tagIds: List<String> = emptyList(),
    val displayColor: String? = null,
    val createdAt: String? = null,
    val authorizedAssignments: List<AssignmentDto> = emptyList(),
    val allowedActions: List<String> = emptyList(),
    val reviewId: String? = null,
    val progressRecords: List<ProgressRecordDto> = emptyList(),
    val completionRequests: List<CompletionRequestDto> = emptyList(),
    val seriesId: String? = null,
    val recurrenceLabel: String? = null,
    val occurrenceCount: Int = 1,
    val occurrenceDate: String? = null,
    val companyWide: Boolean = false
)

@Serializable
data class WorkEntryDto(
    val id: String,
    val version: Long = 0,
    val entryId: String = "",
    val journalId: String = "",
    val workLogId: String = "",
    val workDate: String = "",
    val authorUserId: String = "",
    val clientEntryKey: String = "",
    val content: String = "",
    val displayOrder: Long = 0,
    val recordedAt: String? = null,
    val eligible: Boolean = false
)

/** 整篇日报（/work-logs/{journalId}） */
@Serializable
data class JournalDto(
    val id: String,
    val version: Long = 0,
    val journalId: String = "",
    val authorUserId: String = "",
    val workDate: String = "",
    val publishedAt: String? = null,
    val businessToday: String? = null,
    val allowedActions: List<String> = emptyList(),
    val primaryTagId: String? = null,
    val entries: List<WorkEntryDto> = emptyList(),
    val tags: List<TagDto> = emptyList(),
    val tagIds: List<String> = emptyList()
)

@Serializable
data class StatisticsDto(
    val total: Long = 0,
    val completed: Long = 0,
    val uncompleted: Long = 0,
    val basis: String = ""
)

/** 个人重要事项当前选择（dashboard.personalImportantEntries） */
@Serializable
data class ImportantSelectionDto(
    val entries: List<WorkEntryDto> = emptyList(),
    val hasManualSelection: Boolean = false,
    val requiresSelection: Boolean = false,
    val candidateCount: Long = 0,
    val version: Long = 0
)

@Serializable
data class CompanyItemPageDto(
    val items: List<CompanyItemDto> = emptyList(),
    val nextCursor: String? = null,
    val moreUrl: String? = null
)

@Serializable
data class TaskPageDto(
    val items: List<TaskDto> = emptyList(),
    val nextCursor: String? = null,
    val moreUrl: String? = null
)

@Serializable
data class WorkEntryPageDto(
    val items: List<WorkEntryDto> = emptyList(),
    val nextCursor: String? = null,
    val moreUrl: String? = null
)

@Serializable
data class DashboardDto(
    val companyImportantItems: CompanyItemPageDto = CompanyItemPageDto(),
    val companyTasks: TaskPageDto = TaskPageDto(),
    val personalImportantEntries: ImportantSelectionDto = ImportantSelectionDto(),
    val workEntries: WorkEntryPageDto = WorkEntryPageDto(),
    val statistics: StatisticsDto = StatisticsDto()
)
