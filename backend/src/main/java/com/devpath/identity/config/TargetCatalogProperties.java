package com.devpath.identity.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("devpath.targets")
public record TargetCatalogProperties(@NotBlank String version, @NotEmpty List<String> careers, @NotEmpty List<String> companies) {}
