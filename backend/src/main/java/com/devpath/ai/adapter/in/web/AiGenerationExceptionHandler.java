package com.devpath.ai.adapter.in.web;

import com.devpath.ai.application.GenerationNotFoundException;
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

@RestControllerAdvice(assignableTypes = AiGenerationController.class)
public class AiGenerationExceptionHandler {
    @ExceptionHandler(GenerationNotFoundException.class)
    ResponseEntity<ApiErrorResponse> notFound(RuntimeException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiErrorResponse.of(
            "RESOURCE_NOT_FOUND", "The AI generation resource was not found.", RequestIds.resolve(request)
        ));
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class,
        MethodArgumentNotValidException.class, HttpMessageNotReadableException.class, MissingRequestHeaderException.class})
    ResponseEntity<ApiErrorResponse> invalid(Exception exception, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(ApiErrorResponse.of(
            "VALIDATION_ERROR", "The AI generation request is invalid.", RequestIds.resolve(request)
        ));
    }

    @ExceptionHandler({DataIntegrityViolationException.class, OptimisticLockingFailureException.class})
    ResponseEntity<ApiErrorResponse> conflict(RuntimeException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiErrorResponse.of(
            "RESOURCE_CONFLICT", "The AI generation operation conflicts with existing state.", RequestIds.resolve(request)
        ));
    }
}
