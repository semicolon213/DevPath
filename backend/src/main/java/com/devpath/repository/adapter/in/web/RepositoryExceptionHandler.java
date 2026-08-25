package com.devpath.repository.adapter.in.web;

import com.devpath.repository.application.RepositoryNotAccessibleException;
import com.devpath.repository.application.RepositoryNotFoundException;
import com.devpath.integration.application.GitHubIntegrationUnavailableException;
import com.devpath.integration.application.GitHubRateLimitExceededException;
import com.devpath.shared.api.ApiErrorResponse;
import com.devpath.shared.api.RequestIds;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = {RepositoryController.class, RepositorySynchronizationController.class})
public class RepositoryExceptionHandler {
    @ExceptionHandler(GitHubRateLimitExceededException.class)
    ResponseEntity<ApiErrorResponse> providerRateLimited(
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

    @ExceptionHandler(RepositoryNotFoundException.class)
    ResponseEntity<ApiErrorResponse> notFound(RuntimeException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ApiErrorResponse.of("RESOURCE_NOT_FOUND", "The repository was not found.", RequestIds.resolve(request))
        );
    }

    @ExceptionHandler({
        RepositoryNotAccessibleException.class,
        IllegalArgumentException.class,
        IllegalStateException.class,
        MethodArgumentNotValidException.class,
        HttpMessageNotReadableException.class
    })
    ResponseEntity<ApiErrorResponse> invalidRequest(Exception exception, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(
            ApiErrorResponse.of("VALIDATION_ERROR", "The repository request is invalid.", RequestIds.resolve(request))
        );
    }

    @ExceptionHandler({DataIntegrityViolationException.class, OptimisticLockingFailureException.class})
    ResponseEntity<ApiErrorResponse> conflict(RuntimeException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
            ApiErrorResponse.of("RESOURCE_CONFLICT", "The repository operation conflicted with a concurrent state change.", RequestIds.resolve(request))
        );
    }

    @ExceptionHandler(GitHubIntegrationUnavailableException.class)
    ResponseEntity<ApiErrorResponse> providerUnavailable(
        GitHubIntegrationUnavailableException exception,
        HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
            ApiErrorResponse.of(
                "DEPENDENCY_UNAVAILABLE",
                "GitHub repository verification is temporarily unavailable.",
                RequestIds.resolve(request)
            )
        );
    }
}
