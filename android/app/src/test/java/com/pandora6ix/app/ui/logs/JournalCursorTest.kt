package com.pandora6ix.app.ui.logs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JournalCursorTest {
    @Test fun skipsOldLayoutsForObservedCrashOffsets() {
        for (oldLength in listOf(1, 3, 9)) {
            val measured = "字".repeat(oldLength)
            assertNull(journalCursorOffset(measured, measured + "字", oldLength + 1))
        }
    }

    @Test fun skipsDeletionAndSameLengthReplacementUntilRemeasured() {
        assertNull(journalCursorOffset("日志正文", "日志", 2))
        assertNull(journalCursorOffset("日志", "工作", 2))
        assertEquals(2, journalCursorOffset("工作", "工作", 2))
    }

    @Test fun supportsEmptyMultilineAndEmojiSelections() {
        for (text in listOf("", "第一行\n第二行", "日志😀")) {
            assertEquals(0, journalCursorOffset(text, text, 0))
            assertEquals(text.length, journalCursorOffset(text, text, text.length))
            assertEquals(text.length, journalCursorOffset(text, text, Int.MAX_VALUE))
            assertEquals(0, journalCursorOffset(text, text, -1))
        }
    }
}
