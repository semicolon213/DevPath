package com.devpath.analysis.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity(name = "AnalysisOutboxJpaEntity")
@Table(name = "outbox_events")
class AnalysisOutboxJpaEntity {
    @Id @Column(name = "event_id") private UUID id;
    @Column(name = "aggregate_type", nullable = false, updatable = false, length = 64) private String aggregateType;
    @Column(name = "aggregate_id", nullable = false, updatable = false) private UUID aggregateId;
    @Column(name = "event_type", nullable = false, updatable = false, length = 128) private String eventType;
    @Column(name = "payload_json", nullable = false, updatable = false, columnDefinition = "text") private String payloadJson;
    @Column(name = "occurred_at", nullable = false, updatable = false) private Instant occurredAt;
    @Column(name = "published_at") private Instant publishedAt;
    @Column(name = "attempt_count", nullable = false) private int attemptCount;

    protected AnalysisOutboxJpaEntity() {}
    AnalysisOutboxJpaEntity(String aggregateType, UUID aggregateId, String eventType, String payload, Instant occurredAt) {
        id = UUID.randomUUID(); this.aggregateType = aggregateType; this.aggregateId = aggregateId;
        this.eventType = eventType; payloadJson = payload; this.occurredAt = occurredAt; attemptCount = 0;
    }
}
