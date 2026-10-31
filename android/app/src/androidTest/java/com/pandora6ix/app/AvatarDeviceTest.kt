package com.pandora6ix.app

import android.graphics.Bitmap
import android.content.ContextWrapper
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.platform.app.InstrumentationRegistry
import com.pandora6ix.app.data.ApiClient
import com.pandora6ix.app.data.SessionStore
import com.pandora6ix.app.data.UserRepository
import com.pandora6ix.app.ui.components.RemoteAvatar
import com.pandora6ix.app.ui.theme.PandoraTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Real Android Bitmap/JPEG/network/Compose rendering, against an isolated fixture.
 * Never reads the user's login or calls the production service.
 */
class AvatarDeviceTest {
    @get:Rule val compose = createComposeRule()

    @Test fun uploadRefreshAndFreshReaderRenderTheSharedPhoto() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val base = requireNotNull(InstrumentationRegistry.getArguments().getString("avatarFixtureUrl"))
        require(base.startsWith("http://10.0.2.2:") && base.endsWith("/avatar-fixture"))
        // Instrumentation context belongs to the test APK, not the user's app.
        val session = SessionStore(instrumentation.context)
        val readerSession = SessionStore(object : ContextWrapper(instrumentation.context) {
            override fun getSharedPreferences(name: String, mode: Int) =
                super.getSharedPreferences("avatar_reader_$name", mode)
        })
        try {
            session.save("fixture-subject", "avatar-test-subject")
            val owner = UserRepository(ApiClient(session, base))
            val original = Bitmap.createBitmap(512, 320, Bitmap.Config.ARGB_8888).apply {
                eraseColor(AndroidColor.rgb(30, 150, 70))
            }
            owner.uploadAvatar("avatar-test-subject", original)
            val cached = requireNotNull(owner.avatar("avatar-test-subject"))
            assertEquals(256, cached.width)
            assertEquals(160, cached.height)
            assertEquals(1L, owner.avatarRevision.value)

            // New authenticated reader/repository must fetch and decode persisted bytes.
            readerSession.save("fixture-manager", "avatar-test-manager")
            val reader = UserRepository(ApiClient(readerSession, base))
            val fetched = requireNotNull(reader.avatar("avatar-test-subject"))
            assertEquals(256, fetched.width)
            assertEquals(160, fetched.height)
            assertTrue(AndroidColor.green(fetched.getPixel(128, 80)) > 120)
            compose.setContent {
                PandoraTheme {
                    Column {
                        RemoteAvatar("avatar-test-subject", "成员", 48.dp, 18.sp, owner)
                        RemoteAvatar("avatar-test-subject", "下属", 48.dp, 18.sp, reader)
                    }
                }
            }
            compose.waitUntil(10_000) {
                compose.onAllNodesWithContentDescription("下属 的头像").fetchSemanticsNodes().size == 1
            }
            fun centerGreen(description: String): Float {
                val pixels = compose.onNodeWithContentDescription(description).captureToImage().toPixelMap()
                return pixels[pixels.width / 2, pixels.height / 2].green
            }
            assertTrue(centerGreen("下属 的头像") > .45f)

            // Simulate a gallery/camera replacement: real compressed upload and revision.
            val replacement = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888).apply {
                eraseColor(AndroidColor.rgb(190, 40, 30))
            }
            owner.uploadAvatar("avatar-test-subject", replacement)
            assertEquals(2L, owner.avatarRevision.value)
            compose.waitUntil(10_000) { centerGreen("成员 的头像") < .3f }
            // A separately authenticated, already visible reader must also refresh
            // after the bounded positive-cache TTL, without reopening the screen.
            compose.waitUntil(45_000) { centerGreen("下属 的头像") < .3f }
            val relaunchedReader = UserRepository(ApiClient(readerSession, base))
            assertTrue(AndroidColor.red(requireNotNull(relaunchedReader.avatar("avatar-test-subject")).getPixel(64, 64)) > 150)
        } finally {
            session.clear()
            readerSession.clear()
        }
    }
}
