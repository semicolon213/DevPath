package com.devpath.knowledge.application;

import java.util.List;
import java.util.UUID;

public record KnowledgeSearchFilters(List<String> sourceTypes, List<UUID> documentIds) {
    public KnowledgeSearchFilters {
        sourceTypes = sourceTypes == null || sourceTypes.isEmpty() ? List.of("NOTION") : List.copyOf(sourceTypes);
        documentIds = documentIds == null ? List.of() : List.copyOf(documentIds);
        if (!sourceTypes.equals(List.of("NOTION")) || documentIds.size() > 20
            || documentIds.stream().distinct().count() != documentIds.size()) {
            throw new IllegalArgumentException("Knowledge search filters are invalid");
        }
    }
}
