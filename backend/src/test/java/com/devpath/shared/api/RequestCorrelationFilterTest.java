package com.devpath.shared.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestCorrelationFilterTest {
    private final RequestCorrelationFilter filter = new RequestCorrelationFilter();

    @Test
    void propagatesSafeIdentifiersToTheRequestResponseAndLoggingContext() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/v1/dashboard/summary");
        request.addHeader(RequestIds.REQUEST_HEADER, "request-123");
        request.addHeader(RequestIds.CORRELATION_HEADER, "journey_456");
        var response = new MockHttpServletResponse();
        var observed = new AtomicReference<String>();

        filter.doFilter(request, response, (currentRequest, currentResponse) -> observed.set(
            RequestIds.resolve((MockHttpServletRequest) currentRequest) + ":"
                + RequestIds.resolveCorrelation((MockHttpServletRequest) currentRequest) + ":"
                + MDC.get("request_id") + ":" + MDC.get("correlation_id")));

        assertThat(observed).hasValue("request-123:journey_456:request-123:journey_456");
        assertThat(response.getHeader(RequestIds.REQUEST_HEADER)).isEqualTo("request-123");
        assertThat(response.getHeader(RequestIds.CORRELATION_HEADER)).isEqualTo("journey_456");
        assertThat(MDC.get("request_id")).isNull();
        assertThat(MDC.get("correlation_id")).isNull();
    }

    @Test
    void replacesUnsafeClientIdentifiersAndUsesTheRequestIdAsTheCorrelationFallback() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/v1/users/me");
        request.addHeader(RequestIds.REQUEST_HEADER, "unsafe\nlog-entry");
        request.addHeader(RequestIds.CORRELATION_HEADER, " ");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, (currentRequest, currentResponse) -> {});

        String requestId = response.getHeader(RequestIds.REQUEST_HEADER);
        assertThat(requestId).matches("[0-9a-f-]{36}").doesNotContain("unsafe");
        assertThat(response.getHeader(RequestIds.CORRELATION_HEADER)).isEqualTo(requestId);
    }
}
