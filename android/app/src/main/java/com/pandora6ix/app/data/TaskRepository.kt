package com.pandora6ix.app.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import java.net.URLEncoder

/** POST /tasks 请求体：kind ∈ PERSONAL|DELEGATED；PERSONAL 不带 recipientIds（本人锁定） */
@Serializable
data class TaskCreateDto(
    val kind: String,
    val title: String,
    val description: String? = null,
    val priorityLevel: Int,
    // 不给默认值：ApiClient 的 Json 使用 encodeDefaults=false，等于默认值的字段会被省略，导致后端 @NotNull 校验失败
    val timeKind: String,
    val startDate: String,
    val endDate: String,
    val startTime: String? = null,
    val endTime: String? = null,
    val recipientIds: List<String>? = null,
    val tagIds: List<String>? = null,
    val primaryTagId: String? = null,
    val recurrence: RecurrenceRule? = null,
    val companyWide: Boolean = false
)

@Serializable
data class TaskPatchDto(val expectedVersion: Long, val title: String, val description: String?,
    val priorityLevel: Int, val timeKind: String, val startDate: String, val endDate: String,
    val startTime: String?, val endTime: String?, val tagIds: List<String>, val primaryTagId: String?,
    val recipientIds: List<String>? = null, val kind: String? = null, val companyWide: Boolean? = null)
@Serializable
data class TaskScopeDto(val items: List<TaskDto> = emptyList())
@Serializable
data class ScopeVersionDto(val taskId: String, val taskVersion: Long, val assignmentId: String? = null, val assignmentVersion: Long? = null)
@Serializable
data class ScopedEditDto(val scope: String, val task: TaskPatchDto, val versions: List<ScopeVersionDto>)
@Serializable
data class ScopedStatusDto(val scope: String, val workStatus: String, val note: String? = null, val versions: List<ScopeVersionDto>)

fun taskScopeVersions(tasks: List<TaskDto>, ownerId: String?): List<ScopeVersionDto> = tasks.map { task ->
    val assignment = task.authorizedAssignments.firstOrNull { it.recipientUserId == ownerId }
    ScopeVersionDto(task.id, task.version, assignment?.id, assignment?.version)
}

/** 接收人候选（管理范围内真实用户） */
@Serializable
data class RecipientDto(
    val id: String,
    val displayName: String = "",
    val orgUnitId: String? = null,
    val jobTitle: String? = null,
    val organizationName: String? = null,
    val departmentId: String? = null,
    val departmentName: String? = null,
    val relationship: String = ""
)

@Serializable
data class RecipientPageDto(
    val items: List<RecipientDto> = emptyList(),
    val nextCursor: String? = null,
    val moreUrl: String? = null
)

/** 审批记录中的人员：id + 展示名 + 职位（展示名可能为空，客户端回退显示 ID 后四位） */
@Serializable
data class ReviewActorDto(
    val id: String = "",
    val displayName: String? = null,
    val jobTitle: String? = null
)

/** 审批记录：submittedSnapshot 为提交时不可变内容（字段与 TaskCreateDto 对应，旧快照可能含历史字段） */
@Serializable
data class TaskReviewDto(
    val id: String,
    val version: Long = 0,
    val reviewId: String = "",
    val taskId: String = "",
    val reviewerUserId: String = "",
    val submittedByUserId: String = "",
    /** 请求人（提交人）展示名 */
    val submittedByName: String? = null,
    /** 请求人职位 */
    val submittedByJobTitle: String? = null,
    /** 审批人展示名 */
    val reviewerName: String? = null,
    /** 审批人职位 */
    val reviewerJobTitle: String? = null,
    /** 执行人（任务当前执行关系） */
    val recipients: List<ReviewActorDto> = emptyList(),
    val taskVersion: Long = 0,
    val currentTaskVersion: Long = 0,
    val attemptNo: Long = 0,
    val submittedAt: String? = null,
    val submittedSnapshot: JsonObject? = null,
    val reviewKind: String = "CREATE",
    val decision: String = "PENDING",
    val reviewStatus: String = "PENDING",
    val decisionNote: String? = null,
    val decidedAt: String? = null,
    val isCurrent: Boolean = false,
    /** 仅当前待审且权限/人员范围仍有效时为 [APPROVE, REJECT]；已决定或旧 attempt 为空 */
    val allowedActions: List<String> = emptyList()
)

@Serializable
data class TaskReviewPageDto(
    val items: List<TaskReviewDto> = emptyList(),
    val nextCursor: String? = null,
    val moreUrl: String? = null
)

/** POST /task-reviews/{id}/decision 请求体：expectedVersion 必填，decision ∈ APPROVE|REJECT */
@Serializable
data class ReviewDecisionDto(
    val expectedVersion: Long,
    val decision: String,
    val comment: String? = null
)

/** 决定提交结果：最新 review 状态 + 决定后的任务（批准后进入日历） */
@Serializable
data class ReviewDecisionResultDto(
    val review: TaskReviewDto? = null,
    val task: TaskDto? = null
)

/** POST /task-assignments/{id}/progress-records 请求体：expectedVersion 用执行关系版本；100% 仍为 ACTIVE，不等于完成 */
@Serializable
data class ProgressDto(
    val expectedVersion: Long,
    val progressPercent: Int,
    val note: String? = null
)

/** 进度记录结果：新记录 + 更新后的执行关系（版本已递增，后续进度/完成须用它的新版本） */
@Serializable
data class ProgressResultDto(
    val record: ProgressRecordDto? = null,
    val assignment: AssignmentDto? = null
)

/** POST /tasks/{id}/complete 请求体（个人自完成）：expectedAssignmentVersion 用本人执行关系版本 */
@Serializable
data class CompleteDto(val expectedAssignmentVersion: Long)

/** POST /task-assignments/{id}/completion-requests 请求体（派发申请验收）：expectedVersion 用执行关系版本 */
@Serializable
data class CompletionRequestCreateDto(val expectedVersion: Long, val note: String? = null)

/** POST /task-completion-requests/{id}/decision 请求体：ACCEPT|REJECT（区别于审批的 APPROVE|REJECT）；expectedVersion 用验收申请版本 */
@Serializable
data class CompletionDecisionDto(val expectedVersion: Long, val decision: String, val comment: String? = null)

/** 验收结果：申请视图 + 执行关系（COMPLETED / AWAITING_ACCEPTANCE 等） */
@Serializable
data class CompletionResultDto(
    val request: CompletionRequestDto? = null,
    val assignment: AssignmentDto? = null
)

/**
 * 任务写操作：
 * - 接收人候选 GET /tasks/recipient-candidates?q=&cursor=&limit=
 * - 创建 POST /tasks（201 返回 Task；PENDING 待审，不当作正式派发）
 * - 审批 GET /task-reviews?scope=pending|created（待我审批 / 我提交的）、GET /task-reviews/{id}
 *   决定 POST /task-reviews/{id}/decision：expectedVersion 用 review.version；
 *   409 REVIEW_NOT_PENDING / REVIEW_CONTEXT_CHANGED / VERSION_CONFLICT 需重读最新状态
 * - 进度 POST /task-assignments/{id}/progress-records：expectedVersion 用 assignment.version；
 *   100 仍为 ACTIVE，完成走验收流程；409 后必须从最新任务/返回 assignment 读取新版本
 * - 完成/验收：PERSONAL POST /tasks/{id}/complete（expectedAssignmentVersion）；
 *   DELEGATED 接收人 POST /task-assignments/{id}/completion-requests、
 *   创建者 POST /task-completion-requests/{id}/decision（ACCEPT|REJECT + request.version）
 *   非幂等命令：同一动作重试必须复用同一 Idempotency-Key，新动作用新键
 */
class TaskRepository(private val api: ApiClient) {

    /** Bounded candidate page for team browsing; the server enforces management scope. */
    suspend fun recipientCandidatePage(q: String? = null, cursor: String? = null, limit: Int = 20): RecipientPageDto {
        val query = buildString {
            append("/tasks/recipient-candidates?limit=").append(limit.coerceIn(1, 100))
            q?.takeIf { it.isNotBlank() }?.let { append("&q=").append(URLEncoder.encode(it, "UTF-8")) }
            cursor?.let { append("&cursor=").append(URLEncoder.encode(it, "UTF-8")) }
        }
        return api.get(query, RecipientPageDto.serializer())
    }

    suspend fun recipientCandidates(q: String? = null, limit: Int = 50): RecipientPageDto {
        val values = mutableListOf<RecipientDto>()
        val seen = mutableSetOf<String>()
        var cursor: String? = null
        do {
        val page = recipientCandidatePage(q, cursor, limit)
        values.addAll(page.items)
        cursor = page.nextCursor
        check(cursor == null || seen.add(cursor)) { "Repeated recipient cursor" }
        } while (cursor != null)
        return RecipientPageDto(items = values)
    }

    suspend fun create(command: TaskCreateDto, idempotencyKey: String): TaskDto {
        val body = api.json.encodeToString(TaskCreateDto.serializer(), command)
        return api.postIdempotent(if (command.recurrence == null) "/tasks" else "/tasks/recurring", body, idempotencyKey, TaskDto.serializer())
    }

    /** 审批队列：scope=pending（待我审批）| created（我提交的申请） */
    suspend fun reviews(scope: String = "pending", cursor: String? = null, limit: Int = 20): TaskReviewPageDto {
        val query = buildString {
            append("/task-reviews?scope=").append(URLEncoder.encode(scope, "UTF-8"))
            cursor?.let { append("&cursor=").append(URLEncoder.encode(it, "UTF-8")) }
            append("&limit=").append(limit.coerceIn(1, 100))
        }
        return api.get(query, TaskReviewPageDto.serializer())
    }

    /** 单条审批（含快照详情） */
    suspend fun review(id: String): TaskReviewDto =
        api.get("/task-reviews/${URLEncoder.encode(id, "UTF-8")}", TaskReviewDto.serializer())

    /** 任务详情（含 authorizedAssignments/progressRecords/allowedActions 最新版本） */
    suspend fun task(id: String): TaskDto =
        api.get("/tasks/${URLEncoder.encode(id, "UTF-8")}", TaskDto.serializer())

    suspend fun scopeSnapshot(id: String): TaskScopeDto = api.get("/tasks/${URLEncoder.encode(id, "UTF-8")}/scope", TaskScopeDto.serializer())
    suspend fun scopedEdit(id: String, command: ScopedEditDto, key: String): TaskDto = api.postIdempotent(
        "/tasks/${URLEncoder.encode(id, "UTF-8")}/scoped-edit", api.json.encodeToString(ScopedEditDto.serializer(), command), key, TaskDto.serializer())
    suspend fun scopedStatus(id: String, command: ScopedStatusDto, key: String): TaskDto = api.postIdempotent(
        "/tasks/${URLEncoder.encode(id, "UTF-8")}/scoped-status", api.json.encodeToString(ScopedStatusDto.serializer(), command), key, TaskDto.serializer())

    /** 记录执行进度：percent ∈ 0..100；expectedVersion 为该执行关系当前版本 */
    suspend fun recordProgress(
        assignmentId: String,
        expectedVersion: Long,
        progressPercent: Int,
        note: String? = null,
        idempotencyKey: String
    ): ProgressResultDto {
        val body = api.json.encodeToString(
            ProgressDto.serializer(),
            ProgressDto(expectedVersion = expectedVersion, progressPercent = progressPercent.coerceIn(0, 100), note = note)
        )
        return api.postIdempotent(
            "/task-assignments/${URLEncoder.encode(assignmentId, "UTF-8")}/progress-records",
            body, idempotencyKey, ProgressResultDto.serializer()
        )
    }

    /** 个人任务自完成：expectedAssignmentVersion 为本人执行关系版本；成功返回 COMPLETED 的执行关系 */
    suspend fun completePersonalTask(taskId: String, expectedAssignmentVersion: Long, idempotencyKey: String): AssignmentDto {
        val body = api.json.encodeToString(CompleteDto.serializer(), CompleteDto(expectedAssignmentVersion))
        return api.postIdempotent(
            "/tasks/${URLEncoder.encode(taskId, "UTF-8")}/complete",
            body, idempotencyKey, AssignmentDto.serializer()
        )
    }

    /** 派发任务申请验收：expectedVersion 为执行关系版本；成功后执行关系进入 AWAITING_ACCEPTANCE */
    suspend fun requestCompletion(
        assignmentId: String,
        expectedVersion: Long,
        note: String? = null,
        idempotencyKey: String
    ): CompletionResultDto {
        val body = api.json.encodeToString(CompletionRequestCreateDto.serializer(), CompletionRequestCreateDto(expectedVersion, note))
        return api.postIdempotent(
            "/task-assignments/${URLEncoder.encode(assignmentId, "UTF-8")}/completion-requests",
            body, idempotencyKey, CompletionResultDto.serializer()
        )
    }

    /** 创建者验收决定：decision ∈ ACCEPT|REJECT；expectedVersion 为验收申请（CompletionRequestView）版本 */
    suspend fun decideCompletion(
        requestId: String,
        decision: String,
        expectedVersion: Long,
        comment: String? = null,
        idempotencyKey: String
    ): CompletionResultDto {
        val body = api.json.encodeToString(
            CompletionDecisionDto.serializer(),
            CompletionDecisionDto(expectedVersion = expectedVersion, decision = decision, comment = comment)
        )
        return api.postIdempotent(
            "/task-completion-requests/${URLEncoder.encode(requestId, "UTF-8")}/decision",
            body, idempotencyKey, CompletionResultDto.serializer()
        )
    }

    /** 提交审批决定：decision ∈ APPROVE|REJECT；expectedVersion 必须为该 review 当前 version */
    suspend fun decide(
        reviewId: String,
        decision: String,
        expectedVersion: Long,
        comment: String? = null,
        idempotencyKey: String
    ): ReviewDecisionResultDto {
        val body = api.json.encodeToString(
            ReviewDecisionDto.serializer(),
            ReviewDecisionDto(expectedVersion = expectedVersion, decision = decision, comment = comment)
        )
        return api.postIdempotent(
            "/task-reviews/${URLEncoder.encode(reviewId, "UTF-8")}/decision",
            body, idempotencyKey, ReviewDecisionResultDto.serializer()
        )
    }
}
