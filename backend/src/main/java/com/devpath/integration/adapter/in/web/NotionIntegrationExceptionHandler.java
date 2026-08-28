package com.devpath.integration.adapter.in.web;

import com.devpath.integration.application.NotionConnectionNotFoundException;
import com.devpath.integration.application.NotionIntegrationUnavailableException;
import com.devpath.integration.application.NotionRateLimitExceededException;
import com.devpath.shared.api.ApiErrorResponse;
import com.devpath.shared.api.RequestIds;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = NotionIntegrationController.class)
public class NotionIntegrationExceptionHandler {
    @ExceptionHandler(NotionRateLimitExceededException.class)
    ResponseEntity<ApiErrorResponse> rateLimited(NotionRateLimitExceededException exception, HttpServletRequest request) {
        var response = ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS);
        if (exception.retryAfterSeconds() != null) response.header(HttpHeaders.RETRY_AFTER, exception.retryAfterSeconds().toString());
        return response.body(ApiErrorResponse.of("RATE_LIMIT_EXCEEDED", "Notion request limit was reached. Retry later.", RequestIds.resolve(request)));
    }

    @ExceptionHandler(NotionConnectionNotFoundException.class)
    ResponseEntity<ApiErrorResponse> notFound(NotionConnectionNotFoundException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiErrorResponse.of("RESOURCE_NOT_FOUND", "The Notion connection was not found.", RequestIds.resolve(request)));
    }

    @ExceptionHandler(NotionIntegrationUnavailableException.class)
    ResponseEntity<ApiErrorResponse> unavailable(NotionIntegrationUnavailableException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ApiErrorResponse.of("DEPENDENCY_UNAVAILABLE", "Notion integration is temporarily unavailable.", RequestIds.resolve(request)));
    }
}
