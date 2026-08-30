package com.devpath.knowledge.application;

import java.util.List;

public record KnowledgeDocumentListView(List<KnowledgeDocumentView> documents) {
    public KnowledgeDocumentListView { documents = List.copyOf(documents); }
}
