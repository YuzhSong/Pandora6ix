package com.pandora6ix.app.data

import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class SessionRestoreTest {
    @Test fun absentSessionDoesNotRequestProtectedProfile() = runBlocking {
        val result = restoreSession({ null }) { error("Profile must not be requested") }
        assertEquals(SessionRestoreResult.SignedOut, result)
    }

    @Test fun networkFailureKeepsSessionAndExplicitRetryCanRestoreIt() = runBlocking {
        val token = "fixture-session"
        val failure = restoreSession({ token }) { throw IOException("offline") }
        assertTrue(failure is SessionRestoreResult.Unavailable)
        val user = UserDto("person")
        assertEquals(SessionRestoreResult.Authenticated(user), restoreSession({ token }) { user })
    }

    @Test fun serverFailureIsNotSignedOut() = runBlocking {
        val failure = restoreSession({ "fixture-session" }) { throw ApiException(503, null, "fixture") }
        assertTrue(failure is SessionRestoreResult.Unavailable)
        assertEquals("HTTP 503", (failure as SessionRestoreResult.Unavailable).failureKind)
    }

    @Test fun confirmedInvalidatedSessionShowsLogin() = runBlocking {
        var token: String? = "fixture-session"
        val result = restoreSession({ token }) { token = null; throw SessionExpiredException() }
        assertEquals(SessionRestoreResult.SignedOut, result)
    }

    @Test fun cancellationIsNeverConvertedToLoginOrRetry() = runBlocking {
        val cancelled = CancellationException("fixture cancellation")
        try {
            restoreSession({ "fixture-session" }) { throw cancelled }
            fail("Expected cancellation")
        } catch (actual: CancellationException) { assertSame(cancelled, actual) }
    }

    @Test fun profileFromPreviousLoginCannotPublishCurrentUser() = runBlocking {
        var token = "old-fixture-session"
        val result = restoreSession({ token }) { token = "new-fixture-session"; UserDto("old-person") }
        assertEquals(SessionRestoreResult.Superseded, result)
    }

    @Test fun oldSessionErrorCannotInvalidateReplacementLogin() = runBlocking {
        var token = "old-fixture-session"
        val result = restoreSession({ token }) { token = "new-fixture-session"; throw SessionExpiredException() }
        assertEquals(SessionRestoreResult.Superseded, result)
        assertEquals("new-fixture-session", token)
    }
}
