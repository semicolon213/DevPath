package com.devpath.identity.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("devpath.targets")
public record TargetCatalogProperties(@NotBlank String version) {}
