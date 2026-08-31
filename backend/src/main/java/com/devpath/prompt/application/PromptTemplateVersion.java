package com.devpath.prompt.application;

import java.util.UUID;

public record PromptTemplateVersion(
    UUID id, String taskType, String version, String systemPrompt, String outputFormatPrompt
) {}
