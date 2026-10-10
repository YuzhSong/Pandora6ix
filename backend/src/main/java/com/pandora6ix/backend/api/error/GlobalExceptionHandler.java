package com.pandora6ix.backend.api.error;

import java.time.Instant;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApiException(ApiException exception, HttpServletRequest request) {
        return response(exception.getStatus(), exception.getCode(), exception.getMessage(), request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception exception, Object body, HttpHeaders headers,
            HttpStatusCode status, WebRequest webRequest
    ) {
        HttpServletRequest request = ((ServletWebRequest) webRequest).getRequest();
        HttpStatus httpStatus = HttpStatus.resolve(status.value());
        String code = status.value() == 404 ? "RESOURCE_NOT_FOUND"
                : status.is5xxServerError() ? "INTERNAL_ERROR"
                : httpStatus == null ? "HTTP_ERROR" : httpStatus.name();
        String message = status.value() == 404 ? "Resource was not found."
                : status.is5xxServerError() ? "An unexpected error occurred."
                : httpStatus == null ? "Request could not be processed." : httpStatus.getReasonPhrase();
        return super.handleExceptionInternal(exception, error(status, code, message, request), headers, status, webRequest);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception exception, HttpServletRequest request) {
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred.", request);
    }

    private ResponseEntity<ApiError> response(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(status).body(error(status, code, message, request));
    }

    private ApiError error(HttpStatusCode status, String code, String message, HttpServletRequest request) {
        Object requestId = request.getAttribute(RequestIdFilter.ATTRIBUTE_NAME);
        return new ApiError(
                Instant.now(),
                status.value(),
                code,
                message,
                request.getRequestURI(),
                requestId == null ? "" : requestId.toString()
        );
    }
}
