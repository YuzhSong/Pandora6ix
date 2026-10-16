package com.pandora6ix.app.data

import com.pandora6ix.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

/** 字段级校验错误（422）：fieldErrors 为 {field, message} 数组 */
@Serializable
data class FieldError(
    val field: String = "",
    val message: String = ""
)

/** 业务错误：携带 HTTP 状态、error.code/message/fieldErrors 与 requestId（供排查，不含敏感信息） */
class ApiException(
    val httpStatus: Int,
    val code: String?,
    override val message: String,
    val fieldErrors: List<FieldError> = emptyList(),
    val requestId: String? = null,
    /** 409 DAILY_JOURNAL_EXISTS 时返回：当日已有日报的 journalId，用于并入同一篇继续补充 */
    val existingJournalId: String? = null
) : IOException(message) {

    /** 面向用户的中文提示：已知错误码优先，其次按 HTTP 状态；不暴露英文原文与状态码 */
    fun friendlyMessage(): String = when (code) {
        "INVALID_CREDENTIALS" -> "企业域、登录名或密码不正确"
        "RESOURCE_NOT_FOUND" -> "内容不存在或已被移除"
        "VERSION_CONFLICT" -> "内容已被修改，请刷新后重试"
        "REVIEW_NOT_PENDING" -> "该申请已被处理，请查看最新结果"
        "REVIEW_CONTEXT_CHANGED" -> "审批环境已变化，请刷新后重试"
        "IDEMPOTENCY_CONFLICT" -> "操作已提交，请勿重复操作"
        "DAILY_JOURNAL_EXISTS" -> "今日日报已存在"
        "WORKFLOW_POLICY_UNCONFIRMED" -> "流程策略未确认，请联系管理员"
        else -> when (httpStatus) {
            400 -> "请求内容有误，请检查后重试"
            401 -> "登录已过期，请重新登录"
            403 -> "没有权限执行此操作"
            404 -> "内容不存在或已被移除"
            409 -> "操作冲突，请刷新后重试"
            422 -> fieldErrors.firstOrNull()?.message ?: "输入内容有误，请检查后重试"
            in 500..599 -> "服务暂时不可用，请稍后重试"
            else -> "操作失败，请稍后重试"
        }
    }
}

/** 会话失效（带 token 的请求返回 401）：调用方应回到登录页，不得自动重试或回退 MockData */
class SessionExpiredException : IOException("会话已过期，请重新登录")

object ApiContract {
    @Serializable
    data class ErrorDetail(
        val code: String? = null,
        val message: String = "",
        val fieldErrors: List<FieldError> = emptyList(),
        val existingJournalId: String? = null
    )

    @Serializable
    data class ErrorBody(val error: ErrorDetail = ErrorDetail())
}

/**
 * 统一 API 客户端：
 * - 基址来自 BuildConfig.API_BASE_URL
 * - 自动附带 Authorization: Bearer <token>
 * - 成功解析 {data, requestId}；错误解析 error.code/message/fieldErrors
 * - 带 token 的请求收到 401：清理会话、回调 onSessionExpired、抛 SessionExpiredException
 *   （登录等未携带 token 的请求 401 按普通业务错误处理，如密码错误）
 */
class ApiClient(private val session: SessionStore) {

    val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = false
    }

    /** 401 会话失效回调（由应用层注册，用于回到登录页） */
    var onSessionExpired: (() -> Unit)? = null

    private val http: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val builder = chain.request().newBuilder()
            session.token?.let { builder.header("Authorization", "Bearer $it") }
            chain.proceed(builder.build())
        }
        .build()

    suspend fun <T> get(path: String, dataSerializer: DeserializationStrategy<T>): T =
        call("GET", path, null, null, dataSerializer)

    suspend fun <T> post(path: String, body: String? = null, dataSerializer: DeserializationStrategy<T>): T =
        call("POST", path, body, null, dataSerializer)

    suspend fun <T> patch(path: String, body: String, dataSerializer: DeserializationStrategy<T>): T =
        call("PATCH", path, body, null, dataSerializer)

    suspend fun <T> put(path: String, body: String, dataSerializer: DeserializationStrategy<T>): T =
        call("PUT", path, body, null, dataSerializer)

    /** 携带 Idempotency-Key 的非幂等命令；同一动作重试必须复用原键，新动作用新键 */
    suspend fun <T> postIdempotent(
        path: String,
        body: String,
        idempotencyKey: String,
        dataSerializer: DeserializationStrategy<T>
    ): T = call("POST", path, body, idempotencyKey, dataSerializer)

    /** 无 data 返回体的命令（如 204 的退出/改密） */
    suspend fun postEmpty(path: String, body: String? = null) {
        call("POST", path, body, null, Unit.serializer())
    }

    private suspend fun <T> call(
        method: String,
        path: String,
        body: String?,
        idempotencyKey: String?,
        dataSerializer: DeserializationStrategy<T>
    ): T = withContext(Dispatchers.IO) {
        val tokenAtRequest = session.token
        val builder = Request.Builder()
            .url(BuildConfig.API_BASE_URL + path)
            .method(method, body?.toRequestBody("application/json".toMediaType()))
        idempotencyKey?.let { builder.header("Idempotency-Key", it) }
        http.newCall(builder.build()).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            when {
                resp.code == 401 && tokenAtRequest != null -> {
                    session.clear()
                    onSessionExpired?.invoke()
                    throw SessionExpiredException()
                }
                resp.isSuccessful -> {
                    if (text.isBlank()) {
                        @Suppress("UNCHECKED_CAST")
                        Unit as T
                    } else {
                        val root = json.parseToJsonElement(text).jsonObject
                        val requestId = (root["requestId"] as? JsonPrimitive)?.content
                        val data = root["data"] ?: throw ApiException(
                            resp.code, null, "响应缺少 data 字段", requestId = requestId
                        )
                        json.decodeFromJsonElement(dataSerializer, data)
                    }
                }
                else -> throw parseError(resp.code, text)
            }
        }
    }

    private fun parseError(status: Int, text: String): ApiException {
        val detail = runCatching {
            json.decodeFromString(ApiContract.ErrorBody.serializer(), text).error
        }.getOrNull()
        val requestId = runCatching {
            (json.parseToJsonElement(text).jsonObject["requestId"] as? JsonPrimitive)?.content
        }.getOrNull()
        return ApiException(
            httpStatus = status,
            code = detail?.code,
            message = detail?.message?.takeIf { it.isNotBlank() } ?: "请求失败（HTTP $status）",
            fieldErrors = detail?.fieldErrors ?: emptyList(),
            requestId = requestId,
            existingJournalId = detail?.existingJournalId
        )
    }
}
