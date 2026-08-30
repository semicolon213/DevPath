package com.devpath.knowledge.application;

public enum KnowledgeAuditEvent {
    INGESTION_REQUESTED,
    INGESTION_COMPLETED,
    INGESTION_FAILED,
    DOCUMENT_VIEWED,
    DOCUMENT_ARCHIVED,
    DOCUMENT_REINDEX_REQUESTED,
    KNOWLEDGE_RETRIEVED
}
