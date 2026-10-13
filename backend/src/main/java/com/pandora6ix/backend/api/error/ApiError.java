package com.pandora6ix.backend.api.error;

import java.time.Instant;

public record ApiError(
        Instant timestamp, int status, String code, String message, String path, String requestId) {
    public java.util.Map<String, Object> getError() {
        return java.util.Map.of(
                "code", code, "message", message, "fieldErrors", java.util.List.of());
    }
}
