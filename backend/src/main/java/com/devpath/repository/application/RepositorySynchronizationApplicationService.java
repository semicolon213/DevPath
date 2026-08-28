package com.devpath.repository.application;

import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class RepositorySynchronizationApplicationService {
    private final RepositorySynchronizationTransaction transactions;
    private final Clock clock;

    public RepositorySynchronizationApplicationService(
        RepositorySynchronizationTransaction transactions,
        Clock clock
    ) {
        this.transactions = transactions;
        this.clock = clock;
    }

    public RepositorySyncJobView request(UUID userId, UUID repositoryId, String idempotencyKey) {
        String key = validateKey(idempotencyKey);
        return transactions.request(userId, repositoryId, key, clock.instant());
    }

    public RepositorySyncJobView getJob(UUID userId, UUID jobId) {
        return transactions.getJob(userId, jobId);
    }

    public RepositorySnapshotListView listSnapshots(UUID userId, UUID repositoryId) {
        return transactions.listSnapshots(userId, repositoryId);
    }

    public RepositorySnapshotView getSnapshot(UUID userId, UUID repositoryId, UUID snapshotId) {
        return transactions.getSnapshot(userId, repositoryId, snapshotId);
    }

    public TechnologySummaryView getTechnologies(UUID userId, UUID repositoryId) {
        return transactions.getTechnologies(userId, repositoryId);
    }

    public RepositoryEvidenceSummaryView getEvidence(UUID userId, UUID repositoryId) {
        return transactions.getEvidence(userId, repositoryId, clock.instant());
    }

    private String validateKey(String value) {
        if (value == null || value.isBlank() || value.length() > 128 || !value.matches("[A-Za-z0-9._:-]+")) {
            throw new IllegalArgumentException("Idempotency key is invalid");
        }
        return value;
    }
}
