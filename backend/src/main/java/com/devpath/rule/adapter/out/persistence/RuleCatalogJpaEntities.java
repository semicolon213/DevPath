package com.devpath.rule.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "rule_sets")
class RuleSetJpaEntity {
    @Id @Column(name = "rule_set_id", nullable = false, updatable = false) private UUID id;
    @Column(name = "name", nullable = false, length = 120) private String name;
    @Column(name = "scope", nullable = false, length = 64) private String scope;
    @Column(name = "status", nullable = false, length = 24) private String status;
    @Column(name = "active_rule_set_version_id") private UUID activeVersionId;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    protected RuleSetJpaEntity() {}
    UUID id() { return id; }
    UUID activeVersionId() { return activeVersionId; }
}

@Entity
@Table(name = "rule_set_versions")
class RuleSetVersionJpaEntity {
    @Id @Column(name = "rule_set_version_id", nullable = false, updatable = false) private UUID id;
    @Column(name = "rule_set_id", nullable = false, updatable = false) private UUID ruleSetId;
    @Column(name = "version_label", nullable = false, length = 32) private String versionLabel;
    @Column(name = "formula_library_version", nullable = false, length = 32) private String formulaLibraryVersion;
    @Column(name = "required_extractor_version", nullable = false, length = 64) private String requiredExtractorVersion;
    @Column(name = "status", nullable = false, length = 24) private String status;
    @Column(name = "validation_status", nullable = false, length = 24) private String validationStatus;
    @Column(name = "effective_at", nullable = false) private Instant effectiveAt;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    protected RuleSetVersionJpaEntity() {}
    UUID id() { return id; }
    UUID ruleSetId() { return ruleSetId; }
    String versionLabel() { return versionLabel; }
    String formulaLibraryVersion() { return formulaLibraryVersion; }
    String requiredExtractorVersion() { return requiredExtractorVersion; }
    String status() { return status; }
    String validationStatus() { return validationStatus; }
}

@Entity
@Table(name = "rule_category_weights")
@IdClass(RuleCategoryWeightId.class)
class RuleCategoryWeightJpaEntity {
    @Id @Column(name = "rule_set_version_id", nullable = false, updatable = false) private UUID ruleSetVersionId;
    @Id @Column(name = "category", nullable = false, updatable = false, length = 32) private String category;
    @Column(name = "weight", nullable = false, precision = 9, scale = 6) private BigDecimal weight;

    protected RuleCategoryWeightJpaEntity() {}
    String category() { return category; }
    BigDecimal weight() { return weight; }
}

@Entity
@Table(name = "rules")
@IdClass(RuleDefinitionId.class)
class RuleDefinitionJpaEntity {
    @Id @Column(name = "rule_set_version_id", nullable = false, updatable = false) private UUID ruleSetVersionId;
    @Id @Column(name = "rule_id", nullable = false, updatable = false, length = 64) private String ruleId;
    @Column(name = "rule_version", nullable = false, length = 32) private String ruleVersion;
    @Column(name = "category", nullable = false, length = 32) private String category;
    @Column(name = "name", nullable = false, length = 160) private String name;
    @Column(name = "description", nullable = false, length = 500) private String description;
    @Column(name = "priority", nullable = false) private int priority;
    @Column(name = "evidence_signal_key", nullable = false, length = 64) private String evidenceSignalKey;
    @Column(name = "formula_id", nullable = false, length = 32) private String formulaId;
    @Column(name = "formula_parameter", nullable = false, precision = 18, scale = 6) private BigDecimal formulaParameter;
    @Column(name = "weight", nullable = false, precision = 9, scale = 6) private BigDecimal weight;
    @Column(name = "missing_data_policy", nullable = false, length = 16) private String missingDataPolicy;
    @Column(name = "enabled", nullable = false) private boolean enabled;

    protected RuleDefinitionJpaEntity() {}
    String ruleId() { return ruleId; }
    String ruleVersion() { return ruleVersion; }
    String category() { return category; }
    String name() { return name; }
    int priority() { return priority; }
    String evidenceSignalKey() { return evidenceSignalKey; }
    String formulaId() { return formulaId; }
    BigDecimal formulaParameter() { return formulaParameter; }
    BigDecimal weight() { return weight; }
    String missingDataPolicy() { return missingDataPolicy; }
    boolean enabled() { return enabled; }
}
