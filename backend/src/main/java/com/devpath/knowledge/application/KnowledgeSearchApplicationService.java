package com.devpath.knowledge.application;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeSearchApplicationService {
    static final String POLICY_VERSION = "knowledge-semantic-v1";
    private static final Logger log = LoggerFactory.getLogger(KnowledgeSearchApplicationService.class);
    private final EmbeddingPort embeddings;
    private final ObjectContentPort objects;
    private final KnowledgeSearchRecordService records;
    private final Clock clock;
    private final double minimumRelevance;

    public KnowledgeSearchApplicationService(EmbeddingPort embeddings, ObjectContentPort objects,
        KnowledgeSearchRecordService records, Clock clock,
        @Value("${devpath.knowledge.retrieval.minimum-relevance:0.25}") double minimumRelevance) {
        this.embeddings = embeddings;
        this.objects = objects;
        this.records = records;
        this.clock = clock;
        if (!Double.isFinite(minimumRelevance) || minimumRelevance < 0 || minimumRelevance > 1) {
            throw new IllegalArgumentException("Knowledge minimum relevance is invalid");
        }
        this.minimumRelevance = minimumRelevance;
    }

    public KnowledgeSearchView search(UUID userId, String query, KnowledgeSearchFilters filters,
        Integer requestedLimit, String requestedPurpose) {
        String normalizedQuery = normalizeQuery(query);
        int limit = requestedLimit == null ? 5 : requestedLimit;
        if (limit < 1 || limit > 20) throw new IllegalArgumentException("Knowledge search limit is invalid");
        String purpose = requestedPurpose == null || requestedPurpose.isBlank() ? "USER_SEARCH" : requestedPurpose;
        if (!"USER_SEARCH".equals(purpose)) throw new IllegalArgumentException("Knowledge search purpose is invalid");
        KnowledgeSearchFilters appliedFilters = filters == null ? new KnowledgeSearchFilters(null, null) : filters;

        long started = System.nanoTime();
        try {
            EmbeddingVector queryEmbedding = embeddings.embed(normalizedQuery);
            List<KnowledgeSearchCandidate> candidates = records.retrieve(
                userId, queryEmbedding, appliedFilters, limit, minimumRelevance);
            candidates = records.retainAuthorized(userId, candidates);
            var results = new ArrayList<KnowledgeSearchResultItemView>();
            for (KnowledgeSearchCandidate candidate : candidates) {
                String excerpt = excerpt(objects.read(userId, candidate.objectReference()));
                results.add(new KnowledgeSearchResultItemView(candidate.chunkId(), candidate.documentId(),
                    candidate.documentTitle(), "NOTION", candidate.sourceObjectId(), candidate.sourceUrl(),
                    candidate.heading(), excerpt, clamp(candidate.relevance()), candidate.tokenEstimate(), "FRESH"));
            }
            long durationMs = Math.max(0, (System.nanoTime() - started) / 1_000_000L);
            var completedAt = clock.instant();
            UUID requestId = UUID.randomUUID();
            UUID resultId = UUID.randomUUID();
            var recordItems = new ArrayList<KnowledgeRetrievalRecord.Item>();
            for (int index = 0; index < results.size(); index++) {
                var item = results.get(index);
                recordItems.add(new KnowledgeRetrievalRecord.Item(index, item.chunkId(), item.relevance()));
            }
            records.record(new KnowledgeRetrievalRecord(requestId, resultId, userId,
                KnowledgeChunker.hash(normalizedQuery), purpose, appliedFilters, limit, POLICY_VERSION,
                durationMs, completedAt, recordItems));
            log.info("Knowledge retrieval completed retrieval_result_id={} retrieval_type=SEMANTIC result_count={} duration_ms={} policy_version={}",
                resultId, results.size(), durationMs, POLICY_VERSION);
            return new KnowledgeSearchView(resultId, "SEMANTIC", POLICY_VERSION, purpose, appliedFilters,
                results, results.size(), durationMs, completedAt);
        } catch (RuntimeException exception) {
            throw new KnowledgeRetrievalUnavailableException(exception);
        }
    }

    private static String normalizeQuery(String query) {
        if (query == null) throw new IllegalArgumentException("Knowledge search query is required");
        String normalized = query.strip().replaceAll("\\s+", " ");
        if (normalized.isBlank() || normalized.length() > 500) {
            throw new IllegalArgumentException("Knowledge search query is invalid");
        }
        return normalized;
    }

    private static String excerpt(String content) {
        String normalized = content == null ? "" : content.strip();
        if (normalized.isBlank()) throw new IllegalStateException("Knowledge chunk content is unavailable");
        return normalized.substring(0, Math.min(normalized.length(), 500));
    }

    private static double clamp(double relevance) {
        if (!Double.isFinite(relevance)) throw new IllegalStateException("Knowledge relevance is invalid");
        return Math.max(0d, Math.min(1d, relevance));
    }
}
