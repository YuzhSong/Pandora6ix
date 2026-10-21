package com.pandora6ix.app.data

import android.content.SharedPreferences
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import java.lang.reflect.Proxy
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class PasswordRenewalTest {
    private fun preferences(): SharedPreferences {
        val values = ConcurrentHashMap<String, String>()
        lateinit var editor: SharedPreferences.Editor
        editor = Proxy.newProxyInstance(javaClass.classLoader, arrayOf(SharedPreferences.Editor::class.java)) { _, method, args ->
            when (method.name) {
                "putString" -> { if (args!![1] == null) values.remove(args[0]) else values[args[0] as String] = args[1] as String; editor }
                "clear" -> { values.clear(); editor }
                "apply" -> null
                else -> error("Unexpected editor call: ${method.name}")
            }
        } as SharedPreferences.Editor
        return Proxy.newProxyInstance(javaClass.classLoader, arrayOf(SharedPreferences::class.java)) { _, method, args ->
            when (method.name) {
                "getString" -> values[args!![0]] ?: args[1]
                "edit" -> editor
                else -> error("Unexpected preferences call: ${method.name}")
            }
        } as SharedPreferences
    }

    private class Fixture : AutoCloseable {
        val session = SessionStore(PasswordRenewalTest().preferences())
        val executor = Executors.newCachedThreadPool()
        val server = LocalServer(executor)
        val api = ApiClient(session, "http://127.0.0.1:${server.port}")
        val auth = AuthRepository(api, session)
        val expired = AtomicInteger()
        init {
            session.save("old-token", "person")
            session.saveCredentials("company", "person")
            api.onSessionExpired = { expired.incrementAndGet() }
        }
        fun route(path: String, handler: (Exchange) -> Unit) { server.routes[path] = handler }
        override fun close() { server.close(); executor.shutdownNow() }
    }

    // A real loopback HTTP server without Android instrumentation or additional dependencies.
    private class Exchange(private val socket: Socket) {
        fun reply(status: Int, body: String = "") {
            val bytes = body.toByteArray()
            socket.getOutputStream().apply {
                write("HTTP/1.1 $status Test\r\nContent-Type: application/json\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n".toByteArray())
                write(bytes); flush()
            }
        }
    }
    private class LocalServer(private val executor: java.util.concurrent.ExecutorService) : AutoCloseable {
        private val listener = ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"))
        val port: Int get() = listener.localPort
        val routes = ConcurrentHashMap<String, (Exchange) -> Unit>()
        init {
            executor.submit {
                while (!listener.isClosed) {
                    val socket = try { listener.accept() } catch (_: java.io.IOException) { break }
                    executor.submit {
                        socket.use {
                            it.soTimeout = 15000
                            val input = it.getInputStream()
                            val header = StringBuilder()
                            while (!header.endsWith("\r\n\r\n")) {
                                val byte = input.read(); check(byte != -1); header.append(byte.toChar())
                                check(header.length < 65536)
                            }
                            val lines = header.toString().split("\r\n")
                            val length = lines.firstOrNull { line -> line.startsWith("Content-Length:", true) }?.substringAfter(':')?.trim()?.toInt() ?: 0
                            check(input.readNBytes(length).size == length)
                            val path = lines.first().split(' ')[1].substringBefore('?')
                            val exchange = Exchange(it)
                            routes[path]?.invoke(exchange) ?: exchange.reply(404)
                        }
                    }
                }
            }
        }
        override fun close() { listener.close() }
    }
    private val login = """{"data":{"accessToken":"new-token","user":{"id":"person"}}}"""
    private fun CountDownLatch.awaitRequest() { check(await(10, TimeUnit.SECONDS)) { "HTTP request timed out" } }

    @Test fun oldToken401DuringRenewalDoesNotLogOutAndNormal401StillDoes() = runBlocking {
        Fixture().use { f ->
            val entered = CountDownLatch(1); val release = CountDownLatch(1)
            f.route("/me/password") { it.reply(204) }
            f.route("/auth/login") { entered.countDown(); release.awaitRequest(); it.reply(200, login) }
            f.route("/me") { it.reply(401) }
            val renewal = async(Dispatchers.IO) { f.auth.changePassword("old-password", "new-password") }
            try {
                entered.awaitRequest()
                assertTrue(runCatching { f.auth.me() }.exceptionOrNull() is SessionExpiredException)
                assertEquals(0, f.expired.get())
                assertEquals("old-token", f.session.token)
            } finally { release.countDown() }
            assertEquals("person", renewal.await()?.id)
            assertEquals("new-token", f.session.token)
            assertFalse(f.session.isPasswordRenewing("old-token"))
            assertTrue(runCatching { f.auth.me() }.exceptionOrNull() is SessionExpiredException)
            assertEquals(1, f.expired.get())
            assertNull(f.session.token)
        }
    }

    @Test fun rejectedPasswordRetainsSessionAndReleasesGuard() = runBlocking {
        Fixture().use { f ->
            f.route("/me/password") { it.reply(422, """{"error":{"code":"VALIDATION_FAILED"}}""") }
            assertTrue(runCatching { f.auth.changePassword("wrong", "new-password") }.exceptionOrNull() is ApiException)
            assertEquals("old-token", f.session.token)
            assertFalse(f.session.isPasswordRenewing("old-token"))
        }
    }

    @Test fun failedRenewalClearsRevokedSession() = runBlocking {
        Fixture().use { f ->
            f.route("/me/password") { it.reply(204) }
            f.route("/auth/login") { it.reply(401) }
            assertNull(f.auth.changePassword("old-password", "new-password"))
            assertNull(f.session.token)
            assertFalse(f.session.isPasswordRenewing("old-token"))
        }
    }

    @Test fun missingSavedIdentityRequiresManualLogin() = runBlocking {
        Fixture().use { f ->
            f.session.clear(); f.session.save("old-token", "person")
            f.route("/me/password") { it.reply(204) }
            assertNull(f.auth.changePassword("old-password", "new-password"))
            assertNull(f.session.token)
        }
    }

    @Test fun delayedRenewalCannotReplaceOrClearAnotherAccount() = runBlocking {
        Fixture().use { f ->
            val entered = CountDownLatch(1); val release = CountDownLatch(1)
            f.route("/me/password") { it.reply(204) }
            f.route("/auth/login") { entered.countDown(); release.awaitRequest(); it.reply(200, login) }
            val renewal = async(Dispatchers.IO) { f.auth.changePassword("old-password", "new-password") }
            try {
                entered.awaitRequest()
                f.session.clear(); f.session.save("other-token", "other-person")
                f.session.saveCredentials("other-company", "other-person")
            } finally { release.countDown() }
            assertNull(renewal.await())
            assertEquals("other-token", f.session.token)
            assertEquals("other-person", f.session.savedLoginName)
            assertEquals(0, f.expired.get())
        }
    }

    @Test fun cancellationIsPropagatedAndGuardIsReleased() = runBlocking {
        Fixture().use { f ->
            val entered = CountDownLatch(1); val release = CountDownLatch(1)
            f.route("/me/password") { it.reply(204) }
            f.route("/auth/login") { entered.countDown(); release.awaitRequest(); it.reply(200, login) }
            val renewal = async(Dispatchers.IO) { f.auth.changePassword("old-password", "new-password") }
            try { entered.awaitRequest(); renewal.cancel() } finally { release.countDown() }
            renewal.join()
            assertTrue(renewal.isCancelled)
            assertNull(f.session.token)
            assertFalse(f.session.isPasswordRenewing("old-token"))
        }
    }
}
