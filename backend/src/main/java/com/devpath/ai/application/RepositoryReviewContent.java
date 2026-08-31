package com.devpath.ai.application;

import java.util.List;
import java.util.UUID;

public record RepositoryReviewContent(String summary, List<Section> sections) {
    public RepositoryReviewContent { sections = sections == null ? null : List.copyOf(sections); }

    public record Section(String category, String review, List<UUID> evidenceIds) {
        public Section { evidenceIds = evidenceIds == null ? null : List.copyOf(evidenceIds); }
    }
}
