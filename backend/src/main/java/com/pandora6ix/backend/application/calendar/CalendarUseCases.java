package com.pandora6ix.backend.application.calendar;

public interface CalendarUseCases {
    java.util.Map<String, Object> calendar(
            com.pandora6ix.backend.service.IdentityService.Actor actor,
            String mode,
            java.time.LocalDate anchor,
            java.util.List<String> tagIds,
            String cursor,
            int limit);
}
