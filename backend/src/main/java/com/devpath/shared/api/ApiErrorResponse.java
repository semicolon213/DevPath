package com.devpath.shared.api;

public record ApiErrorResponse(ApiError error, ApiMetadata metadata) {
    public static ApiErrorResponse of(String code, String message, String requestId) {
        return new ApiErrorResponse(new ApiError(code, message), ApiMetadata.create(requestId));
    }

    public record ApiError(String code, String message) {
    }
}
