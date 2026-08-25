package com.devpath.rule.adapter.in.web;

import com.devpath.rule.application.RuleEvaluationNotFoundException;
import com.devpath.rule.application.SkillMatrixNotFoundException;
import com.devpath.rule.application.SkillNotFoundException;
import com.devpath.shared.api.ApiErrorResponse;
import com.devpath.shared.api.RequestIds;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = {RuleEvaluationController.class, SkillMatrixController.class, SkillController.class})
public class RuleEvaluationExceptionHandler {
    @ExceptionHandler(RuleEvaluationNotFoundException.class)
    ResponseEntity<ApiErrorResponse> notFound(RuntimeException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ApiErrorResponse.of("RESOURCE_NOT_FOUND", "The rule evaluation was not found.", RequestIds.resolve(request))
        );
    }

    @ExceptionHandler(SkillMatrixNotFoundException.class)
    ResponseEntity<ApiErrorResponse> skillMatrixNotFound(RuntimeException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ApiErrorResponse.of("RESOURCE_NOT_FOUND", "The skill matrix was not found.", RequestIds.resolve(request))
        );
    }

    @ExceptionHandler(SkillNotFoundException.class)
    ResponseEntity<ApiErrorResponse> skillNotFound(RuntimeException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ApiErrorResponse.of("RESOURCE_NOT_FOUND", "The current skill assessment was not found.", RequestIds.resolve(request))
        );
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    ResponseEntity<ApiErrorResponse> invalidRequest(RuntimeException exception, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(
            ApiErrorResponse.of("VALIDATION_ERROR", "The rule evaluation request is invalid.", RequestIds.resolve(request))
        );
    }
}
