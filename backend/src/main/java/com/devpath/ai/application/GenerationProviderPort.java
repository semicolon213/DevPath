package com.devpath.ai.application;

public interface GenerationProviderPort {
    GenerationProviderResult generate(String prompt);
}
