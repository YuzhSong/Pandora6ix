package com.pandora6ix.app.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 用服务端真实返回的原始响应验证「完成验收」队列解析：
 * 响应取自 GET /task-completion-requests?scope=pending&limit=50（创始人派发 3 人任务、
 * 一名执行人提交完成申请后）。Json 配置必须与 [ApiClient.json] 保持一致。
 *
 * 回归点：服务端把可空列输出为 null（taskId / decidedByUserId），
 * decodeFromJsonElement 不会把 null 当缺省，因此 DTO 必须可空、Json 必须开启 coerceInputValues。
 */
class CompletionQueueParseTest {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = false
        coerceInputValues = true
    }

    private fun dataOf(body: String) = json.parseToJsonElement(body).jsonObject.getValue("data")

    @Test fun parsesLivePendingQueueResponse() {
        val body = """
            {"data":{"items":[{"id":"be0cbfd1-06a2-447d-9c80-603ef6bc892d",
            "assignmentId":"561d8fae-90b3-49c2-8be4-f96f3d2fa272",
            "requestedByUserId":"b636a707-a9a9-4375-818e-00a436eac3c1",
            "submittedNote":"Done",
            "requestedAt":"2026-11-12T07:08:24.586321Z",
            "decision":"PENDING",
            "decidedByUserId":null,
            "decisionNote":null,
            "decidedAt":null,
            "version":0,
            "taskTitle":"Task",
            "requesterName":"employee",
            "taskId":null}],"nextCursor":null},
            "requestId":"1f6e169e-92aa-48d0-8697-a53dee5e10f7"}
        """.trimIndent()
        val page = json.decodeFromJsonElement(CompletionRequestPageDto.serializer(), dataOf(body))
        assertEquals(1, page.items.size)
        assertEquals(null, page.nextCursor)
        val item = page.items.first()
        assertEquals("be0cbfd1-06a2-447d-9c80-603ef6bc892d", item.id)
        assertEquals("Task", item.taskTitle)
        assertEquals("PENDING", item.decision)
        assertNull(item.taskId)
        assertNull(item.decidedByUserId)
        assertNull(item.decidedAt)
        assertEquals(0L, item.version)
    }

    @Test fun parsesHistoricalAcceptedRequestWithTaskId() {
        val body = """
            {"data":{"items":[{"id":"r1","assignmentId":"a1","requestedByUserId":"u1",
            "requesterName":"高远","submittedNote":null,"requestedAt":"2026-11-10T09:30:00Z",
            "decision":"ACCEPTED","decidedByUserId":"u2","decisionNote":"通过",
            "decidedAt":"2026-11-10T10:00:00Z","version":1,"taskTitle":"周末全公司团建",
            "taskId":"t1"}],"nextCursor":"c2"},"requestId":"rid"}
        """.trimIndent()
        val page = json.decodeFromJsonElement(CompletionRequestPageDto.serializer(), dataOf(body))
        val item = page.items.first()
        assertEquals("t1", item.taskId)
        assertEquals("ACCEPTED", item.decision)
        assertEquals("u2", item.decidedByUserId)
        assertEquals("c2", page.nextCursor)
    }
}
