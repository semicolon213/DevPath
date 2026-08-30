package com.devpath.knowledge.application;

import java.util.List;

public record EmbeddingVector(String provider, String model, String modelVersion, int dimension, List<Double> values) {
    public EmbeddingVector {
        values = List.copyOf(values);
        if (dimension != values.size()) throw new IllegalArgumentException("Embedding dimension does not match values");
    }
}
