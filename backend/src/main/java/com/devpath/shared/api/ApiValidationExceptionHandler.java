package com.devpath.shared.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Order(Ordered.LOWEST_PRECEDENCE)
@RestControllerAdvice
public class ApiValidationExceptionHandler {
    @ExceptionHandler({MissingRequestHeaderException.class, HandlerMethodValidationException.class,
        ConstraintViolationException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ApiErrorResponse> invalidRequest(Exception exception, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(ApiErrorResponse.of(
            "VALIDATION_ERROR", "The request is invalid.", RequestIds.resolve(request)
        ));
    }
}
