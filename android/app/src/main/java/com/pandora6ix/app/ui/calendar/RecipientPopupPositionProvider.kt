package com.pandora6ix.app.ui.calendar

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.PopupPositionProvider

internal class RecipientPopupPositionProvider(private val gap: Int, private val inset: Int) : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
        val maxLeft = (windowSize.width - popupContentSize.width).coerceAtLeast(0)
        val minX = inset.coerceAtMost(maxLeft)
        val maxX = (maxLeft - inset).coerceAtLeast(minX)
        val anchorX = if (layoutDirection == LayoutDirection.Ltr) anchorBounds.left else anchorBounds.right - popupContentSize.width
        val maxTop = (windowSize.height - popupContentSize.height).coerceAtLeast(0)
        val minY = inset.coerceAtMost(maxTop)
        val maxY = (maxTop - inset).coerceAtLeast(minY)
        return IntOffset(anchorX.coerceIn(minX, maxX), (anchorBounds.top - popupContentSize.height - gap).coerceIn(minY, maxY))
    }
}
