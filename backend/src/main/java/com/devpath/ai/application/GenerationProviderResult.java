package com.devpath.ai.application;

public record GenerationProviderResult(
    String content, Integer promptTokens, Integer completionTokens
) {}
