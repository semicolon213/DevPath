package com.devpath.knowledge.adapter.out.persistence;

import com.devpath.knowledge.application.EmbeddingVector;
import com.devpath.knowledge.application.KnowledgeRetrievalRecord;
import com.devpath.knowledge.application.KnowledgeSearchCandidate;
import com.devpath.knowledge.application.KnowledgeSearchFilters;
import com.devpath.knowledge.application.KnowledgeSearchPersistencePort;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcKnowledgeSearchPersistenceAdapter implements KnowledgeSearchPersistencePort {
    private final JdbcTemplate jdbc;

    public JdbcKnowledgeSearchPersistenceAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Set<UUID> findAuthorizedCurrentChunkIds(UUID userId, List<UUID> chunkIds) {
        if (chunkIds.isEmpty()) return Set.of();
        String placeholders = chunkIds.stream().map(ignored -> "?").collect(Collectors.joining(","));
        String sql = "SELECT c.knowledge_chunk_id FROM knowledge_chunks c" +
            " JOIN knowledge_document_versions v ON v.knowledge_document_version_id=c.knowledge_document_version_id AND v.user_id=c.user_id" +
            " JOIN knowledge_documents d ON d.knowledge_document_id=v.knowledge_document_id AND d.user_id=v.user_id" +
            "   AND d.current_version_id=v.knowledge_document_version_id" +
            " JOIN notion_workspace_connections nc ON nc.notion_connection_id=d.source_connection_id AND nc.user_id=d.user_id" +
            " JOIN notion_page_metadata pm ON pm.notion_connection_id=d.source_connection_id AND pm.user_id=d.user_id" +
            "   AND pm.provider_page_id=d.source_object_id" +
            " WHERE c.user_id=? AND c.knowledge_chunk_id IN (" + placeholders + ")" +
            " AND d.lifecycle_status='ACTIVE' AND v.version_status='INDEXED' AND c.index_status='INDEXED'" +
            " AND nc.connection_status='ACTIVE' AND pm.in_trash=false AND pm.object_type='PAGE'" +
            " AND pm.last_edited_at<=v.source_updated_at";
        var arguments = new ArrayList<Object>();
        arguments.add(userId);
        arguments.addAll(chunkIds);
        return Set.copyOf(jdbc.query(sql, (result, row) -> result.getObject(1, UUID.class), arguments.toArray()));
    }

    @Override
    public List<KnowledgeSearchCandidate> search(UUID userId, EmbeddingVector queryEmbedding,
        KnowledgeSearchFilters filters, int limit, double minimumRelevance) {
        String documentFilter = filters.documentIds().isEmpty() ? "" :
            " AND d.knowledge_document_id IN (" + filters.documentIds().stream().map(ignored -> "?")
                .collect(Collectors.joining(",")) + ")";
        String sql = "WITH authorized_candidates AS (" +
            " SELECT c.knowledge_chunk_id,c.object_reference,c.heading,c.token_estimate," +
            " d.knowledge_document_id,d.title,d.source_object_id,pm.provider_url,v.source_updated_at," +
            " e.embedding <=> CAST(? AS vector) AS distance" +
            " FROM embedding_records e" +
            " JOIN knowledge_chunks c ON c.knowledge_chunk_id=e.knowledge_chunk_id AND c.user_id=e.user_id" +
            " JOIN knowledge_document_versions v ON v.knowledge_document_version_id=c.knowledge_document_version_id AND v.user_id=c.user_id" +
            " JOIN knowledge_documents d ON d.knowledge_document_id=v.knowledge_document_id AND d.user_id=v.user_id" +
            "   AND d.current_version_id=v.knowledge_document_version_id" +
            " JOIN notion_workspace_connections nc ON nc.notion_connection_id=d.source_connection_id AND nc.user_id=d.user_id" +
            " JOIN notion_page_metadata pm ON pm.notion_connection_id=d.source_connection_id AND pm.user_id=d.user_id" +
            "   AND pm.provider_page_id=d.source_object_id" +
            " WHERE e.user_id=? AND d.source_type='NOTION' AND d.lifecycle_status='ACTIVE'" +
            " AND v.version_status='INDEXED' AND c.index_status='INDEXED' AND e.embedding_status='ACTIVE'" +
            " AND nc.connection_status='ACTIVE' AND pm.in_trash=false AND pm.object_type='PAGE'" +
            " AND pm.last_edited_at<=v.source_updated_at" +
            " AND e.provider=? AND e.model=? AND e.model_version=? AND e.dimension=?" + documentFilter +
            ") SELECT *,1-distance AS relevance FROM authorized_candidates" +
            " WHERE 1-distance>=? ORDER BY distance ASC,knowledge_chunk_id ASC LIMIT ?";
        String vector = queryEmbedding.values().stream().map(String::valueOf)
            .collect(Collectors.joining(",", "[", "]"));
        var arguments = new ArrayList<Object>();
        arguments.add(vector);
        arguments.add(userId);
        arguments.add(queryEmbedding.provider());
        arguments.add(queryEmbedding.model());
        arguments.add(queryEmbedding.modelVersion());
        arguments.add(queryEmbedding.dimension());
        arguments.addAll(filters.documentIds());
        arguments.add(minimumRelevance);
        arguments.add(limit);
        return jdbc.query(sql, (result, row) -> new KnowledgeSearchCandidate(
            result.getObject("knowledge_chunk_id", UUID.class),
            result.getObject("knowledge_document_id", UUID.class),
            result.getString("title"), result.getString("source_object_id"),
            result.getString("provider_url"), result.getString("heading"),
            result.getString("object_reference"), result.getInt("token_estimate"),
            result.getDouble("relevance"), result.getTimestamp("source_updated_at").toInstant()
        ), arguments.toArray());
    }

    @Override
    public void record(KnowledgeRetrievalRecord record) {
        Timestamp completed = Timestamp.from(record.completedAt());
        jdbc.update("INSERT INTO retrieval_requests (retrieval_request_id,user_id,query_hash,context_purpose," +
                "source_types,document_filter_count,requested_limit,policy_version,request_status,requested_at," +
                "completed_at,duration_ms) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
            record.requestId(), record.userId(), record.queryHash(), record.contextPurpose(),
            String.join(",", record.filters().sourceTypes()), record.filters().documentIds().size(),
            record.requestedLimit(), record.policyVersion(), "COMPLETED", completed, completed, record.durationMs());
        jdbc.update("INSERT INTO retrieval_results (retrieval_result_id,retrieval_request_id,user_id,result_count," +
                "completed_at,expires_at) VALUES (?,?,?,?,?,?)",
            record.resultId(), record.requestId(), record.userId(), record.items().size(), completed,
            Timestamp.from(record.completedAt().plusSeconds(7 * 24 * 60 * 60L)));
        for (KnowledgeRetrievalRecord.Item item : record.items()) {
            jdbc.update("INSERT INTO retrieval_result_items (retrieval_result_item_id,retrieval_result_id,user_id," +
                    "result_position,knowledge_chunk_id,relevance) VALUES (?,?,?,?,?,?)",
                UUID.randomUUID(), record.resultId(), record.userId(), item.position(), item.chunkId(),
                item.relevance());
        }
    }
}
