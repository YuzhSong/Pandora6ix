package com.pandora6ix.backend.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class JournalEditWindowTests {
    @Test
    void editWindowEndsExactly24HoursAfterCreation() {
        var created = Instant.parse("2026-10-31T23:30:00Z");
        var deadline = Instant.parse("2026-11-01T23:30:00Z");
        assertTrue(JournalService.withinEditWindow(created, created));
        assertTrue(JournalService.withinEditWindow(created, Instant.parse("2026-11-01T00:30:00Z")));
        assertTrue(JournalService.withinEditWindow(created, deadline.minusNanos(1)));
        assertFalse(JournalService.withinEditWindow(created, deadline));
        assertFalse(JournalService.withinEditWindow(created, deadline.plusNanos(1)));
        // 数据库时间戳精度或瞬时时钟回拨不应让刚创建的日志暂时只读。
        assertTrue(JournalService.withinEditWindow(created, created.minusNanos(1)));
    }

    @Test
    void timestampOffsetDoesNotChangeEligibility() {
        var created = Instant.parse("2026-10-31T23:30:00Z");
        var now = created.plusSeconds(23 * 3600);
        assertTrue(JournalService.withinEditWindow(created.atOffset(ZoneOffset.ofHours(14)), now));
        assertTrue(JournalService.withinEditWindow(created.atOffset(ZoneOffset.ofHours(-12)), now));
        assertFalse(JournalService.withinEditWindow(null, now));
    }
}
