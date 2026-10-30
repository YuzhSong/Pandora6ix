package com.pandora6ix.backend;

import com.pandora6ix.backend.api.Contracts.Recurrence;
import com.pandora6ix.backend.domain.task.RecurrenceDates;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RecurrenceDatesTests {
    private LocalDate date(String value) { return LocalDate.parse(value); }
    @Test void monthEndDoesNotDrift() {
        assertEquals(List.of(date("2028-01-31"), date("2028-03-31")), RecurrenceDates.generate(
                date("2028-01-31"), date("2028-04-30"), new Recurrence("MONTHLY", 1, List.of())));
    }
    @Test void leapDayAndCustomMonths() {
        assertEquals(List.of(date("2028-02-29"), date("2032-02-29")), RecurrenceDates.generate(
                date("2028-02-29"), date("2032-02-29"), new Recurrence("MONTHLY", 12, List.of())));
    }
    @Test void weeklyAnchorsToMondayAndClipsBothEnds() {
        assertEquals(List.of(date("2026-10-30"), date("2026-11-09"), date("2026-11-13")),
                RecurrenceDates.generate(date("2026-10-28"), date("2026-11-13"), new Recurrence("WEEKLY", 2, List.of(1,5))));
    }
    @Test void dailyIntervalAndInclusiveEnd() {
        assertEquals(List.of(date("2030-01-01"), date("2030-01-04"), date("2030-01-07")),
                RecurrenceDates.generate(date("2030-01-01"), date("2030-01-07"), new Recurrence("DAILY", 3, null)));
    }
    @Test void rejectsInvalidOrUnboundedRules() {
        var start = date("2030-01-01");
        for (var rule : List.of(new Recurrence("DAILY",0,null), new Recurrence("WEEKLY",1,List.of()),
                new Recurrence("WEEKLY",1,List.of(8)), new Recurrence("OTHER",1,null)))
            assertThrows(RuntimeException.class, () -> RecurrenceDates.generate(start, start.plusDays(1), rule));
        assertThrows(RuntimeException.class, () -> RecurrenceDates.generate(start, start.plusDays(366), new Recurrence("DAILY",1,null)));
        assertThrows(RuntimeException.class, () -> RecurrenceDates.generate(start, start.minusDays(1), new Recurrence("DAILY",1,null)));
    }
}
