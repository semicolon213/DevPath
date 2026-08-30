package com.devpath.knowledge.application;

import java.util.List;

public record KnowledgeChunkSummaryListView(List<KnowledgeChunkSummaryView> chunks) {
    public KnowledgeChunkSummaryListView { chunks = List.copyOf(chunks); }
}
