package com.devpath.integration.adapter.out.notion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import com.devpath.integration.adapter.out.persistence.EncryptedNotionCredentialStore;
import com.devpath.integration.adapter.out.persistence.NotionPageMetadataStore;
import com.devpath.integration.adapter.out.persistence.StoredNotionCredential;
import com.devpath.integration.application.IntegrationAuditEvent;
import com.devpath.integration.application.IntegrationAuditPort;
import com.devpath.integration.config.NotionIntegrationProperties;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class NotionApiAdapterTest {
    @Test
    void exchangesOAuthCodeAndStoresBothTokensWithoutExposingThem() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var credentials = mock(EncryptedNotionCredentialStore.class);
        var metadata = mock(NotionPageMetadataStore.class);
        var audit = mock(IntegrationAuditPort.class);
        var adapter = new NotionApiAdapter(properties(), credentials, metadata, audit, builder);
        UUID userId = UUID.randomUUID(); Instant now = Instant.parse("2026-08-27T00:00:00Z");
        var stored = stored(userId, now);
        when(credentials.save(userId, "workspace-1", "bot-1", "DevPath", null,
            "access-secret", "refresh-secret", now)).thenReturn(stored);
        server.expect(once(), requestTo("https://api.notion.com/v1/oauth/token"))
            .andExpect(method(HttpMethod.POST)).andRespond(withSuccess("""
                {"access_token":"access-secret","refresh_token":"refresh-secret","bot_id":"bot-1",
                 "workspace_id":"workspace-1","workspace_name":"DevPath","workspace_icon":null}
                """, MediaType.APPLICATION_JSON));

        var result = adapter.complete(userId, "temporary-code", now);

        assertThat(result.provider()).isEqualTo("NOTION");
        assertThat(result.toString()).doesNotContain("access-secret", "refresh-secret");
        server.verify();
    }

    @Test
    void discoversEverySharedPageAndPersistsOnlyNormalizedMetadata() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var credentials = mock(EncryptedNotionCredentialStore.class);
        var metadata = mock(NotionPageMetadataStore.class);
        var audit = mock(IntegrationAuditPort.class);
        var adapter = new NotionApiAdapter(properties(), credentials, metadata, audit, builder);
        UUID userId = UUID.randomUUID(); Instant now = Instant.parse("2026-08-27T00:00:00Z");
        var stored = stored(userId, now.minusSeconds(60));
        when(credentials.findActive(userId)).thenReturn(Optional.of(stored));
        server.expect(once(), requestTo("https://api.notion.com/v1/search"))
            .andExpect(method(HttpMethod.POST)).andRespond(withSuccess("""
                {"results":[{"object":"page","id":"page-1","url":"https://notion.so/page-1",
                 "last_edited_time":"2026-08-26T12:00:00Z","in_trash":false,
                 "properties":{"Name":{"type":"title","title":[{"plain_text":"Roadmap"}]}}}],
                 "has_more":false,"next_cursor":null}
                """, MediaType.APPLICATION_JSON));

        var result = adapter.discover(userId, now);

        assertThat(result.workspaces()).singleElement().satisfies(workspace -> {
            assertThat(workspace.workspaceName()).isEqualTo("DevPath");
            assertThat(workspace.pages()).singleElement().satisfies(page -> {
                assertThat(page.title()).isEqualTo("Roadmap");
                assertThat(page.objectType()).isEqualTo("PAGE");
            });
        });
        verify(metadata).replace(eq(stored.connectionId()), eq(userId), eq(result.workspaces().getFirst().pages()), eq(now));
        verify(audit).record(IntegrationAuditEvent.NOTION_METADATA_DISCOVERED, userId, stored.connectionId().toString(), now);
        server.verify();
    }

    @Test
    void invalidRefreshExpiresTheConnectionAndDeletesDiscoveredMetadata() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var credentials = mock(EncryptedNotionCredentialStore.class);
        var metadata = mock(NotionPageMetadataStore.class);
        var audit = mock(IntegrationAuditPort.class);
        var adapter = new NotionApiAdapter(properties(), credentials, metadata, audit, builder);
        UUID userId = UUID.randomUUID(); Instant now = Instant.parse("2026-08-27T00:00:00Z");
        var stored = stored(userId, now.minusSeconds(60));
        when(credentials.findActive(userId)).thenReturn(Optional.of(stored));
        server.expect(once(), requestTo("https://api.notion.com/v1/search"))
            .andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        server.expect(once(), requestTo("https://api.notion.com/v1/oauth/token"))
            .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> adapter.discover(userId, now))
            .isInstanceOf(com.devpath.integration.application.NotionIntegrationUnavailableException.class);

        verify(credentials).expireActive(userId, now);
        verify(metadata).delete(stored.connectionId());
        verify(audit).record(IntegrationAuditEvent.NOTION_PERMISSION_CHANGED, userId,
            stored.connectionId().toString(), now);
        server.verify();
    }

    @Test
    void disconnectDeletesMetadataAndKeepsRemoteRevocationBestEffort() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var credentials = mock(EncryptedNotionCredentialStore.class);
        var metadata = mock(NotionPageMetadataStore.class);
        var adapter = new NotionApiAdapter(properties(), credentials, metadata,
            mock(IntegrationAuditPort.class), builder);
        UUID userId = UUID.randomUUID(); Instant now = Instant.parse("2026-08-27T00:00:00Z");
        var stored = stored(userId, now.minusSeconds(60));
        when(credentials.revokeActive(userId, now)).thenReturn(Optional.of(stored));
        server.expect(once(), requestTo("https://api.notion.com/v1/oauth/revoke"))
            .andExpect(method(HttpMethod.POST)).andRespond(withSuccess());

        var result = adapter.disconnect(userId, now);

        assertThat(result.status()).isEqualTo("REVOKED");
        verify(metadata).delete(stored.connectionId());
        server.verify();
    }

    private NotionIntegrationProperties properties() {
        return new NotionIntegrationProperties("client-id", "client-secret",
            "http://localhost:8080/api/v1/integrations/notion/callback",
            Base64.getEncoder().encodeToString(new byte[32]), "local-v1");
    }

    private StoredNotionCredential stored(UUID userId, Instant connectedAt) {
        return new StoredNotionCredential(UUID.randomUUID(), userId, "workspace-1", "bot-1", "DevPath", null,
            "access-secret", "refresh-secret", connectedAt);
    }
}
