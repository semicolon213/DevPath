package com.devpath.audit.adapter.out.persistence;

import com.devpath.identity.application.AuthenticationAuditEvent;
import com.devpath.identity.application.AuthenticationAuditPort;
import com.devpath.identity.domain.OAuthProvider;
import com.devpath.integration.application.IntegrationAuditEvent;
import com.devpath.integration.application.IntegrationAuditPort;
import com.devpath.repository.application.RepositoryAuditEvent;
import com.devpath.repository.application.RepositoryAuditPort;
import com.devpath.analysis.application.AnalysisAuditEvent;
import com.devpath.analysis.application.AnalysisAuditPort;
import com.devpath.dashboard.application.DashboardAuditEvent;
import com.devpath.dashboard.application.DashboardAuditPort;
import com.devpath.learning.application.LearningAuditEvent;
import com.devpath.learning.application.LearningAuditPort;
import com.devpath.rule.application.SkillMatrixAuditEvent;
import com.devpath.rule.application.SkillMatrixAuditPort;
import com.devpath.onboarding.application.OnboardingAuditEvent;
import com.devpath.onboarding.application.OnboardingAuditPort;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class DurableAuditRecordAdapter implements AuthenticationAuditPort, IntegrationAuditPort, RepositoryAuditPort,
    AnalysisAuditPort, DashboardAuditPort, LearningAuditPort, SkillMatrixAuditPort, OnboardingAuditPort {
    private static final String PRIVACY_CLASS = "AUDIT_RESTRICTED";
    private final AuditRecordJpaRepository repository;

    DurableAuditRecordAdapter(AuditRecordJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void record(
        AuthenticationAuditEvent event,
        UUID userId,
        OAuthProvider provider,
        Instant occurredAt
    ) {
        String outcome = switch (event) {
            case LOGIN_FAILED -> "FAILED";
            case AUTHENTICATION_REJECTED_DISABLED_ACCOUNT -> "DENIED";
            default -> "SUCCEEDED";
        };
        String resourceType = switch (event) {
            case LOGOUT_SUCCEEDED, SESSION_ABSOLUTE_TIMEOUT -> "SESSION";
            default -> "EXTERNAL_IDENTITY";
        };
        save(event.name(), userId, resourceType, provider.name(), outcome, occurredAt);
    }

    @Override
    public void record(IntegrationAuditEvent event, UUID userId, String resourceId, Instant occurredAt) {
        String outcome = switch (event) {
            case GITHUB_TOKEN_REFRESH_FAILED, GITHUB_PERMISSION_CHANGED, GITHUB_RATE_LIMITED -> "FAILED";
            default -> "SUCCEEDED";
        };
        save(event.name(), userId, "PROVIDER_CONNECTION", resourceId, outcome, occurredAt);
    }

    @Override
    public void record(RepositoryAuditEvent event, UUID userId, UUID repositoryId, Instant occurredAt) {
        save(event.name(), userId, "REPOSITORY", repositoryId.toString(), "SUCCEEDED", occurredAt);
    }

    @Override
    public void record(AnalysisAuditEvent event, UUID userId, String resourceId, Instant occurredAt) {
        String resourceType = switch (event) {
            case ANALYSIS_RESULT_VIEWED -> "ANALYSIS";
            case ANALYSIS_HISTORY_VIEWED -> "ANALYSIS_HISTORY";
            case ANALYSES_COMPARED -> "ANALYSIS_COMPARISON";
        };
        save(event.name(), userId, resourceType, resourceId, "SUCCEEDED", occurredAt);
    }

    @Override
    public void record(DashboardAuditEvent event, UUID userId, Instant occurredAt) {
        save(event.name(), userId, "DASHBOARD", "CURRENT", "SUCCEEDED", occurredAt);
    }

    @Override
    public void record(OnboardingAuditEvent event, UUID userId, Instant occurredAt) {
        save(event.name(), userId, "ONBOARDING_PROGRESS", "CURRENT", "SUCCEEDED", occurredAt);
    }

    @Override
    public void record(LearningAuditEvent event, UUID userId, UUID roadmapId, Instant occurredAt) {
        save(event.name(), userId, "LEARNING_ROADMAP", roadmapId.toString(), "SUCCEEDED", occurredAt);
    }

    @Override
    public void record(SkillMatrixAuditEvent event, UUID userId, List<UUID> matrixIds, Instant occurredAt) {
        String resourceId = matrixIds.stream().map(UUID::toString).collect(java.util.stream.Collectors.joining(","));
        save(event.name(), userId, "SKILL_MATRIX_COMPARISON", resourceId, "SUCCEEDED", occurredAt);
    }

    @Override
    public void record(SkillMatrixAuditEvent event, UUID userId, UUID skillId, Instant occurredAt) {
        String resourceType = switch (event) {
            case SKILL_DETAIL_VIEWED -> "SKILL";
            case SKILL_EVIDENCE_VIEWED -> "SKILL_EVIDENCE";
            case SKILL_MATRICES_COMPARED -> "SKILL_MATRIX_COMPARISON";
        };
        save(event.name(), userId, resourceType, skillId.toString(), "SUCCEEDED", occurredAt);
    }

    private void save(
        String actionType,
        UUID userId,
        String resourceType,
        String resourceId,
        String outcome,
        Instant occurredAt
    ) {
        repository.save(new AuditRecordJpaEntity(
            UUID.randomUUID(), userId, actionType, resourceType, resourceId,
            PRIVACY_CLASS, outcome, occurredAt
        ));
    }
}
