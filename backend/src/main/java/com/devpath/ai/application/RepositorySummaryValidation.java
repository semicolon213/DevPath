package com.devpath.ai.application;

import java.util.List;

public record RepositorySummaryValidation(RepositorySummaryContent content, List<String> violations) {
    public RepositorySummaryValidation { violations = List.copyOf(violations); }
    public boolean passed() { return content != null && violations.isEmpty(); }
}
