package com.devpath.integration.adapter.out.github;

import com.devpath.integration.adapter.out.persistence.EncryptedProviderCredentialStore;
import com.devpath.integration.adapter.out.persistence.StoredProviderCredential;
import com.devpath.integration.application.ConnectedAccountView;
import com.devpath.integration.application.GitHubConnectionPort;
import com.devpath.integration.application.GitHubConnectionNotFoundException;
import com.devpath.integration.application.GitHubInstallationRequiredException;
import com.devpath.integration.application.GitHubIntegrationUnavailableException;
import com.devpath.integration.application.GitHubPermissionChangedException;
import com.devpath.integration.application.GitHubRepositoryListView;
import com.devpath.integration.application.GitHubBranchFact;
import com.devpath.integration.application.GitHubCommitFact;
import com.devpath.integration.application.GitHubDependencyFact;
import com.devpath.integration.application.GitHubFileFact;
import com.devpath.integration.application.GitHubLanguageFact;
import com.devpath.integration.application.GitHubRepositorySnapshot;
import com.devpath.integration.application.GitHubRepositoryView;
import com.devpath.integration.application.IntegrationAuditEvent;
import com.devpath.integration.application.IntegrationAuditPort;
import com.devpath.integration.config.GitHubIntegrationProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.time.OffsetDateTime;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class GitHubApiAdapter implements GitHubConnectionPort {
    private static final String API_VERSION = "2022-11-28";
    private static final int PAGE_SIZE = 100;
    private static final int MAX_PAGE_COUNT = 100;
    private static final int MAX_MANIFEST_COUNT = 50;
    private static final int MAX_FILE_COUNT = 20_000;
    private static final long MAX_MANIFEST_BYTES = 1_000_000;
    private static final Pattern GRADLE_DEPENDENCY = Pattern.compile(
        "(?m)^\\s*(implementation|api|runtimeOnly|testImplementation|testRuntimeOnly|classpath)\\s*(?:\\(|\\s)\\s*['\\\"]([^'\\\"]+)['\\\"]"
    );
    private static final Pattern SPRING_BOOT_PLUGIN = Pattern.compile("id\\s*['\\\"]org\\.springframework\\.boot['\\\"]");
    private static final ObjectMapper MANIFEST_MAPPER = new ObjectMapper();
    private final GitHubIntegrationProperties properties;
    private final EncryptedProviderCredentialStore credentials;
    private final IntegrationAuditPort audit;
    private final RestClient github;

    public GitHubApiAdapter(
        GitHubIntegrationProperties properties,
        EncryptedProviderCredentialStore credentials,
        IntegrationAuditPort audit,
        RestClient.Builder restClientBuilder
    ) {
        this.properties = properties;
        this.credentials = credentials;
        this.audit = audit;
        this.github = restClientBuilder
            .defaultHeader("Accept", "application/vnd.github+json")
            .defaultHeader("X-GitHub-Api-Version", API_VERSION)
            .build();
    }

    @Override
    public String authorizationUrl(String state) {
        requireConfigured();
        return UriComponentsBuilder.fromHttpUrl("https://github.com/login/oauth/authorize")
            .queryParam("client_id", properties.clientId())
            .queryParam("redirect_uri", properties.redirectUri())
            .queryParam("state", state)
            .build().encode().toUriString();
    }

    @Override
    public ConnectedAccountView complete(
        UUID userId,
        UUID externalIdentityId,
        String expectedProviderSubject,
        String code,
        Instant now
    ) {
        requireConfigured();
        GitHubTokenResponse token = exchangeCode(code);
        GitHubUser user = get("https://api.github.com/user", token.accessToken(), GitHubUser.class);
        if (!expectedProviderSubject.equals(Long.toString(user.id()))) {
            throw new GitHubIntegrationUnavailableException("GitHub integration account does not match the login identity");
        }
        GitHubInstallationList installations = get(
            "https://api.github.com/user/installations?per_page=1",
            token.accessToken(),
            GitHubInstallationList.class
        );
        if (installations.installations() == null || installations.installations().isEmpty()) {
            revokeAccessToken(token.accessToken());
            throw new GitHubInstallationRequiredException();
        }
        Instant expiresAt = addSeconds(now, token.expiresIn());
        Instant refreshExpiresAt = addSeconds(now, token.refreshTokenExpiresIn());
        StoredProviderCredential stored = credentials.save(
            userId, externalIdentityId, token.accessToken(), expiresAt,
            token.refreshToken(), refreshExpiresAt, token.scope(), now
        );
        return view(stored);
    }

    @Override
    public GitHubRepositoryListView listRepositories(UUID userId, Instant now) {
        StoredProviderCredential credential = credentials.findActive(userId)
            .orElseThrow(() -> new GitHubIntegrationUnavailableException("GitHub repository access is not connected"));
        credential = refreshIfRequired(credential, now);
        try {
            Map<Long, GitHubRepositoryView> repositories = new LinkedHashMap<>();
            for (GitHubInstallation installation : listInstallations(credential.accessToken())) {
                for (GitHubRepository repository : listRepositories(installation.id(), credential.accessToken())) {
                    repositories.putIfAbsent(repository.id(), new GitHubRepositoryView(
                        Long.toString(repository.id()), repository.name(), repository.fullName(),
                        repository.owner().login(), repository.privateRepository(), repository.archived(),
                        repository.defaultBranch(), repository.htmlUrl()
                    ));
                }
            }
            return new GitHubRepositoryListView(repositories.values().stream()
                .sorted(Comparator.comparing(GitHubRepositoryView::fullName, String.CASE_INSENSITIVE_ORDER))
                .toList());
        } catch (GitHubPermissionChangedException exception) {
            audit.record(
                IntegrationAuditEvent.GITHUB_PERMISSION_CHANGED,
                userId,
                credential.connectionId().toString(),
                now
            );
            throw exception;
        }
    }

    @Override
    public GitHubRepositorySnapshot collectRepository(
        UUID userId,
        String providerRepositoryId,
        String defaultBranch,
        Instant now
    ) {
        StoredProviderCredential credential = credentials.findActive(userId)
            .orElseThrow(() -> new GitHubIntegrationUnavailableException("GitHub repository access is not connected"));
        credential = refreshIfRequired(credential, now);
        try {
            List<GitHubBranchFact> branches = listBranches(
                providerRepositoryId, defaultBranch, credential.accessToken()
            );
            List<GitHubCommitFact> commits = listCommits(
                providerRepositoryId, defaultBranch, credential.accessToken()
            );
            List<GitHubLanguageFact> languages = listLanguages(providerRepositoryId, credential.accessToken());
            String sourceRevision = branches.stream()
                .filter(GitHubBranchFact::defaultBranch)
                .map(GitHubBranchFact::headCommitSha)
                .findFirst()
                .orElseGet(() -> commits.isEmpty() ? "0000000000000000000000000000000000000000" : commits.get(0).sha());
            List<GitHubFileFact> files = listFiles(providerRepositoryId, sourceRevision, credential.accessToken());
            List<GitHubDependencyFact> dependencies = listDependencies(providerRepositoryId, files, credential.accessToken());
            return new GitHubRepositorySnapshot(sourceRevision, branches, commits, languages, dependencies, files);
        } catch (GitHubPermissionChangedException exception) {
            audit.record(
                IntegrationAuditEvent.GITHUB_PERMISSION_CHANGED,
                userId,
                credential.connectionId().toString(),
                now
            );
            throw exception;
        }
    }

    @Override
    public ConnectedAccountView disconnect(UUID userId, Instant now) {
        requireConfigured();
        StoredProviderCredential credential = credentials.removeActive(userId)
            .orElseThrow(GitHubConnectionNotFoundException::new);
        revokeAccessToken(credential.accessToken());
        return new ConnectedAccountView(
            credential.connectionId(), "GITHUB", "REVOKED",
            List.of(), credential.connectedAt(), now
        );
    }

    private StoredProviderCredential refreshIfRequired(StoredProviderCredential credential, Instant now) {
        if (credential.expiresAt() == null || credential.expiresAt().isAfter(now.plus(1, ChronoUnit.MINUTES))) {
            return credential;
        }
        if (credential.refreshToken() == null
            || (credential.refreshTokenExpiresAt() != null && !credential.refreshTokenExpiresAt().isAfter(now))) {
            throw new GitHubIntegrationUnavailableException("GitHub repository access must be reconnected");
        }
        var form = new LinkedMultiValueMap<String, String>();
        form.add("client_id", properties.clientId());
        form.add("client_secret", properties.clientSecret());
        form.add("grant_type", "refresh_token");
        form.add("refresh_token", credential.refreshToken());
        GitHubTokenResponse refreshed;
        try {
            refreshed = postToken(form);
        } catch (GitHubIntegrationUnavailableException exception) {
            audit.record(
                IntegrationAuditEvent.GITHUB_TOKEN_REFRESH_FAILED,
                credential.userId(),
                credential.connectionId().toString(),
                now
            );
            throw exception;
        }
        String refreshToken = refreshed.refreshToken() == null
            ? credential.refreshToken()
            : refreshed.refreshToken();
        Instant refreshTokenExpiresAt = refreshed.refreshTokenExpiresIn() == null
            ? credential.refreshTokenExpiresAt()
            : addSeconds(now, refreshed.refreshTokenExpiresIn());
        return credentials.save(
            credential.userId(), credential.externalIdentityId(), refreshed.accessToken(),
            addSeconds(now, refreshed.expiresIn()), refreshToken,
            refreshTokenExpiresAt, refreshed.scope(), now
        );
    }

    private GitHubTokenResponse exchangeCode(String code) {
        var form = new LinkedMultiValueMap<String, String>();
        form.add("client_id", properties.clientId());
        form.add("client_secret", properties.clientSecret());
        form.add("code", code);
        form.add("redirect_uri", properties.redirectUri());
        return postToken(form);
    }

    private GitHubTokenResponse postToken(LinkedMultiValueMap<String, String> form) {
        try {
            GitHubTokenResponse response = github.post()
                .uri("https://github.com/login/oauth/access_token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .accept(MediaType.APPLICATION_JSON)
                .body(form)
                .retrieve()
                .body(GitHubTokenResponse.class);
            if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
                throw new GitHubIntegrationUnavailableException("GitHub did not issue an access token");
            }
            return response;
        } catch (RestClientException exception) {
            throw new GitHubIntegrationUnavailableException("GitHub authorization is unavailable", exception);
        }
    }

    private <T> T get(String url, String token, Class<T> type) {
        try {
            T response = github.get().uri(url)
                .headers(headers -> headers.setBearerAuth(token))
                .retrieve().body(type);
            if (response == null) {
                throw new GitHubIntegrationUnavailableException("GitHub returned an empty response");
            }
            return response;
        } catch (RestClientResponseException exception) {
            boolean rateLimited = exception.getStatusCode().value() == 403
                && "0".equals(exception.getResponseHeaders() == null
                    ? null
                    : exception.getResponseHeaders().getFirst("X-RateLimit-Remaining"));
            if (exception.getStatusCode().value() == 404
                || (exception.getStatusCode().value() == 403 && !rateLimited)) {
                throw new GitHubPermissionChangedException("GitHub permissions no longer allow this request", exception);
            }
            throw new GitHubIntegrationUnavailableException("GitHub API is unavailable", exception);
        } catch (RestClientException exception) {
            throw new GitHubIntegrationUnavailableException("GitHub API is unavailable", exception);
        }
    }

    private List<GitHubInstallation> listInstallations(String token) {
        var installations = new ArrayList<GitHubInstallation>();
        for (int page = 1; page <= MAX_PAGE_COUNT; page++) {
            GitHubInstallationList response = get(
                "https://api.github.com/user/installations?per_page=" + PAGE_SIZE + "&page=" + page,
                token,
                GitHubInstallationList.class
            );
            List<GitHubInstallation> values = safe(response.installations());
            installations.addAll(values);
            if (values.size() < PAGE_SIZE) {
                return installations;
            }
        }
        throw new GitHubIntegrationUnavailableException("GitHub installation result exceeds the safe pagination limit");
    }

    private List<GitHubRepository> listRepositories(long installationId, String token) {
        var repositories = new ArrayList<GitHubRepository>();
        for (int page = 1; page <= MAX_PAGE_COUNT; page++) {
            GitHubRepositoryList response = get(
                "https://api.github.com/user/installations/" + installationId
                    + "/repositories?per_page=" + PAGE_SIZE + "&page=" + page,
                token,
                GitHubRepositoryList.class
            );
            List<GitHubRepository> values = safe(response.repositories());
            repositories.addAll(values);
            if (values.size() < PAGE_SIZE) {
                return repositories;
            }
        }
        throw new GitHubIntegrationUnavailableException("GitHub repository result exceeds the safe pagination limit");
    }

    private List<GitHubBranchFact> listBranches(String repositoryId, String defaultBranch, String token) {
        var branches = new ArrayList<GitHubBranchFact>();
        for (int page = 1; page <= MAX_PAGE_COUNT; page++) {
            GitHubBranch[] response = get(
                "https://api.github.com/repositories/" + repositoryId
                    + "/branches?per_page=" + PAGE_SIZE + "&page=" + page,
                token,
                GitHubBranch[].class
            );
            List<GitHubBranch> values = Arrays.asList(response);
            values.stream().map(branch -> new GitHubBranchFact(
                branch.name(), branch.commit().sha(), branch.name().equals(defaultBranch)
            )).forEach(branches::add);
            if (values.size() < PAGE_SIZE) {
                return List.copyOf(branches);
            }
        }
        throw new GitHubIntegrationUnavailableException("GitHub branch result exceeds the safe pagination limit");
    }

    private List<GitHubCommitFact> listCommits(String repositoryId, String defaultBranch, String token) {
        var commits = new ArrayList<GitHubCommitFact>();
        for (int page = 1; page <= MAX_PAGE_COUNT; page++) {
            String url = UriComponentsBuilder
                .fromHttpUrl("https://api.github.com/repositories/" + repositoryId + "/commits")
                .queryParam("sha", defaultBranch)
                .queryParam("per_page", PAGE_SIZE)
                .queryParam("page", page)
                .build().encode().toUriString();
            GitHubCommit[] response = getCommits(url, token);
            List<GitHubCommit> values = Arrays.asList(response);
            values.stream().map(this::commitFact).forEach(commits::add);
            if (values.size() < PAGE_SIZE) {
                return List.copyOf(commits);
            }
        }
        throw new GitHubIntegrationUnavailableException("GitHub commit result exceeds the safe pagination limit");
    }

    @SuppressWarnings("unchecked")
    private List<GitHubLanguageFact> listLanguages(String repositoryId, String token) {
        Map<String, Object> response = get(
            "https://api.github.com/repositories/" + repositoryId + "/languages", token, Map.class
        );
        return response.entrySet().stream()
            .filter(entry -> entry.getValue() instanceof Number)
            .map(entry -> new GitHubLanguageFact(entry.getKey(), ((Number) entry.getValue()).longValue()))
            .sorted(Comparator.comparingLong(GitHubLanguageFact::byteCount).reversed()
                .thenComparing(GitHubLanguageFact::providerLabel))
            .toList();
    }

    private List<GitHubFileFact> listFiles(String repositoryId, String revision, String token) {
        if (revision.matches("0{40,64}")) return List.of();
        GitHubTreeResponse response = get(
            "https://api.github.com/repositories/" + repositoryId + "/git/trees/" + revision + "?recursive=1",
            token, GitHubTreeResponse.class
        );
        if (response.truncated()) {
            throw new GitHubIntegrationUnavailableException("GitHub repository tree exceeds the safe collection limit");
        }
        List<GitHubFileFact> files = safe(response.tree()).stream()
            .filter(entry -> "blob".equals(entry.type()) && isSafePath(entry.path()))
            .filter(entry -> entry.sha() != null && entry.sha().matches("[a-fA-F0-9]{40,64}"))
            .sorted(Comparator.comparing(GitHubTreeEntry::path))
            .limit(MAX_FILE_COUNT + 1L)
            .map(entry -> new GitHubFileFact(entry.path(), entry.sha(), entry.size() == null ? 0 : entry.size()))
            .toList();
        if (files.size() > MAX_FILE_COUNT) {
            throw new GitHubIntegrationUnavailableException("GitHub repository file count exceeds the safe collection limit");
        }
        return files;
    }

    private List<GitHubDependencyFact> listDependencies(
        String repositoryId, List<GitHubFileFact> files, String token
    ) {
        return files.stream()
            .filter(entry -> isSupportedManifest(entry.path()))
            .filter(entry -> entry.byteSize() <= MAX_MANIFEST_BYTES)
            .limit(MAX_MANIFEST_COUNT)
            .flatMap(entry -> parseManifest(repositoryId, entry, token).stream())
            .distinct()
            .toList();
    }

    private boolean isSafePath(String path) {
        return path != null && !path.isBlank() && path.length() <= 1000
            && !path.startsWith("/") && !path.contains("../");
    }

    private boolean isSupportedManifest(String path) {
        if (!isSafePath(path)) return false;
        return path.equals("package.json") || path.endsWith("/package.json")
            || path.equals("build.gradle") || path.endsWith("/build.gradle")
            || path.equals("build.gradle.kts") || path.endsWith("/build.gradle.kts");
    }

    private List<GitHubDependencyFact> parseManifest(String repositoryId, GitHubFileFact entry, String token) {
        GitHubBlob blob = get(
            "https://api.github.com/repositories/" + repositoryId + "/git/blobs/" + entry.blobSha(), token, GitHubBlob.class
        );
        if (!"base64".equalsIgnoreCase(blob.encoding()) || blob.content() == null) return List.of();
        byte[] decoded;
        try {
            decoded = Base64.getMimeDecoder().decode(blob.content());
        } catch (IllegalArgumentException exception) {
            return List.of();
        }
        if (decoded.length > MAX_MANIFEST_BYTES) return List.of();
        String content = new String(decoded, StandardCharsets.UTF_8);
        return entry.path().endsWith("package.json")
            ? parsePackageJson(entry.path(), content)
            : parseGradle(entry.path(), content);
    }

    private List<GitHubDependencyFact> parsePackageJson(String path, String content) {
        try {
            JsonNode root = MANIFEST_MAPPER.readTree(content);
            var values = new ArrayList<GitHubDependencyFact>();
            appendNpmDependencies(values, root.path("dependencies"), "RUNTIME", path);
            appendNpmDependencies(values, root.path("optionalDependencies"), "RUNTIME", path);
            appendNpmDependencies(values, root.path("peerDependencies"), "RUNTIME", path);
            appendNpmDependencies(values, root.path("devDependencies"), "DEVELOPMENT", path);
            Map<String, GitHubDependencyFact> unique = new LinkedHashMap<>();
            values.forEach(value -> unique.putIfAbsent(
                value.ecosystem() + ":" + value.packageName() + ":" + value.scope() + ":" + value.manifestPath(), value
            ));
            return List.copyOf(unique.values());
        } catch (RuntimeException | java.io.IOException exception) {
            return List.of();
        }
    }

    private void appendNpmDependencies(
        List<GitHubDependencyFact> target, JsonNode dependencies, String scope, String path
    ) {
        if (!dependencies.isObject()) return;
        dependencies.fields().forEachRemaining(entry -> target.add(new GitHubDependencyFact(
            "npm", entry.getKey(), entry.getValue().isTextual() ? entry.getValue().asText() : null, scope, path
        )));
    }

    private List<GitHubDependencyFact> parseGradle(String path, String content) {
        var values = new ArrayList<GitHubDependencyFact>();
        Matcher matcher = GRADLE_DEPENDENCY.matcher(content);
        while (matcher.find()) {
            String[] coordinate = matcher.group(2).split(":", 3);
            if (coordinate.length >= 2) {
                String scope = matcher.group(1).startsWith("test") ? "TEST"
                    : "classpath".equals(matcher.group(1)) ? "PLUGIN" : "RUNTIME";
                values.add(new GitHubDependencyFact("gradle", coordinate[0] + ":" + coordinate[1],
                    coordinate.length == 3 ? coordinate[2] : null, scope, path));
            }
        }
        if (SPRING_BOOT_PLUGIN.matcher(content).find()) {
            values.add(new GitHubDependencyFact(
                "gradle", "org.springframework.boot:spring-boot", null, "PLUGIN", path
            ));
        }
        return List.copyOf(values);
    }

    private GitHubCommit[] getCommits(String url, String token) {
        try {
            GitHubCommit[] response = github.get().uri(url)
                .headers(headers -> headers.setBearerAuth(token))
                .retrieve().body(GitHubCommit[].class);
            return response == null ? new GitHubCommit[0] : response;
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 409) {
                return new GitHubCommit[0];
            }
            boolean rateLimited = exception.getStatusCode().value() == 403
                && "0".equals(exception.getResponseHeaders() == null
                    ? null
                    : exception.getResponseHeaders().getFirst("X-RateLimit-Remaining"));
            if (exception.getStatusCode().value() == 404
                || (exception.getStatusCode().value() == 403 && !rateLimited)) {
                throw new GitHubPermissionChangedException("GitHub permissions no longer allow this request", exception);
            }
            throw new GitHubIntegrationUnavailableException("GitHub API is unavailable", exception);
        } catch (RestClientException exception) {
            throw new GitHubIntegrationUnavailableException("GitHub API is unavailable", exception);
        }
    }

    private GitHubCommitFact commitFact(GitHubCommit value) {
        String message = value.commit().message() == null ? "" : value.commit().message().lines().findFirst().orElse("");
        if (message.length() > 500) {
            message = message.substring(0, 500);
        }
        return new GitHubCommitFact(
            value.sha(), value.author() == null ? null : value.author().login(),
            value.commit().author().date().toInstant(), message
        );
    }

    private void revokeAccessToken(String accessToken) {
        try {
            github.method(HttpMethod.DELETE)
                .uri("https://api.github.com/applications/" + properties.clientId() + "/token")
                .headers(headers -> headers.setBasicAuth(properties.clientId(), properties.clientSecret()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("access_token", accessToken))
                .retrieve()
                .toBodilessEntity();
        } catch (RestClientException ignored) {
            // The local credential has already been discarded; remote revocation is best effort.
        }
    }

    private ConnectedAccountView view(StoredProviderCredential credential) {
        return new ConnectedAccountView(
            credential.connectionId(), "GITHUB", "ACTIVE",
            Arrays.stream(credential.scopeSummary().split(" ")).filter(value -> !value.isBlank()).toList(),
            credential.connectedAt(), credential.refreshTokenExpiresAt() == null ? credential.expiresAt() : credential.refreshTokenExpiresAt()
        );
    }

    private Instant addSeconds(Instant now, Long seconds) {
        return seconds == null ? null : now.plusSeconds(seconds);
    }

    private <T> List<T> safe(List<T> values) {
        return values == null ? List.of() : values;
    }

    private void requireConfigured() {
        if (!properties.configured()) {
            throw new GitHubIntegrationUnavailableException("GitHub integration configuration is incomplete");
        }
    }

    private record GitHubTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("expires_in") Long expiresIn,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("refresh_token_expires_in") Long refreshTokenExpiresIn,
        String scope
    ) {}

    private record GitHubUser(long id) {}
    private record GitHubInstallationList(List<GitHubInstallation> installations) {}
    private record GitHubInstallation(long id) {}
    private record GitHubRepositoryList(List<GitHubRepository> repositories) {}
    private record GitHubOwner(String login) {}
    private record GitHubRepository(
        long id,
        String name,
        @JsonProperty("full_name") String fullName,
        GitHubOwner owner,
        @JsonProperty("private") boolean privateRepository,
        boolean archived,
        @JsonProperty("default_branch") String defaultBranch,
        @JsonProperty("html_url") String htmlUrl
    ) {}
    private record GitHubBranch(String name, GitHubBranchCommit commit) {}
    private record GitHubBranchCommit(String sha) {}
    private record GitHubCommit(String sha, GitHubCommitAuthor author, GitHubCommitDetail commit) {}
    private record GitHubCommitAuthor(String login) {}
    private record GitHubCommitDetail(String message, GitHubCommitSignature author) {}
    private record GitHubCommitSignature(OffsetDateTime date) {}
    private record GitHubTreeResponse(boolean truncated, List<GitHubTreeEntry> tree) {}
    private record GitHubTreeEntry(String path, String type, String sha, Long size) {}
    private record GitHubBlob(String encoding, String content) {}
}
