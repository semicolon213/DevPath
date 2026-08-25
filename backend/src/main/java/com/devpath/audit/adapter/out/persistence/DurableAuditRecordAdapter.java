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
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class DurableAuditRecordAdapter implements AuthenticationAuditPort, IntegrationAuditPort, RepositoryAuditPort, AnalysisAuditPort {
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
        save(event.name(), userId, "EXTERNAL_IDENTITY", provider.name(), outcome, occurredAt);
    }

    @Override
    public void record(IntegrationAuditEvent event, UUID userId, String resourceId, Instant occurredAt) {
        String outcome = switch (event) {
            case GITHUB_TOKEN_REFRESH_FAILED, GITHUB_PERMISSION_CHANGED -> "FAILED";
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
        String resourceType = event == AnalysisAuditEvent.ANALYSIS_RESULT_VIEWED ? "ANALYSIS" : "ANALYSIS_HISTORY";
        save(event.name(), userId, resourceType, resourceId, "SUCCEEDED", occurredAt);
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
