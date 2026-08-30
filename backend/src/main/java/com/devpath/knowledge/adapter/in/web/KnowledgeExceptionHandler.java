package com.devpath.knowledge.adapter.in.web;

import com.devpath.knowledge.application.KnowledgeNotFoundException;
import com.devpath.knowledge.application.KnowledgeRetrievalUnavailableException;
import com.devpath.integration.application.NotionConnectionNotFoundException;
import com.devpath.integration.application.NotionIntegrationUnavailableException;
import com.devpath.shared.api.ApiErrorResponse;
import com.devpath.shared.api.RequestIds;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes=KnowledgeController.class)
public class KnowledgeExceptionHandler {
    @ExceptionHandler({KnowledgeNotFoundException.class,NotionConnectionNotFoundException.class})
    ResponseEntity<ApiErrorResponse> notFound(RuntimeException exception,HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiErrorResponse.of("RESOURCE_NOT_FOUND",
            "The knowledge resource was not found.",RequestIds.resolve(request)));
    }
    @ExceptionHandler(NotionIntegrationUnavailableException.class)
    ResponseEntity<ApiErrorResponse> unavailable(RuntimeException exception,HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ApiErrorResponse.of("DEPENDENCY_UNAVAILABLE",
            "The Notion connection is temporarily unavailable.",RequestIds.resolve(request)));
    }
    @ExceptionHandler(KnowledgeRetrievalUnavailableException.class)
    ResponseEntity<ApiErrorResponse> retrievalUnavailable(RuntimeException exception,HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ApiErrorResponse.of("KNOWLEDGE_RETRIEVAL_FAILED",
            "Knowledge search is temporarily unavailable.",RequestIds.resolve(request)));
    }
    @ExceptionHandler({IllegalArgumentException.class,IllegalStateException.class,MethodArgumentNotValidException.class,
        HttpMessageNotReadableException.class,MissingRequestHeaderException.class})
    ResponseEntity<ApiErrorResponse> invalid(Exception exception,HttpServletRequest request) {
        return ResponseEntity.badRequest().body(ApiErrorResponse.of("VALIDATION_ERROR",
            "The knowledge request is invalid.",RequestIds.resolve(request)));
    }
    @ExceptionHandler({DataIntegrityViolationException.class,OptimisticLockingFailureException.class})
    ResponseEntity<ApiErrorResponse> conflict(RuntimeException exception,HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiErrorResponse.of("RESOURCE_CONFLICT",
            "The knowledge operation conflicted with concurrent state.",RequestIds.resolve(request)));
    }
}
