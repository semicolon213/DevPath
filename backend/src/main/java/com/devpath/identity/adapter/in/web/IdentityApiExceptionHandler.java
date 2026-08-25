package com.devpath.identity.adapter.in.web;

import com.devpath.identity.application.UserNotFoundException;
import com.devpath.identity.application.ProfileNotFoundException;
import com.devpath.identity.application.UnsupportedTargetException;
import com.devpath.identity.domain.DisabledAccountException;
import com.devpath.shared.api.ApiErrorResponse;
import com.devpath.shared.api.RequestIds;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;

@RestControllerAdvice(assignableTypes = {CurrentUserController.class, UserProfileController.class})
public class IdentityApiExceptionHandler {
    @ExceptionHandler({UserNotFoundException.class, DisabledAccountException.class})
    ResponseEntity<ApiErrorResponse> unavailableAccount(RuntimeException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
            ApiErrorResponse.of(
                "AUTHENTICATION_REQUIRED",
                "The authenticated account is unavailable.",
                RequestIds.resolve(request)
            )
        );
    }

    @ExceptionHandler(ProfileNotFoundException.class)
    ResponseEntity<ApiErrorResponse> missingProfile(ProfileNotFoundException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiErrorResponse.of("RESOURCE_NOT_FOUND", "The user profile was not found.", RequestIds.resolve(request)));
    }

    @ExceptionHandler({UnsupportedTargetException.class, IllegalArgumentException.class})
    ResponseEntity<ApiErrorResponse> invalidInput(RuntimeException exception, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(ApiErrorResponse.of("VALIDATION_ERROR", "The request contains an unsupported or invalid value.", RequestIds.resolve(request)));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    ResponseEntity<ApiErrorResponse> invalidRequest(Exception exception, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(ApiErrorResponse.of("VALIDATION_ERROR", "The request body is invalid.", RequestIds.resolve(request)));
    }
}
