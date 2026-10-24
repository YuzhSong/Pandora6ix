package com.pandora6ix.app.ui.calendar

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import org.junit.Assert.*
import org.junit.Test

class RecipientPopupPositionProviderTest {
    private val position = RecipientPopupPositionProvider(gap = 8, inset = 16)

    @Test fun candidatesStayAboveQueryInKeyboardReducedWindow() {
        val anchor = IntRect(30, 580, 530, 636)
        val result = position.calculatePosition(anchor, IntSize(600, 700), LayoutDirection.Ltr, IntSize(500, 280))
        assertEquals(IntOffset(30, 292), result)
        assertEquals(anchor.top - 8, result.y + 280)
    }

    @Test fun clippingAtWindowEdgesKeepsPopupInsideViewport() {
        val result = position.calculatePosition(IntRect(580, 80, 630, 136), IntSize(600, 400), LayoutDirection.Ltr, IntSize(500, 250))
        assertTrue(result.x >= 0 && result.x + 500 <= 600)
        assertTrue(result.y >= 16 && result.y + 250 <= 400)
    }

    @Test fun rtlAlignmentAndFullWidthPopupsRemainReachable() {
        assertEquals(100, position.calculatePosition(IntRect(30, 500, 400, 556), IntSize(600, 700), LayoutDirection.Rtl, IntSize(300, 200)).x)
        assertEquals(0, position.calculatePosition(IntRect(0, 500, 600, 556), IntSize(600, 700), LayoutDirection.Ltr, IntSize(600, 200)).x)
    }
}
