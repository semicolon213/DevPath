package com.devpath.ai.application;

import java.util.List;

public record RepositoryReviewValidation(RepositoryReviewContent content, List<String> violations) {
    public RepositoryReviewValidation { violations = List.copyOf(violations); }
    public boolean passed() { return content != null && violations.isEmpty(); }
}
