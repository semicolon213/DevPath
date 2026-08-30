package com.devpath.knowledge.application;

public interface EmbeddingPort {
    EmbeddingVector embed(String content);
}
