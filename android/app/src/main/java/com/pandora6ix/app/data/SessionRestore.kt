package com.pandora6ix.app.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

sealed interface SessionRestoreResult {
    data class Authenticated(val user: UserDto) : SessionRestoreResult
    data object SignedOut : SessionRestoreResult
    data object Superseded : SessionRestoreResult
    data class Unavailable(val message: String, val failureKind: String) : SessionRestoreResult
}

/** A transient failure is not evidence that the stored login is invalid. */
suspend fun restoreSession(
    currentToken: () -> String?,
    loadUser: suspend () -> UserDto
): SessionRestoreResult {
    val expectedToken = currentToken() ?: return SessionRestoreResult.SignedOut
    return try {
        val user = loadUser()
        currentCoroutineContext().ensureActive()
        if (currentToken() == expectedToken) SessionRestoreResult.Authenticated(user)
        else SessionRestoreResult.Superseded
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        when (currentToken()) {
            null -> SessionRestoreResult.SignedOut
            expectedToken -> SessionRestoreResult.Unavailable(
                (failure as? ApiException)?.friendlyMessage() ?: "暂时无法连接服务，请检查网络后重试",
                if (failure is ApiException) "HTTP ${failure.httpStatus}" else failure.javaClass.simpleName
            )
            else -> SessionRestoreResult.Superseded
        }
    }
}
