package com.devpath.shared.api;

public record ApiResponse<T>(T data, ApiMetadata metadata) {
    public static <T> ApiResponse<T> of(T data, String requestId) {
        return new ApiResponse<>(data, ApiMetadata.create(requestId));
    }
}
