package com.devpath.rule.application;

import com.devpath.rule.domain.RuleSetVersion;

public interface RuleCatalogPort {
    RuleSetVersion loadActive(String scope);
}
