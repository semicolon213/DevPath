package com.devpath.identity.application;

import com.devpath.identity.domain.PreferenceType;

public interface TargetCatalogPort {
    boolean supports(PreferenceType type, String targetId);
    String version();
}
