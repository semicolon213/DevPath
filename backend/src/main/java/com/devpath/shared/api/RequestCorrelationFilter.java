package com.devpath.shared.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

public final class RequestCorrelationFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(RequestCorrelationFilter.class);

    @Override
    protected void doFilterInternal(
        HttpServletRequest request, HttpServletResponse response, FilterChain filterChain
    ) throws ServletException, IOException {
        String requestId = RequestIds.sanitizeOrGenerate(request.getHeader(RequestIds.REQUEST_HEADER));
        request.setAttribute(RequestIds.REQUEST_ATTRIBUTE, requestId);
        String correlationId = RequestIds.resolveCorrelation(request);
        request.setAttribute(RequestIds.CORRELATION_ATTRIBUTE, correlationId);
        response.setHeader(RequestIds.REQUEST_HEADER, requestId);
        response.setHeader(RequestIds.CORRELATION_HEADER, correlationId);

        long startedAt = System.nanoTime();
        try (MDC.MDCCloseable ignoredRequest = MDC.putCloseable("request_id", requestId);
             MDC.MDCCloseable ignoredCorrelation = MDC.putCloseable("correlation_id", correlationId)) {
            try {
                filterChain.doFilter(request, response);
            } finally {
                long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
                log.info("http_request_completed method={} path={} status={} duration_ms={}",
                    request.getMethod(), request.getRequestURI(), response.getStatus(), durationMs);
            }
        }
    }
}
