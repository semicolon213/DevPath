package com.devpath.rule.adapter.out.persistence;

import com.devpath.rule.application.RuleCatalogPort;
import com.devpath.rule.domain.MissingDataPolicy;
import com.devpath.rule.domain.RuleCategory;
import com.devpath.rule.domain.RuleDefinition;
import com.devpath.rule.domain.RuleFormula;
import com.devpath.rule.domain.RuleSetVersion;
import java.util.EnumMap;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class JpaRuleCatalogAdapter implements RuleCatalogPort {
    private final RuleSetJpaRepository ruleSets;
    private final RuleSetVersionJpaRepository versions;
    private final RuleCategoryWeightJpaRepository categoryWeights;
    private final RuleDefinitionJpaRepository rules;

    JpaRuleCatalogAdapter(
        RuleSetJpaRepository ruleSets,
        RuleSetVersionJpaRepository versions,
        RuleCategoryWeightJpaRepository categoryWeights,
        RuleDefinitionJpaRepository rules
    ) {
        this.ruleSets = ruleSets;
        this.versions = versions;
        this.categoryWeights = categoryWeights;
        this.rules = rules;
    }

    @Override
    @Transactional(readOnly = true)
    public RuleSetVersion loadActive(String scope) {
        RuleSetJpaEntity ruleSet = ruleSets.findByScopeAndStatus(scope, "ACTIVE")
            .orElseThrow(() -> new IllegalStateException("No active rule set for scope " + scope));
        if (ruleSet.activeVersionId() == null) throw new IllegalStateException("Active rule set has no active version");
        RuleSetVersionJpaEntity version = versions.findById(ruleSet.activeVersionId())
            .orElseThrow(() -> new IllegalStateException("Active rule-set version does not exist"));
        if (!version.ruleSetId().equals(ruleSet.id()) || !"ACTIVE".equals(version.status())
            || !"VALID".equals(version.validationStatus())) {
            throw new IllegalStateException("Active rule-set version is not valid for execution");
        }
        var weights = new EnumMap<RuleCategory, java.math.BigDecimal>(RuleCategory.class);
        categoryWeights.findAllByRuleSetVersionId(version.id())
            .forEach(value -> weights.put(RuleCategory.valueOf(value.category()), value.weight()));
        var definitions = rules.findAllByRuleSetVersionIdOrderByPriorityAscRuleIdAsc(version.id()).stream()
            .map(value -> new RuleDefinition(value.ruleId(), value.ruleVersion(), RuleCategory.valueOf(value.category()),
                value.name(), value.priority(), value.evidenceSignalKey(), RuleFormula.valueOf(value.formulaId()),
                value.formulaParameter(), value.weight(), MissingDataPolicy.valueOf(value.missingDataPolicy()), value.enabled()))
            .toList();
        return new RuleSetVersion(ruleSet.id().toString(), version.id().toString(), version.versionLabel(),
            version.formulaLibraryVersion(), version.requiredExtractorVersion(), weights, definitions);
    }
}
