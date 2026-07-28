package com.devpath.shared.api;

import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;

public final class RequestIds {
    private RequestIds() {
    }

    public static String resolve(HttpServletRequest request) {
        String supplied = request.getHeader("X-Request-Id");
        return supplied == null || supplied.isBlank() ? UUID.randomUUID().toString() : supplied.trim();
    }
}
