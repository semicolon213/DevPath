package com.devpath.audit.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "audit_records")
class AuditRecordJpaEntity {
    @Id
    @Column(name = "audit_record_id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "actor_user_id", updatable = false)
    private UUID actorUserId;

    @Column(name = "action_type", nullable = false, updatable = false, length = 96)
    private String actionType;

    @Column(name = "resource_type", nullable = false, updatable = false, length = 64)
    private String resourceType;

    @Column(name = "resource_id", updatable = false, length = 128)
    private String resourceId;

    @Column(name = "privacy_class", nullable = false, updatable = false, length = 32)
    private String privacyClass;

    @Column(name = "outcome", nullable = false, updatable = false, length = 32)
    private String outcome;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "details", nullable = false, updatable = false, columnDefinition = "jsonb")
    private Map<String, Object> details;

    protected AuditRecordJpaEntity() {
    }

    AuditRecordJpaEntity(
        UUID id,
        UUID actorUserId,
        String actionType,
        String resourceType,
        String resourceId,
        String privacyClass,
        String outcome,
        Instant occurredAt,
        Map<String, Object> details
    ) {
        this.id = id;
        this.actorUserId = actorUserId;
        this.actionType = actionType;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.privacyClass = privacyClass;
        this.outcome = outcome;
        this.occurredAt = occurredAt;
        this.details = Map.copyOf(details);
    }
}
