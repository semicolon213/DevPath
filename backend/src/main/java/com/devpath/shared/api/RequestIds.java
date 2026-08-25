package com.devpath.shared.api;

import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;

public final class RequestIds {
    public static final String REQUEST_HEADER = "X-Request-Id";
    public static final String CORRELATION_HEADER = "X-Correlation-Id";
    static final String REQUEST_ATTRIBUTE = RequestIds.class.getName() + ".requestId";
    static final String CORRELATION_ATTRIBUTE = RequestIds.class.getName() + ".correlationId";
    private static final int MAX_IDENTIFIER_LENGTH = 64;
    private static final String SAFE_IDENTIFIER = "[A-Za-z0-9][A-Za-z0-9._-]{0,63}";

    private RequestIds() {
    }

    public static String resolve(HttpServletRequest request) {
        Object resolved = request.getAttribute(REQUEST_ATTRIBUTE);
        if (resolved instanceof String value) return value;
        return sanitizeOrGenerate(request.getHeader(REQUEST_HEADER));
    }

    public static String resolveCorrelation(HttpServletRequest request) {
        Object resolved = request.getAttribute(CORRELATION_ATTRIBUTE);
        if (resolved instanceof String value) return value;
        String supplied = sanitize(request.getHeader(CORRELATION_HEADER));
        return supplied == null ? resolve(request) : supplied;
    }

    static String sanitizeOrGenerate(String supplied) {
        String sanitized = sanitize(supplied);
        return sanitized == null ? UUID.randomUUID().toString() : sanitized;
    }

    private static String sanitize(String supplied) {
        if (supplied == null) return null;
        String value = supplied.trim();
        return value.length() <= MAX_IDENTIFIER_LENGTH && value.matches(SAFE_IDENTIFIER) ? value : null;
    }
}
