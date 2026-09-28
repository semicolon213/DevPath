package com.devpath.audit.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.devpath.ai.application.AiAuditEvent;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class DurableAuditRecordAdapterTest {
    @Test
    void aiAuditRecordsContainOnlyNormalizedMetadata() {
        var repository = Mockito.mock(AuditRecordJpaRepository.class);
        var adapter = new DurableAuditRecordAdapter(repository);
        UUID owner = UUID.randomUUID(), task = UUID.randomUUID(), artifact = UUID.randomUUID();
        Instant now = Instant.parse("2026-09-13T00:00:00Z");

        adapter.record(AiAuditEvent.GENERATION_REQUESTED, owner, task, now);
        adapter.record(AiAuditEvent.GENERATION_COMPLETED, owner, artifact, now);
        adapter.record(AiAuditEvent.GENERATION_FAILED, owner, task, now);

        ArgumentCaptor<AuditRecordJpaEntity> records = ArgumentCaptor.forClass(AuditRecordJpaEntity.class);
        verify(repository, Mockito.times(3)).save(records.capture());
        assertThat(records.getAllValues()).extracting(record -> field(record, "actionType"))
            .containsExactly("GENERATION_REQUESTED", "GENERATION_COMPLETED", "GENERATION_FAILED");
        assertThat(records.getAllValues()).extracting(record -> field(record, "outcome"))
            .containsExactly("SUCCEEDED", "SUCCEEDED", "FAILED");
        assertThat(records.getAllValues()).extracting(record -> field(record, "actorUserId"))
            .containsOnly(owner);
        assertThat(records.getAllValues()).extracting(record -> field(record, "resourceType"))
            .containsExactly("AI_TASK", "GENERATED_ARTIFACT", "AI_TASK");
        assertThat(records.getAllValues()).extracting(record -> field(record, "resourceId"))
            .containsExactly(task.toString(), artifact.toString(), task.toString());
        assertThat(records.getAllValues()).extracting(record -> field(record, "privacyClass"))
            .containsOnly("AUDIT_RESTRICTED");
        assertThat(records.getAllValues()).extracting(record -> field(record, "occurredAt"))
            .containsOnly(now);
        assertThat(records.getAllValues()).allSatisfy(record -> {
            assertThat(field(record, "details")).isEqualTo(Map.of());
        });
    }

    private static Object field(Object target, String name) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(target);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }
}
