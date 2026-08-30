package com.devpath.knowledge.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.devpath.knowledge.application.EmbeddingVector;
import com.devpath.knowledge.application.PreparedKnowledgeChunk;
import com.devpath.knowledge.application.PreparedKnowledgeDocument;
import com.devpath.knowledge.application.KnowledgeRetrievalRecord;
import com.devpath.knowledge.application.KnowledgeSearchFilters;
import com.devpath.knowledge.domain.KnowledgeIngestionJob;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest(properties={"spring.jpa.hibernate.ddl-auto=validate","spring.flyway.enabled=true"})
@AutoConfigureTestDatabase(replace=AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaKnowledgePersistenceAdapter.class,JdbcKnowledgeSearchPersistenceAdapter.class})
@Testcontainers(disabledWithoutDocker=true)
class KnowledgePersistenceIntegrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new com.devpath.test.PgVectorPostgreSQLContainer();
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username",POSTGRES::getUsername);
        registry.add("spring.datasource.password",POSTGRES::getPassword);
    }
    @Autowired JpaKnowledgePersistenceAdapter persistence;
    @Autowired JdbcKnowledgeSearchPersistenceAdapter search;
    @Autowired JdbcTemplate jdbc;

    @Test
    void versionsVectorsAndArchiveRemainOwnerScopedAndRebuildable() {
        Instant now=Instant.parse("2026-08-30T00:00:00Z");
        UUID owner=UUID.randomUUID(), other=UUID.randomUUID(), connection=UUID.randomUUID(), document=UUID.randomUUID();
        insertUser(owner,now); insertUser(other,now); insertConnection(connection,owner,now);
        KnowledgeIngestionJob job=KnowledgeIngestionJob.queue(owner,connection,"page-1",document,"key-1",now).claim(now);
        persistence.saveJob(job);
        UUID version1=UUID.randomUUID(), chunk1=UUID.randomUUID();
        persistence.complete(job,prepared(document,version1,chunk1,"hash-1","chunk-hash-1",now),now);
        insertPage(connection,owner,"page-1",now);

        assertThat(persistence.findDocumentByIdAndOwner(document,owner)).isPresent();
        assertThat(persistence.findDocumentByIdAndOwner(document,other)).isEmpty();
        assertThat(persistence.findCurrentChunks(document,owner)).singleElement().satisfies(chunk -> {
            assertThat(chunk.id()).isEqualTo(chunk1); assertThat(chunk.status()).isEqualTo("INDEXED");
        });
        assertThat(jdbc.queryForObject("select count(*) from embedding_records where user_id=? and embedding_status='ACTIVE'",Integer.class,owner)).isEqualTo(1);
        var queryEmbedding=new EmbeddingVector("OLLAMA","nomic-embed-text","nomic-v1",768,Collections.nCopies(768,0.01d));
        var filters=new KnowledgeSearchFilters(List.of("NOTION"),List.of());
        assertThat(search.search(owner,queryEmbedding,filters,5,0.25)).singleElement().satisfies(result -> {
            assertThat(result.documentId()).isEqualTo(document);
            assertThat(result.chunkId()).isEqualTo(chunk1);
            assertThat(result.sourceUrl()).isEqualTo("https://www.notion.so/page-1");
            assertThat(result.relevance()).isEqualTo(1d);
        });
        assertThat(search.search(other,queryEmbedding,filters,5,0.25)).isEmpty();
        assertThat(search.findAuthorizedCurrentChunkIds(other,List.of(chunk1))).isEmpty();
        assertThat(search.findAuthorizedCurrentChunkIds(owner,List.of(chunk1))).containsExactly(chunk1);
        assertThat(search.search(owner,queryEmbedding,new KnowledgeSearchFilters(List.of("NOTION"),List.of(UUID.randomUUID())),5,0.25)).isEmpty();

        jdbc.update("update notion_page_metadata set last_edited_at=? where notion_connection_id=? and provider_page_id=?",
            java.sql.Timestamp.from(now.plusSeconds(1)),connection,"page-1");
        assertThat(search.search(owner,queryEmbedding,filters,5,0.25)).isEmpty();
        assertThat(search.findAuthorizedCurrentChunkIds(owner,List.of(chunk1))).isEmpty();
        jdbc.update("update notion_page_metadata set last_edited_at=? where notion_connection_id=? and provider_page_id=?",
            java.sql.Timestamp.from(now),connection,"page-1");

        UUID version2=UUID.randomUUID(), chunk2=UUID.randomUUID();
        persistence.complete(job,prepared(document,version2,chunk2,"hash-2","chunk-hash-2",now.plusSeconds(60)),now.plusSeconds(60));
        assertThat(persistence.findCurrentChunks(document,owner)).extracting(chunk -> chunk.id()).containsExactly(chunk2);
        assertThat(jdbc.queryForObject("select count(*) from embedding_records where user_id=? and embedding_status='STALE'",Integer.class,owner)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from embedding_records where user_id=? and embedding_status='ACTIVE'",Integer.class,owner)).isEqualTo(1);

        UUID version3=UUID.randomUUID(), chunk3=UUID.randomUUID();
        persistence.complete(job,prepared(document,version3,chunk3,"hash-1","chunk-hash-3",now.plusSeconds(90)),now.plusSeconds(90));
        assertThat(persistence.findCurrentChunks(document,owner)).extracting(chunk -> chunk.id()).containsExactly(chunk3);
        assertThat(jdbc.queryForObject("select count(*) from knowledge_document_versions where knowledge_document_id=?",Integer.class,document)).isEqualTo(3);
        assertThat(jdbc.queryForObject("select count(*) from embedding_records where user_id=? and embedding_status='ACTIVE'",Integer.class,owner)).isEqualTo(1);

        jdbc.update("update notion_workspace_connections set connection_status='REVOKED' where notion_connection_id=?",connection);
        assertThat(search.search(owner,queryEmbedding,filters,5,0.25)).isEmpty();
        assertThat(search.findAuthorizedCurrentChunkIds(owner,List.of(chunk3))).isEmpty();
        jdbc.update("update notion_workspace_connections set connection_status='ACTIVE' where notion_connection_id=?",connection);

        UUID requestId=UUID.randomUUID(),resultId=UUID.randomUUID();
        search.record(new KnowledgeRetrievalRecord(requestId,resultId,owner,"a".repeat(64),"USER_SEARCH",filters,
            5,"knowledge-semantic-v1",12,now.plusSeconds(100),List.of(
                new KnowledgeRetrievalRecord.Item(0,chunk3,1d))));
        assertThat(jdbc.queryForObject("select query_hash from retrieval_requests where retrieval_request_id=?",String.class,requestId))
            .isEqualTo("a".repeat(64));
        assertThat(jdbc.queryForObject("select count(*) from retrieval_result_items where retrieval_result_id=? and user_id=?",Integer.class,resultId,owner)).isEqualTo(1);

        var archived=persistence.archive(persistence.findDocumentByIdAndOwner(document,owner).orElseThrow(),now.plusSeconds(120));
        assertThat(archived.status()).isEqualTo("ARCHIVED");
        assertThat(jdbc.queryForObject("select count(*) from embedding_records where user_id=? and embedding_status='ACTIVE'",Integer.class,owner)).isZero();
        assertThat(search.search(owner,queryEmbedding,filters,5,0.25)).isEmpty();
    }

    private PreparedKnowledgeDocument prepared(UUID document,UUID version,UUID chunk,String contentHash,String chunkHash,Instant now) {
        var embedding=new EmbeddingVector("OLLAMA","nomic-embed-text","nomic-v1",768,Collections.nCopies(768,0.01d));
        return new PreparedKnowledgeDocument(document,version,"Page title","page-1",now,contentHash,
            "object://source",List.of(new PreparedKnowledgeChunk(chunk,0,"Heading","object://chunk",chunkHash,10,embedding)));
    }
    private void insertUser(UUID id,Instant now) { var timestamp=java.sql.Timestamp.from(now); jdbc.update("insert into users(user_id,account_status,display_name,created_at,updated_at,version) values (?,?,?,?,?,0)",id,"ACTIVE","User",timestamp,timestamp); }
    private void insertConnection(UUID id,UUID user,Instant now) { var timestamp=java.sql.Timestamp.from(now); jdbc.update("insert into notion_workspace_connections(notion_connection_id,user_id,provider_workspace_id,provider_bot_id,workspace_name,encrypted_access_token,access_token_iv,encrypted_refresh_token,refresh_token_iv,key_version,connection_status,connected_at,updated_at,version) values (?,?,?,?,?,?,?,?,?,?,?,?,?,0)",id,user,"workspace","bot","Workspace",new byte[]{1},new byte[12],new byte[]{1},new byte[12],"test","ACTIVE",timestamp,timestamp); }
    private void insertPage(UUID connection,UUID user,String pageId,Instant now) { var timestamp=java.sql.Timestamp.from(now); jdbc.update("insert into notion_page_metadata(notion_page_metadata_id,notion_connection_id,user_id,provider_page_id,object_type,title,provider_url,last_edited_at,in_trash,discovered_at) values (?,?,?,?,?,?,?,?,?,?)",UUID.randomUUID(),connection,user,pageId,"PAGE","Page title","https://www.notion.so/"+pageId,timestamp,false,timestamp); }
}
