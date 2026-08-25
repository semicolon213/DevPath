package com.devpath.integration.adapter.in.web;

import com.devpath.integration.application.GitHubIntegrationUnavailableException;
import com.devpath.integration.application.GitHubConnectionNotFoundException;
import com.devpath.integration.application.GitHubRateLimitExceededException;
import com.devpath.shared.api.ApiErrorResponse;
import com.devpath.shared.api.RequestIds;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = GitHubIntegrationController.class)
public class GitHubIntegrationExceptionHandler {
    @ExceptionHandler(GitHubRateLimitExceededException.class)
    ResponseEntity<ApiErrorResponse> rateLimited(
        GitHubRateLimitExceededException exception,
        HttpServletRequest request
    ) {
        var response = ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS);
        if (exception.retryAfterSeconds() != null) {
            response.header(HttpHeaders.RETRY_AFTER, exception.retryAfterSeconds().toString());
        } else if (exception.resetAt() != null) {
            response.header(
                HttpHeaders.RETRY_AFTER,
                DateTimeFormatter.RFC_1123_DATE_TIME.format(exception.resetAt().atZone(ZoneOffset.UTC))
            );
        }
        if (exception.resetAt() != null) {
            response.header("X-RateLimit-Reset", Long.toString(exception.resetAt().getEpochSecond()));
        }
        return response.body(ApiErrorResponse.of(
            "RATE_LIMIT_EXCEEDED",
            "GitHub request limit was reached. Retry after the provider limit resets.",
            RequestIds.resolve(request)
        ));
    }

    @ExceptionHandler(GitHubConnectionNotFoundException.class)
    ResponseEntity<ApiErrorResponse> notFound(
        GitHubConnectionNotFoundException exception,
        HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ApiErrorResponse.of(
                "RESOURCE_NOT_FOUND",
                "The GitHub connection was not found.",
                RequestIds.resolve(request)
            )
        );
    }

    @ExceptionHandler(GitHubIntegrationUnavailableException.class)
    ResponseEntity<ApiErrorResponse> unavailable(
        GitHubIntegrationUnavailableException exception,
        HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
            ApiErrorResponse.of(
                "DEPENDENCY_UNAVAILABLE",
                "GitHub integration is temporarily unavailable.",
                RequestIds.resolve(request)
            )
        );
    }
}
