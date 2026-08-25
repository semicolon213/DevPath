package com.devpath.repository.domain;

import java.util.List;

public record RepositoryTechnology(String name, String category, String evidenceLabel, List<String> evidencePaths) {
    public RepositoryTechnology {
        evidencePaths = List.copyOf(evidencePaths);
    }
}
