package com.devpath.identity.application;

import java.time.Instant;

public record UserPreferenceView(String careerId, String companyId, Instant updatedAt) {}
