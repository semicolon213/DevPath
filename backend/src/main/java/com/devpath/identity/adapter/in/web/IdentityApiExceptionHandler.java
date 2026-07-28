package com.devpath.identity.adapter.in.web;

import com.devpath.identity.application.UserNotFoundException;
import com.devpath.identity.domain.DisabledAccountException;
import com.devpath.shared.api.ApiErrorResponse;
import com.devpath.shared.api.RequestIds;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = CurrentUserController.class)
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
}
