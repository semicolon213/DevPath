package com.devpath.prompt.application;

public record PromptPackage(String contextPayload, String prompt, String contextHash, int tokenBudget) {}
