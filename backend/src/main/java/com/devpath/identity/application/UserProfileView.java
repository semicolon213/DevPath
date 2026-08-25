package com.devpath.identity.application;

import com.devpath.identity.domain.CareerStage;
import java.time.Instant;
import java.util.UUID;

public record UserProfileView(UUID profileId, String displayName, CareerStage careerStage, String bio, Instant updatedAt) {}
