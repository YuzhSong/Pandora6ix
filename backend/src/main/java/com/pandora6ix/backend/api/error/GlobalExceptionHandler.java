package com.pandora6ix.backend.api.error;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Instant;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final Pattern CONSTRAINT = Pattern.compile("(?i)constraint\\s+[\\\"']?([A-Za-z][A-Za-z0-9_.-]{0,127})");
    @ExceptionHandler(com.pandora6ix.backend.api.BusinessException.class)
    public ResponseEntity<Object> handleBusiness(
            com.pandora6ix.backend.api.BusinessException exception, HttpServletRequest request) {
        var body = new java.util.LinkedHashMap<String, Object>();
        var err =
                error(exception.getStatus(), exception.getCode(), exception.getMessage(), request);
        body.put("timestamp", err.timestamp());
        body.put("status", err.status());
        body.put("code", err.code());
        body.put("message", err.message());
        body.put("path", err.path());
        body.put("requestId", err.requestId());
        var nested = new java.util.LinkedHashMap<String, Object>(err.getError());
        nested.putAll(exception.details());
        body.put("error", nested);
        return ResponseEntity.status(exception.getStatus()).body(body);
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleIntegrity(
            org.springframework.dao.DataIntegrityViolationException exception,
            HttpServletRequest request) {
        String sqlState = null, constraint = null;
        int vendorCode = 0;
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sql) {
                if (sqlState == null) sqlState = sql.getSQLState();
                if (vendorCode == 0) vendorCode = sql.getErrorCode();
            }
            String safeName = constraintName(cause.getMessage());
            if (constraint == null && safeName != null) constraint = safeName;
        }
        Object requestId = request.getAttribute(RequestIdFilter.ATTRIBUTE_NAME);
        log.warn("database_integrity_conflict requestId={} method={} path={} sqlState={} vendorCode={} constraint={}",
                requestId, request.getMethod(), request.getRequestURI(), sqlState, vendorCode,
                constraint == null ? "unavailable" : constraint);
        return handleBusiness(
                new com.pandora6ix.backend.api.BusinessException(
                        409,
                        "DATA_CONFLICT",
                        "A unique resource or valid relationship is required."),
                request);
    }

    private String constraintName(String message) {
        if (message == null) return null;
        Matcher matcher = CONSTRAINT.matcher(message);
        return matcher.find() ? matcher.group(1) : null;
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApiException(
            ApiException exception, HttpServletRequest request) {
        return response(
                exception.getStatus(), exception.getCode(), exception.getMessage(), request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception exception,
            Object body,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest webRequest) {
        HttpServletRequest request = ((ServletWebRequest) webRequest).getRequest();
        if (request.getRequestURI().startsWith("/api/v1/")
                && exception
                        instanceof
                        org.springframework.web.bind.MethodArgumentNotValidException validation) {
            var fields =
                    validation.getBindingResult().getFieldErrors().stream()
                            .map(
                                    f ->
                                            java.util.Map.of(
                                                    "field",
                                                    f.getField(),
                                                    "message",
                                                    f.getDefaultMessage() == null
                                                            ? "Invalid field"
                                                            : f.getDefaultMessage()))
                            .toList();
            return handleBusiness(
                    new com.pandora6ix.backend.api.BusinessException(
                            422,
                            "VALIDATION_FAILED",
                            "Check the request fields.",
                            java.util.Map.of("fieldErrors", fields)),
                    request);
        }
        HttpStatus httpStatus = HttpStatus.resolve(status.value());
        String code =
                status.value() == 404
                        ? "RESOURCE_NOT_FOUND"
                        : status.is5xxServerError()
                                ? "INTERNAL_ERROR"
                                : httpStatus == null ? "HTTP_ERROR" : httpStatus.name();
        String message =
                status.value() == 404
                        ? "Resource was not found."
                        : status.is5xxServerError()
                                ? "An unexpected error occurred."
                                : httpStatus == null
                                        ? "Request could not be processed."
                                        : httpStatus.getReasonPhrase();
        return super.handleExceptionInternal(
                exception, error(status, code, message, request), headers, status, webRequest);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(
            Exception exception, HttpServletRequest request) {
        return response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "An unexpected error occurred.",
                request);
    }

    private ResponseEntity<ApiError> response(
            HttpStatus status, String code, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(error(status, code, message, request));
    }

    private ApiError error(
            HttpStatusCode status, String code, String message, HttpServletRequest request) {
        Object requestId = request.getAttribute(RequestIdFilter.ATTRIBUTE_NAME);
        return new ApiError(
                Instant.now(),
                status.value(),
                code,
                message,
                request.getRequestURI(),
                requestId == null ? "" : requestId.toString());
    }
}
