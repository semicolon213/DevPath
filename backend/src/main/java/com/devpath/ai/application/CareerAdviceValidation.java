package com.devpath.ai.application;

import java.util.List;

public record CareerAdviceValidation(CareerAdviceContent content, List<String> violations) {
    public CareerAdviceValidation { violations = List.copyOf(violations); }
}
