package com.devpath.career.adapter.in.web;

import com.devpath.career.application.CareerNotFoundException;
import com.devpath.career.application.CareerReadinessNotFoundException;
import com.devpath.shared.api.ApiErrorResponse;
import com.devpath.shared.api.RequestIds;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = {CareerCatalogController.class, CareerReadinessController.class})
public class CareerApiExceptionHandler {
    @ExceptionHandler(CareerNotFoundException.class)
    ResponseEntity<ApiErrorResponse> notFound(CareerNotFoundException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ApiErrorResponse.of("RESOURCE_NOT_FOUND", "The career profile was not found.", RequestIds.resolve(request))
        );
    }

    @ExceptionHandler(CareerReadinessNotFoundException.class)
    ResponseEntity<ApiErrorResponse> readinessNotFound(
        CareerReadinessNotFoundException exception, HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ApiErrorResponse.of("RESOURCE_NOT_FOUND", "The career readiness result was not found.",
                RequestIds.resolve(request))
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiErrorResponse> invalid(ConstraintViolationException exception, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(
            ApiErrorResponse.of("VALIDATION_ERROR", "The career identifier is invalid.", RequestIds.resolve(request))
        );
    }
}
