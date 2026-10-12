package com.pandora6ix.backend.api.error;

import java.time.Instant;

public record ApiError(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path,
        String requestId
) {
}
