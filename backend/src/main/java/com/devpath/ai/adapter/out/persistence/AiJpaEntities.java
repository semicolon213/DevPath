package com.devpath.ai.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "prompt_template_versions")
class PromptTemplateVersionJpaEntity {
    @Id @Column(name = "prompt_template_version_id") UUID id;
    @Column(name = "task_type", nullable = false) String taskType;
    @Column(name = "version_label", nullable = false) String versionLabel;
    @Column(nullable = false) String status;
    @Column(name = "system_prompt", nullable = false) String systemPrompt;
    @Column(name = "output_format_prompt", nullable = false) String outputFormatPrompt;
    @Column(name = "created_at", nullable = false) Instant createdAt;
    protected PromptTemplateVersionJpaEntity() {}
}

@Entity
@Table(name = "prompt_contexts")
class PromptContextJpaEntity {
    @Id @Column(name = "prompt_context_id") UUID id;
    @Column(name = "user_id", nullable = false) UUID userId;
    @Column(name = "prompt_template_version_id", nullable = false) UUID templateVersionId;
    @Column(name = "skill_matrix_id", nullable = false) UUID skillMatrixId;
    @Column(name = "analysis_id") UUID analysisId;
    @Column(name = "task_type", nullable = false) String taskType;
    @Column(name = "token_budget", nullable = false) int tokenBudget;
    @Column(name = "context_hash", nullable = false) String contextHash;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "context_payload", nullable = false, columnDefinition = "jsonb") String contextPayload;
    @Column(name = "provider_prompt", nullable = false) String providerPrompt;
    @Column(nullable = false) String status;
    @Column(name = "locked_at", nullable = false) Instant lockedAt;
    protected PromptContextJpaEntity() {}
    PromptContextJpaEntity(com.devpath.ai.application.StoredPromptContext value) {
        id = value.id(); userId = value.userId(); templateVersionId = value.templateVersionId();
        skillMatrixId = value.skillMatrixId(); analysisId = value.analysisId(); taskType = value.taskType();
        tokenBudget = value.tokenBudget();
        contextHash = value.contextHash(); contextPayload = value.contextPayload(); providerPrompt = value.prompt();
        status = "LOCKED"; lockedAt = value.lockedAt();
    }
}

@Entity
@Table(name = "ai_tasks")
class AiTaskJpaEntity {
    @Id @Column(name = "ai_task_id") UUID id;
    @Column(name = "user_id", nullable = false) UUID userId;
    @Column(name = "prompt_context_id", nullable = false) UUID promptContextId;
    @Column(name = "idempotency_key", nullable = false) String idempotencyKey;
    @Column(name = "task_type", nullable = false) String taskType;
    @Column(nullable = false) String status;
    @Column(name = "validation_status", nullable = false) String validationStatus;
    @Column(name = "attempt_count", nullable = false) int attemptCount;
    @Column(name = "failure_code") String failureCode;
    @Version @Column(nullable = false) long version;
    @Column(name = "requested_at", nullable = false) Instant requestedAt;
    @Column(name = "started_at") Instant startedAt;
    @Column(name = "completed_at") Instant completedAt;
    protected AiTaskJpaEntity() {}
}

@Entity
@Table(name = "model_executions")
class ModelExecutionJpaEntity {
    @Id @Column(name = "model_execution_id") UUID id;
    @Column(name = "ai_task_id", nullable = false) UUID taskId;
    @Column(nullable = false) String provider;
    @Column(name = "model_identifier", nullable = false) String model;
    @Column(name = "attempt_number", nullable = false) int attemptNumber;
    @Column(nullable = false) String status;
    @Column(name = "latency_ms") Long latencyMs;
    @Column(name = "prompt_tokens") Integer promptTokens;
    @Column(name = "completion_tokens") Integer completionTokens;
    @Column(name = "submitted_at", nullable = false) Instant submittedAt;
    @Column(name = "completed_at") Instant completedAt;
    @Column(name = "failure_code") String failureCode;
    protected ModelExecutionJpaEntity() {}
}

@Entity
@Table(name = "ai_responses")
class AiResponseJpaEntity {
    @Id @Column(name = "ai_response_id") UUID id;
    @Column(name = "model_execution_id", nullable = false) UUID modelExecutionId;
    @Column(name = "prompt_context_id", nullable = false) UUID promptContextId;
    @Column(name = "validation_status", nullable = false) String validationStatus;
    @Column(name = "response_content_reference", nullable = false) String contentReference;
    @Column(name = "received_at", nullable = false) Instant receivedAt;
    protected AiResponseJpaEntity() {}
}

@Entity
@Table(name = "response_validation_results")
class ResponseValidationResultJpaEntity {
    @Id @Column(name = "response_validation_result_id") UUID id;
    @Column(name = "ai_response_id", nullable = false) UUID responseId;
    @Column(nullable = false) String status;
    @Column(name = "validator_version", nullable = false) String validatorVersion;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false, columnDefinition = "jsonb") List<String> violations;
    @Column(name = "validated_at", nullable = false) Instant validatedAt;
    protected ResponseValidationResultJpaEntity() {}
}

@Entity
@Table(name = "generated_artifacts")
class GeneratedArtifactJpaEntity {
    @Id @Column(name = "generated_artifact_id") UUID id;
    @Column(name = "user_id", nullable = false) UUID userId;
    @Column(name = "ai_response_id", nullable = false) UUID responseId;
    @Column(name = "prompt_context_id", nullable = false) UUID promptContextId;
    @Column(name = "artifact_type", nullable = false) String artifactType;
    @Column(nullable = false) String status;
    @Column(name = "content_reference", nullable = false) String contentReference;
    @Column(name = "created_at", nullable = false) Instant createdAt;
    protected GeneratedArtifactJpaEntity() {}
}
