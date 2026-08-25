package com.devpath.integration.adapter.in.web;

import com.devpath.integration.application.GitHubIntegrationUnavailableException;
import com.devpath.integration.application.GitHubConnectionNotFoundException;
import com.devpath.shared.api.ApiErrorResponse;
import com.devpath.shared.api.RequestIds;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = GitHubIntegrationController.class)
public class GitHubIntegrationExceptionHandler {
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
