package com.pandora6ix.backend.api;

import com.pandora6ix.backend.api.error.ApiException;

import org.springframework.http.HttpStatus;

import java.util.Map;

public class BusinessException extends ApiException {
    private final Map<String, Object> details;

    public BusinessException(int status, String code, String message) {
        this(status, code, message, Map.of());
    }

    public BusinessException(int status, String code, String message, Map<String, Object> details) {
        super(HttpStatus.valueOf(status), code, message);
        this.details = details;
    }

    public Map<String, Object> details() {
        return details;
    }

    public static BusinessException missing() {
        return new BusinessException(404, "RESOURCE_NOT_FOUND", "Resource was not found.");
    }

    public static BusinessException forbidden() {
        return new BusinessException(403, "FORBIDDEN", "Permission is required.");
    }

    public static BusinessException invalid(String message) {
        return new BusinessException(422, "VALIDATION_FAILED", message);
    }

    public static BusinessException policy(String message) {
        return new BusinessException(409, "WORKFLOW_POLICY_UNCONFIRMED", message);
    }

    public static void checkVersion(int updated) {
        if (updated != 1)
            throw new BusinessException(
                    409, "VERSION_CONFLICT", "Read the latest version before retrying.");
    }
}
