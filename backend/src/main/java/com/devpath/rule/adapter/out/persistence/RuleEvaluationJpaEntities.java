package com.devpath.rule.adapter.out.persistence;

import com.devpath.rule.domain.CompletedRuleEvaluation;
import com.devpath.rule.domain.RuleCategoryScore;
import com.devpath.rule.domain.RuleExecutionResult;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "evaluations")
class RuleEvaluationJpaEntity {
    @Id @Column(name = "evaluation_id", nullable = false, updatable = false) private UUID id;
    @Column(name = "user_id", nullable = false, updatable = false) private UUID userId;
    @Column(name = "snapshot_id", nullable = false, updatable = false) private UUID snapshotId;
    @Column(name = "rule_set_version_id", nullable = false, updatable = false) private UUID ruleSetVersionId;
    @Column(name = "status", nullable = false, length = 24) private String status;
    @Column(name = "input_hash", nullable = false, updatable = false, length = 64, columnDefinition = "CHAR(64)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private String inputHash;
    @Column(name = "rule_set_version_label", nullable = false, updatable = false, length = 32) private String ruleSetVersionLabel;
    @Column(name = "formula_library_version", nullable = false, updatable = false, length = 32) private String formulaLibraryVersion;
    @Column(name = "extractor_version", nullable = false, updatable = false, length = 64) private String extractorVersion;
    @Column(name = "overall_score", nullable = false, updatable = false, precision = 5, scale = 2) private BigDecimal overallScore;
    @Column(name = "confidence", nullable = false, updatable = false, precision = 5, scale = 2) private BigDecimal confidence;
    @Column(name = "started_at", nullable = false, updatable = false) private Instant startedAt;
    @Column(name = "completed_at", nullable = false, updatable = false) private Instant completedAt;

    protected RuleEvaluationJpaEntity() {}
    RuleEvaluationJpaEntity(CompletedRuleEvaluation value) {
        id = value.id(); userId = value.userId(); snapshotId = value.snapshotId(); ruleSetVersionId = value.ruleSetVersionId();
        status = "COMPLETED"; inputHash = value.inputHash(); ruleSetVersionLabel = value.result().ruleSetVersion();
        formulaLibraryVersion = value.result().formulaLibraryVersion(); extractorVersion = value.result().extractorVersion();
        overallScore = value.result().overallScore(); confidence = value.result().confidence();
        startedAt = value.startedAt(); completedAt = value.completedAt();
    }
    UUID id() { return id; } UUID userId() { return userId; } UUID snapshotId() { return snapshotId; }
    UUID ruleSetVersionId() { return ruleSetVersionId; } String inputHash() { return inputHash; }
    String ruleSetVersionLabel() { return ruleSetVersionLabel; } String formulaLibraryVersion() { return formulaLibraryVersion; }
    String extractorVersion() { return extractorVersion; } BigDecimal overallScore() { return overallScore; }
    BigDecimal confidence() { return confidence; } Instant startedAt() { return startedAt; } Instant completedAt() { return completedAt; }
}

@Entity
@Table(name = "evaluation_warnings")
class EvaluationWarningJpaEntity {
    @Id @Column(name = "evaluation_warning_id", nullable = false, updatable = false) private UUID id;
    @Column(name = "evaluation_id", nullable = false, updatable = false) private UUID evaluationId;
    @Column(name = "warning_order", nullable = false, updatable = false) private int orderIndex;
    @Column(name = "warning_message", nullable = false, updatable = false, length = 500) private String message;
    protected EvaluationWarningJpaEntity() {}
    EvaluationWarningJpaEntity(UUID evaluationId, int orderIndex, String message) {
        this.id = UUID.randomUUID(); this.evaluationId = evaluationId; this.orderIndex = orderIndex; this.message = message;
    }
    String message() { return message; }
}

@Entity
@Table(name = "category_evaluations")
class RuleCategoryEvaluationJpaEntity {
    @Id @Column(name = "category_evaluation_id", nullable = false, updatable = false) private UUID id;
    @Column(name = "evaluation_id", nullable = false, updatable = false) private UUID evaluationId;
    @Column(name = "category", nullable = false, updatable = false, length = 32) private String category;
    @Column(name = "score", nullable = false, updatable = false, precision = 5, scale = 2) private BigDecimal score;
    @Column(name = "weight", nullable = false, updatable = false, precision = 9, scale = 6) private BigDecimal weight;
    @Column(name = "confidence", nullable = false, updatable = false, precision = 5, scale = 2) private BigDecimal confidence;
    @Column(name = "evidence_count", nullable = false, updatable = false) private int evidenceCount;
    protected RuleCategoryEvaluationJpaEntity() {}
    RuleCategoryEvaluationJpaEntity(UUID evaluationId, RuleCategoryScore value) {
        id = UUID.randomUUID(); this.evaluationId = evaluationId; category = value.category().name(); score = value.score();
        weight = value.weight(); confidence = value.confidence();
        evidenceCount = Math.toIntExact(value.ruleResults().stream().flatMap(rule -> rule.evidenceReferences().stream()).distinct().count());
    }
    UUID id() { return id; } String category() { return category; } BigDecimal score() { return score; }
    BigDecimal weight() { return weight; } BigDecimal confidence() { return confidence; }
}

@Entity
@Table(name = "category_missing_evidence")
class CategoryMissingEvidenceJpaEntity {
    @Id @Column(name = "category_missing_evidence_id", nullable = false, updatable = false) private UUID id;
    @Column(name = "category_evaluation_id", nullable = false, updatable = false) private UUID categoryEvaluationId;
    @Column(name = "missing_order", nullable = false, updatable = false) private int orderIndex;
    @Column(name = "evidence_key", nullable = false, updatable = false, length = 128) private String evidenceKey;
    protected CategoryMissingEvidenceJpaEntity() {}
    CategoryMissingEvidenceJpaEntity(UUID categoryEvaluationId, int orderIndex, String evidenceKey) {
        id = UUID.randomUUID(); this.categoryEvaluationId = categoryEvaluationId; this.orderIndex = orderIndex; this.evidenceKey = evidenceKey;
    }
    String evidenceKey() { return evidenceKey; }
}

@Entity
@Table(name = "rule_execution_results")
class RuleExecutionResultJpaEntity {
    @Id @Column(name = "rule_execution_result_id", nullable = false, updatable = false) private UUID id;
    @Column(name = "evaluation_id", nullable = false, updatable = false) private UUID evaluationId;
    @Column(name = "rule_set_version_id", nullable = false, updatable = false) private UUID ruleSetVersionId;
    @Column(name = "rule_id", nullable = false, updatable = false, length = 64) private String ruleId;
    @Column(name = "rule_version", nullable = false, updatable = false, length = 32) private String ruleVersion;
    @Column(name = "category", nullable = false, updatable = false, length = 32) private String category;
    @Column(name = "outcome_status", nullable = false, updatable = false, length = 16) private String outcomeStatus;
    @Column(name = "raw_value", nullable = false, updatable = false, precision = 18, scale = 6) private BigDecimal rawValue;
    @Column(name = "score", nullable = false, updatable = false, precision = 5, scale = 2) private BigDecimal score;
    @Column(name = "weight", nullable = false, updatable = false, precision = 9, scale = 6) private BigDecimal weight;
    @Column(name = "formula_id", nullable = false, updatable = false, length = 80) private String formulaId;
    @Column(name = "calculation_trace", nullable = false, updatable = false, length = 1000) private String calculationTrace;
    @Column(name = "evidence_count", nullable = false, updatable = false) private int evidenceCount;
    protected RuleExecutionResultJpaEntity() {}
    RuleExecutionResultJpaEntity(UUID evaluationId, UUID ruleSetVersionId, RuleExecutionResult value) {
        id = UUID.randomUUID(); this.evaluationId = evaluationId; this.ruleSetVersionId = ruleSetVersionId;
        ruleId = value.ruleId(); ruleVersion = value.ruleVersion(); category = value.category().name(); outcomeStatus = value.status().name();
        rawValue = value.rawValue(); score = value.score(); weight = value.weight(); formulaId = value.formulaId();
        calculationTrace = value.trace(); evidenceCount = value.evidenceReferences().size();
    }
    UUID id() { return id; } String ruleId() { return ruleId; } String ruleVersion() { return ruleVersion; }
    String category() { return category; } String outcomeStatus() { return outcomeStatus; } BigDecimal rawValue() { return rawValue; }
    BigDecimal score() { return score; } BigDecimal weight() { return weight; } String formulaId() { return formulaId; }
    String calculationTrace() { return calculationTrace; }
}

@Entity
@Table(name = "evidence_records")
class RuleEvidenceJpaEntity {
    @Id @Column(name = "evidence_id", nullable = false, updatable = false) private UUID id;
    @Column(name = "user_id", nullable = false, updatable = false) private UUID userId;
    @Column(name = "snapshot_id", nullable = false, updatable = false) private UUID snapshotId;
    @Column(name = "evidence_type", nullable = false, updatable = false, length = 32) private String evidenceType;
    @Column(name = "source_reference", nullable = false, updatable = false, length = 1200) private String sourceReference;
    @Column(name = "source_reference_hash", nullable = false, updatable = false, length = 64, columnDefinition = "CHAR(64)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private String sourceReferenceHash;
    @Column(name = "observed_fact_summary", nullable = false, updatable = false, length = 500) private String observedFactSummary;
    @Column(name = "confidence", nullable = false, updatable = false, precision = 5, scale = 2) private BigDecimal confidence;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    protected RuleEvidenceJpaEntity() {}
    RuleEvidenceJpaEntity(UUID userId, UUID snapshotId, String type, String reference, String referenceHash,
                          String summary, BigDecimal confidence, Instant createdAt) {
        id = UUID.randomUUID(); this.userId = userId; this.snapshotId = snapshotId; evidenceType = type;
        sourceReference = reference; sourceReferenceHash = referenceHash; observedFactSummary = summary;
        this.confidence = confidence; this.createdAt = createdAt;
    }
    UUID id() { return id; } UUID userId() { return userId; } UUID snapshotId() { return snapshotId; }
    String evidenceType() { return evidenceType; } String sourceReference() { return sourceReference; }
    String observedFactSummary() { return observedFactSummary; } BigDecimal confidence() { return confidence; }
}

@Entity
@Table(name = "score_evidence_links")
class ScoreEvidenceLinkJpaEntity {
    @Id @Column(name = "score_evidence_link_id", nullable = false, updatable = false) private UUID id;
    @Column(name = "evidence_id", nullable = false, updatable = false) private UUID evidenceId;
    @Column(name = "evaluation_id", nullable = false, updatable = false) private UUID evaluationId;
    @Column(name = "rule_execution_result_id", nullable = false, updatable = false) private UUID ruleExecutionResultId;
    @Column(name = "contribution_role", nullable = false, updatable = false, length = 32) private String contributionRole;
    protected ScoreEvidenceLinkJpaEntity() {}
    ScoreEvidenceLinkJpaEntity(UUID evidenceId, UUID evaluationId, UUID ruleExecutionResultId) {
        id = UUID.randomUUID(); this.evidenceId = evidenceId; this.evaluationId = evaluationId;
        this.ruleExecutionResultId = ruleExecutionResultId; contributionRole = "DIRECT";
    }
    UUID evidenceId() { return evidenceId; } UUID ruleExecutionResultId() { return ruleExecutionResultId; }
    String contributionRole() { return contributionRole; }
}
