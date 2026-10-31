package com.pandora6ix.app.ui.logs

/** A stale layout must never be queried using the current input's cursor offset. */
internal fun journalCursorOffset(measuredText: String, currentText: String, selectionEnd: Int): Int? {
    if (measuredText != currentText) return null
    return selectionEnd.coerceIn(0, measuredText.length)
}
