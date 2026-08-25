package com.devpath.rule.application;

import com.devpath.repository.domain.RepositorySnapshot;
import com.devpath.rule.domain.DeterministicRuleEngine;
import com.devpath.rule.domain.RuleEvaluationResult;
import org.springframework.stereotype.Service;

@Service
public class RepositoryRuleEvaluationApplicationService {
    public static final String BASELINE_SCOPE = "REPOSITORY_BASELINE";

    private final RuleCatalogPort ruleCatalog;
    private final DeterministicRuleEngine engine = new DeterministicRuleEngine();

    public RepositoryRuleEvaluationApplicationService(RuleCatalogPort ruleCatalog) {
        this.ruleCatalog = ruleCatalog;
    }

    public RuleEvaluationResult evaluate(RepositorySnapshot snapshot) {
        var ruleSet = ruleCatalog.loadActive(BASELINE_SCOPE);
        return engine.evaluate(snapshot.id().toString(), RepositoryRuleEvidenceMapper.EXTRACTOR_VERSION,
            ruleSet, RepositoryRuleEvidenceMapper.map(snapshot));
    }
}
