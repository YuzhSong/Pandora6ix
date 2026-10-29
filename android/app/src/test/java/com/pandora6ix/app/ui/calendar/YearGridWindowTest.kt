package com.pandora6ix.app.ui.calendar

import com.pandora6ix.app.mock.DemoDate
import org.junit.Assert.*
import org.junit.Test

class YearGridWindowTest {
    @Test fun distantSelectionsInitializeAtTheirActualMonth() {
        for (year in listOf(1900, 1990, 2000, 2030, 2090, 2100)) {
            for (month in 1..12) {
                val target = DemoDate(year, month, 1)
                val window = YearGridWindow(year, month)
                assertEquals(target, window.monthAt(window.initialIndex))
                assertEquals(0, window.scrollOffset)
                assertEquals(target.plusMonths(-1), window.monthAt(window.initialIndex - 1))
                assertEquals(target.plusMonths(1), window.monthAt(window.initialIndex + 1))
                // Jump distance does not grow the generated window or require walking intervening years.
                assertEquals(YearGridWindow(2026, 1).itemCount, window.itemCount)
            }
        }
    }

    @Test fun bothEdgesPreserveVisibleMonthAndPixelOffset() {
        val window = YearGridWindow(2030, 6)
        for (index in listOf(0, 12, window.itemCount - 24, window.itemCount - 2)) {
            for (offset in listOf(0, 17, 233)) {
                val firstVisibleMonth = window.monthAt(index)
                val recentered = window.recenteredAt(index, offset)
                assertNotEquals(window, recentered)
                assertEquals(firstVisibleMonth, recentered.monthAt(recentered.initialIndex))
                assertEquals(offset, recentered.scrollOffset)
                assertSame(recentered, recentered.recenteredAt(recentered.initialIndex, offset))
            }
        }
    }

    @Test fun ordinaryScrollingDoesNotReplaceTheWindow() {
        val window = YearGridWindow(2090, 12)
        for (delta in -24..24) {
            assertSame(window, window.recenteredAt(window.initialIndex + delta, 75))
        }
    }

    @Test fun continuousRecenteringKeepsNeighborsAcrossYears() {
        for (forward in listOf(false, true)) {
            var window = YearGridWindow(2030, 6)
            repeat(20) {
                val edgeIndex = if (forward) window.itemCount - 2 else 0
                val expected = window.monthAt(edgeIndex)
                window = window.recenteredAt(edgeIndex, 123)
                assertEquals(expected, window.monthAt(window.initialIndex))
                assertEquals(expected.plusMonths(-1), window.monthAt(window.initialIndex - 1))
                assertEquals(expected.plusMonths(1), window.monthAt(window.initialIndex + 1))
                assertEquals(123, window.scrollOffset)
            }
        }
    }
}
