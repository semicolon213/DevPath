package com.devpath.knowledge.application;

public class KnowledgeRetrievalUnavailableException extends RuntimeException {
    public KnowledgeRetrievalUnavailableException(Throwable cause) {
        super("Knowledge retrieval is temporarily unavailable", cause);
    }
}
