package com.devpath.integration.adapter.out.github;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;

import com.devpath.integration.adapter.out.persistence.EncryptedProviderCredentialStore;
import com.devpath.integration.adapter.out.persistence.StoredProviderCredential;
import com.devpath.integration.config.GitHubIntegrationProperties;
import com.devpath.integration.application.IntegrationAuditPort;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GitHubApiAdapterTest {
    @Test
    void exchangesTheCodeVerifiesTheLoginIdentityAndStoresTheTokenServerSide() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var credentials = mock(EncryptedProviderCredentialStore.class);
        var audit = mock(IntegrationAuditPort.class);
        var properties = new GitHubIntegrationProperties(
            "client-id", "client-secret", "http://localhost:8080/api/v1/integrations/github/callback",
            Base64.getEncoder().encodeToString(new byte[32]), "local-v1"
        );
        var adapter = new GitHubApiAdapter(properties, credentials, audit, builder);
        UUID userId = UUID.randomUUID();
        UUID identityId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-11T00:00:00Z");
        var stored = new StoredProviderCredential(
            UUID.randomUUID(), userId, identityId, "secret-access-token", now.plusSeconds(28_800),
            "secret-refresh-token", now.plusSeconds(15_552_000), "", now
        );
        when(credentials.save(
            eq(userId), eq(identityId), eq("secret-access-token"), eq(now.plusSeconds(28_800)),
            eq("secret-refresh-token"), eq(now.plusSeconds(15_552_000)), eq(""), eq(now)
        )).thenReturn(stored);

        server.expect(once(), requestTo("https://github.com/login/oauth/access_token"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess("""
                {"access_token":"secret-access-token","expires_in":28800,
                 "refresh_token":"secret-refresh-token","refresh_token_expires_in":15552000,"scope":""}
                """, MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo("https://api.github.com/user"))
            .andRespond(withSuccess("{\"id\":1849102}", MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo("https://api.github.com/user/installations?per_page=1"))
            .andRespond(withSuccess("{\"installations\":[{\"id\":42}]}", MediaType.APPLICATION_JSON));

        var result = adapter.complete(userId, identityId, "1849102", "temporary-code", now);

        assertThat(result.provider()).isEqualTo("GITHUB");
        verify(credentials).save(
            userId, identityId, "secret-access-token", now.plusSeconds(28_800),
            "secret-refresh-token", now.plusSeconds(15_552_000), "", now
        );
        server.verify();
    }

    @Test
    void followsEveryRepositoryPageAndSortsTheNormalizedResult() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var credentials = mock(EncryptedProviderCredentialStore.class);
        var audit = mock(IntegrationAuditPort.class);
        var properties = properties();
        var adapter = new GitHubApiAdapter(properties, credentials, audit, builder);
        UUID userId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-11T00:00:00Z");
        when(credentials.findActive(userId)).thenReturn(Optional.of(stored(userId, now)));

        server.expect(once(), requestTo("https://api.github.com/user/installations?per_page=100&page=1"))
            .andRespond(withSuccess("{\"installations\":[{\"id\":42}]}", MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo("https://api.github.com/user/installations/42/repositories?per_page=100&page=1"))
            .andRespond(withSuccess(repositoryPage(1, 100), MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo("https://api.github.com/user/installations/42/repositories?per_page=100&page=2"))
            .andRespond(withSuccess(repositoryPage(101, 1), MediaType.APPLICATION_JSON));

        var result = adapter.listRepositories(userId, now);

        assertThat(result.repositories()).hasSize(101);
        assertThat(result.repositories()).isSortedAccordingTo(
            java.util.Comparator.comparing(
                com.devpath.integration.application.GitHubRepositoryView::fullName,
                String.CASE_INSENSITIVE_ORDER
            )
        );
        verifyNoInteractions(audit);
        server.verify();
    }

    @Test
    void removesTheLocalCredentialAndAttemptsRemoteRevocationOnDisconnect() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var credentials = mock(EncryptedProviderCredentialStore.class);
        var audit = mock(IntegrationAuditPort.class);
        var properties = properties();
        var adapter = new GitHubApiAdapter(properties, credentials, audit, builder);
        UUID userId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-11T00:00:00Z");
        StoredProviderCredential stored = stored(userId, now);
        when(credentials.removeActive(userId)).thenReturn(Optional.of(stored));
        server.expect(once(), requestTo("https://api.github.com/applications/client-id/token"))
            .andExpect(method(HttpMethod.DELETE))
            .andRespond(withNoContent());

        var result = adapter.disconnect(userId, now);

        assertThat(result.status()).isEqualTo("REVOKED");
        verify(credentials).removeActive(userId);
        server.verify();
    }

    @Test
    void collectsBranchesCommitsLanguagesAndManifestDependenciesIntoProviderIndependentSnapshotFacts() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var credentials = mock(EncryptedProviderCredentialStore.class);
        var audit = mock(IntegrationAuditPort.class);
        var adapter = new GitHubApiAdapter(properties(), credentials, audit, builder);
        UUID userId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-11T00:00:00Z");
        when(credentials.findActive(userId)).thenReturn(Optional.of(stored(userId, now)));
        server.expect(once(), requestTo("https://api.github.com/repositories/42/branches?per_page=100&page=1"))
            .andRespond(withSuccess("[{\"name\":\"main\",\"commit\":{\"sha\":\"abc123\"}}]", MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo("https://api.github.com/repositories/42/commits?sha=main&per_page=100&page=1"))
            .andRespond(withSuccess("""
                [{"sha":"abc123","author":{"login":"owner"},
                  "commit":{"message":"first commit\\nbody","author":{"date":"2026-08-10T00:00:00Z"}}}]
                """, MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo("https://api.github.com/repositories/42/languages"))
            .andRespond(withSuccess("{\"Java\":7500,\"TypeScript\":2500}", MediaType.APPLICATION_JSON));
        String blobSha = "cccccccccccccccccccccccccccccccccccccccc";
        server.expect(once(), requestTo("https://api.github.com/repositories/42/git/trees/abc123?recursive=1"))
            .andRespond(withSuccess("{\"truncated\":false,\"tree\":[{\"path\":\"frontend/package.json\",\"type\":\"blob\",\"sha\":\""
                + blobSha + "\",\"size\":100}]}", MediaType.APPLICATION_JSON));
        String manifest = "{\"dependencies\":{\"react\":\"18.3.1\",\"pg\":\"8.0.0\"}}";
        server.expect(once(), requestTo("https://api.github.com/repositories/42/git/blobs/" + blobSha))
            .andRespond(withSuccess("{\"encoding\":\"base64\",\"content\":\""
                + Base64.getEncoder().encodeToString(manifest.getBytes(java.nio.charset.StandardCharsets.UTF_8))
                + "\"}", MediaType.APPLICATION_JSON));

        var result = adapter.collectRepository(userId, "42", "main", now);

        assertThat(result.sourceRevision()).isEqualTo("abc123");
        assertThat(result.branches()).singleElement().satisfies(branch -> assertThat(branch.defaultBranch()).isTrue());
        assertThat(result.commits()).singleElement().satisfies(commit -> {
            assertThat(commit.authorLogin()).isEqualTo("owner");
            assertThat(commit.messageSummary()).isEqualTo("first commit");
        });
        assertThat(result.languages()).extracting(value -> value.providerLabel())
            .containsExactly("Java", "TypeScript");
        assertThat(result.dependencies()).extracting(value -> value.packageName())
            .containsExactly("react", "pg");
        assertThat(result.files()).singleElement().satisfies(file ->
            assertThat(file.path()).isEqualTo("frontend/package.json")
        );
        server.verify();
    }

    private GitHubIntegrationProperties properties() {
        return new GitHubIntegrationProperties(
            "client-id", "client-secret", "http://localhost:8080/api/v1/integrations/github/callback",
            Base64.getEncoder().encodeToString(new byte[32]), "local-v1"
        );
    }

    private StoredProviderCredential stored(UUID userId, Instant now) {
        return new StoredProviderCredential(
            UUID.randomUUID(), userId, UUID.randomUUID(), "secret-access-token", now.plusSeconds(28_800),
            "secret-refresh-token", now.plusSeconds(15_552_000), "", now
        );
    }

    private String repositoryPage(int firstId, int count) {
        String repositories = IntStream.range(firstId, firstId + count)
            .mapToObj(id -> """
                {"id":%d,"name":"repo-%d","full_name":"owner/repo-%d","owner":{"login":"owner"},
                 "private":false,"archived":false,"default_branch":"main","html_url":"https://github.com/owner/repo-%d"}
                """.formatted(id, id, id, id))
            .collect(java.util.stream.Collectors.joining(","));
        return "{\"repositories\":[" + repositories + "]}";
    }
}
