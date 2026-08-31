package com.devpath.ai.application;

import java.time.Instant;
import java.util.List;

public record ResponseValidationView(
    String status, String validatorVersion, Instant validatedAt, List<String> violations
) {
    public ResponseValidationView {
        violations = List.copyOf(violations);
    }
}
