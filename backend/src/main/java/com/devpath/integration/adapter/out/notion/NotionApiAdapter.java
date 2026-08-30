package com.devpath.integration.adapter.out.notion;

import com.devpath.integration.adapter.out.persistence.EncryptedNotionCredentialStore;
import com.devpath.integration.adapter.out.persistence.NotionPageMetadataStore;
import com.devpath.integration.adapter.out.persistence.StoredNotionCredential;
import com.devpath.integration.application.ConnectedAccountView;
import com.devpath.integration.application.IntegrationAuditEvent;
import com.devpath.integration.application.IntegrationAuditPort;
import com.devpath.integration.application.NotionConnectionNotFoundException;
import com.devpath.integration.application.NotionConnectionPort;
import com.devpath.integration.application.NotionIntegrationUnavailableException;
import com.devpath.integration.application.NotionRateLimitExceededException;
import com.devpath.integration.application.NotionWorkspaceListView;
import com.devpath.integration.application.NotionPageContentView;
import com.devpath.integration.application.NotionWorkspacePageView;
import com.devpath.integration.application.NotionWorkspaceView;
import com.devpath.integration.config.NotionIntegrationProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class NotionApiAdapter implements NotionConnectionPort {
    private static final String API_VERSION = "2026-03-11";
    private static final int PAGE_SIZE = 100;
    private static final int MAX_PAGE_COUNT = 10;
    private final NotionIntegrationProperties properties;
    private final EncryptedNotionCredentialStore credentials;
    private final NotionPageMetadataStore metadata;
    private final IntegrationAuditPort audit;
    private final RestClient notion;

    public NotionApiAdapter(NotionIntegrationProperties properties, EncryptedNotionCredentialStore credentials,
        NotionPageMetadataStore metadata, IntegrationAuditPort audit,
        @Qualifier("notionRestClientBuilder") RestClient.Builder builder) {
        this.properties = properties; this.credentials = credentials; this.metadata = metadata; this.audit = audit;
        this.notion = builder.defaultHeader("Notion-Version", API_VERSION).defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE).build();
    }

    @Override
    public String authorizationUrl(String state) {
        requireConfigured();
        return UriComponentsBuilder.fromHttpUrl("https://api.notion.com/v1/oauth/authorize")
            .queryParam("client_id", properties.clientId()).queryParam("redirect_uri", properties.redirectUri())
            .queryParam("response_type", "code").queryParam("owner", "user").queryParam("state", state)
            .build().encode().toUriString();
    }

    @Override
    public ConnectedAccountView complete(UUID userId, String code, Instant now) {
        requireConfigured();
        try {
            TokenResponse token = token(Map.of("grant_type", "authorization_code", "code", code,
                "redirect_uri", properties.redirectUri()));
            requireTokenResponse(token);
            StoredNotionCredential stored = credentials.save(userId, token.workspaceId(), token.botId(), token.workspaceName(),
                token.workspaceIcon(), token.accessToken(), token.refreshToken(), now);
            return view(stored, "ACTIVE", null);
        } catch (RestClientException exception) {
            throw unavailable(exception);
        }
    }

    @Override
    public NotionWorkspaceListView discover(UUID userId, Instant now) {
        StoredNotionCredential credential = credentials.findActive(userId).orElseThrow(NotionConnectionNotFoundException::new);
        try {
            List<NotionWorkspacePageView> pages = searchAll(credential.accessToken());
            metadata.replace(credential.connectionId(), userId, pages, now);
            audit.record(IntegrationAuditEvent.NOTION_METADATA_DISCOVERED, userId, credential.connectionId().toString(), now);
            return workspaceList(credential, pages, now);
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 429) {
                audit.record(IntegrationAuditEvent.NOTION_RATE_LIMITED, userId, credential.connectionId().toString(), now);
                throw new NotionRateLimitExceededException(retryAfter(exception));
            }
            if (exception.getStatusCode().value() == 401) {
                return retryAfterRefresh(credential, now);
            }
            if (exception.getStatusCode().value() == 403) {
                expire(credential, now);
            }
            throw unavailable(exception);
        } catch (RestClientException exception) {
            throw unavailable(exception);
        }
    }

    @Override
    public NotionPageContentView collectPage(UUID userId, String providerPageId, Instant now) {
        if (providerPageId == null || providerPageId.isBlank() || providerPageId.length() > 255) {
            throw new IllegalArgumentException("Notion page identifier is invalid");
        }
        StoredNotionCredential credential = credentials.findActive(userId)
            .orElseThrow(NotionConnectionNotFoundException::new);
        var owned = metadata.findOwned(userId, providerPageId)
            .filter(page -> page.connectionId().equals(credential.connectionId()))
            .orElseThrow(NotionConnectionNotFoundException::new);
        try {
            String content = collectBlocks(credential.accessToken(), providerPageId, 0, new int[]{0}).trim();
            if (content.isBlank()) {
                throw new NotionIntegrationUnavailableException("Notion page has no importable text content");
            }
            return new NotionPageContentView(credential.connectionId(), providerPageId,
                owned.page().title(), owned.page().lastEditedAt(), content);
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 429) {
                throw new NotionRateLimitExceededException(retryAfter(exception));
            }
            if (exception.getStatusCode().value() == 401 || exception.getStatusCode().value() == 403) {
                expire(credential, now);
                throw new NotionConnectionNotFoundException();
            }
            throw unavailable(exception);
        } catch (RestClientException exception) {
            throw unavailable(exception);
        }
    }

    @Override
    public void verifyPageAccess(UUID userId, UUID connectionId, String providerPageId) {
        if (connectionId == null || providerPageId == null || providerPageId.isBlank() || providerPageId.length() > 255) {
            throw new IllegalArgumentException("Notion page selection is invalid");
        }
        StoredNotionCredential credential = credentials.findActive(userId)
            .filter(item -> item.connectionId().equals(connectionId))
            .orElseThrow(NotionConnectionNotFoundException::new);
        metadata.findOwned(userId, providerPageId)
            .filter(page -> page.connectionId().equals(credential.connectionId()))
            .orElseThrow(NotionConnectionNotFoundException::new);
    }

    private String collectBlocks(String accessToken, String blockId, int depth, int[] total) {
        if (depth > 8) throw new NotionIntegrationUnavailableException("Notion page nesting exceeds the safe limit");
        StringBuilder content = new StringBuilder();
        String cursor = null;
        for (int pageNumber = 0; pageNumber < MAX_PAGE_COUNT; pageNumber++) {
            var uri = UriComponentsBuilder.fromHttpUrl("https://api.notion.com/v1/blocks/" + blockId + "/children")
                .queryParam("page_size", PAGE_SIZE);
            if (cursor != null) uri.queryParam("start_cursor", cursor);
            BlockChildrenResponse response = notion.get().uri(uri.build().encode().toUriString())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken).retrieve().body(BlockChildrenResponse.class);
            if (response == null || response.results() == null) {
                throw new NotionIntegrationUnavailableException("Notion returned an invalid block response");
            }
            for (JsonNode block : response.results()) {
                if (++total[0] > 1000) throw new NotionIntegrationUnavailableException("Notion page exceeds the safe block limit");
                String text = blockText(block);
                if (!text.isBlank()) content.append(text).append('\n');
                if (block.path("has_children").asBoolean(false)) {
                    content.append(collectBlocks(accessToken, requiredText(block, "id"), depth + 1, total));
                }
            }
            if (!response.hasMore()) return content.toString();
            if (response.nextCursor() == null || response.nextCursor().isBlank()) {
                throw new NotionIntegrationUnavailableException("Notion block pagination cursor is unavailable");
            }
            cursor = response.nextCursor();
        }
        throw new NotionIntegrationUnavailableException("Notion block pagination exceeds the safe limit");
    }

    private String blockText(JsonNode block) {
        String type = block.path("type").asText();
        JsonNode value = block.path(type);
        String text = richText(value.path("rich_text"));
        if (text.isBlank() && ("child_page".equals(type) || "child_database".equals(type))) {
            text = value.path("title").asText("").trim();
        }
        return switch (type) {
            case "heading_1" -> text.isBlank() ? "" : "# " + text;
            case "heading_2" -> text.isBlank() ? "" : "## " + text;
            case "heading_3" -> text.isBlank() ? "" : "### " + text;
            case "bulleted_list_item" -> text.isBlank() ? "" : "- " + text;
            case "numbered_list_item" -> text.isBlank() ? "" : "1. " + text;
            case "to_do" -> text.isBlank() ? "" : "- [" + (value.path("checked").asBoolean() ? "x" : " ") + "] " + text;
            case "code" -> text.isBlank() ? "" : "```\n" + text + "\n```";
            default -> text;
        };
    }

    private NotionWorkspaceListView retryAfterRefresh(StoredNotionCredential credential, Instant now) {
        try {
            TokenResponse refreshed = token(Map.of("grant_type", "refresh_token", "refresh_token", credential.refreshToken()));
            requireTokenResponse(refreshed);
            StoredNotionCredential rotated = credentials.save(credential.userId(), credential.workspaceId(), credential.botId(),
                credential.workspaceName(), credential.workspaceIconUrl(), refreshed.accessToken(), refreshed.refreshToken(), now);
            List<NotionWorkspacePageView> pages = searchAll(rotated.accessToken());
            metadata.replace(rotated.connectionId(), rotated.userId(), pages, now);
            audit.record(IntegrationAuditEvent.NOTION_METADATA_DISCOVERED, rotated.userId(), rotated.connectionId().toString(), now);
            return workspaceList(rotated, pages, now);
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 429) {
                audit.record(IntegrationAuditEvent.NOTION_RATE_LIMITED, credential.userId(), credential.connectionId().toString(), now);
                throw new NotionRateLimitExceededException(retryAfter(exception));
            }
            if (exception.getStatusCode().value() == 400 || exception.getStatusCode().value() == 401
                || exception.getStatusCode().value() == 403) {
                expire(credential, now);
            }
            throw unavailable(exception);
        }
    }

    @Override
    public ConnectedAccountView disconnect(UUID userId, Instant now) {
        requireConfigured();
        StoredNotionCredential credential = credentials.revokeActive(userId, now).orElseThrow(NotionConnectionNotFoundException::new);
        metadata.delete(credential.connectionId());
        try {
            notion.post().uri("https://api.notion.com/v1/oauth/revoke")
                .headers(headers -> headers.setBasicAuth(properties.clientId(), properties.clientSecret()))
                .contentType(MediaType.APPLICATION_JSON).body(Map.of("token", credential.accessToken())).retrieve().toBodilessEntity();
        } catch (RestClientException ignored) {
            // Local revocation is authoritative; remote revocation is best-effort and never restores the secret.
        }
        return view(credential, "REVOKED", now);
    }

    private TokenResponse token(Map<String, String> body) {
        return notion.post().uri("https://api.notion.com/v1/oauth/token")
            .headers(headers -> headers.setBasicAuth(properties.clientId(), properties.clientSecret()))
            .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(TokenResponse.class);
    }

    private List<NotionWorkspacePageView> searchAll(String accessToken) {
        List<NotionWorkspacePageView> pages = new ArrayList<>();
        String cursor = null;
        for (int pageNumber = 0; pageNumber < MAX_PAGE_COUNT; pageNumber++) {
            var body = new java.util.LinkedHashMap<String, Object>();
            body.put("page_size", PAGE_SIZE);
            body.put("sort", Map.of("direction", "descending", "timestamp", "last_edited_time"));
            if (cursor != null) body.put("start_cursor", cursor);
            SearchResponse response = notion.post().uri("https://api.notion.com/v1/search")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken).contentType(MediaType.APPLICATION_JSON)
                .body(body).retrieve().body(SearchResponse.class);
            if (response == null || response.results() == null) throw new NotionIntegrationUnavailableException("Notion returned an invalid search response");
            response.results().stream().map(this::page).forEach(pages::add);
            if (!response.hasMore()) return pages.stream()
                .sorted(Comparator.comparing(NotionWorkspacePageView::lastEditedAt).reversed().thenComparing(NotionWorkspacePageView::providerPageId))
                .toList();
            if (response.nextCursor() == null || response.nextCursor().isBlank()) throw new NotionIntegrationUnavailableException("Notion pagination cursor is unavailable");
            cursor = response.nextCursor();
        }
        throw new NotionIntegrationUnavailableException("Notion metadata exceeds the safe discovery limit");
    }

    private NotionWorkspacePageView page(JsonNode node) {
        String object = node.path("object").asText();
        String objectType = "data_source".equals(object) ? "DATA_SOURCE" : "PAGE";
        return new NotionWorkspacePageView(requiredText(node, "id"), title(node), objectType,
            nullableText(node, "url"), Instant.parse(requiredText(node, "last_edited_time")), node.path("in_trash").asBoolean(false));
    }

    private String title(JsonNode node) {
        JsonNode direct = node.path("title");
        if (direct.isArray()) return richText(direct);
        JsonNode properties = node.path("properties");
        if (properties.isObject()) {
            for (JsonNode property : properties) {
                if ("title".equals(property.path("type").asText())) return richText(property.path("title"));
            }
        }
        return "제목 없음";
    }

    private String richText(JsonNode values) {
        StringBuilder title = new StringBuilder();
        if (values.isArray()) values.forEach(value -> title.append(value.path("plain_text").asText("")));
        String result = title.toString().trim();
        return result.isEmpty() ? "제목 없음" : result.substring(0, Math.min(result.length(), 512));
    }

    private NotionWorkspaceListView workspaceList(StoredNotionCredential credential, List<NotionWorkspacePageView> pages, Instant now) {
        return new NotionWorkspaceListView(List.of(new NotionWorkspaceView(credential.connectionId(), credential.workspaceId(),
            credential.workspaceName(), credential.workspaceIconUrl(), "ACTIVE", credential.connectedAt(), now, pages)));
    }

    private ConnectedAccountView view(StoredNotionCredential credential, String status, Instant expiresAt) {
        List<String> scopes = "ACTIVE".equals(status) ? List.of("read_content") : List.of();
        return new ConnectedAccountView(credential.connectionId(), "NOTION", status, scopes,
            credential.connectedAt(), expiresAt);
    }

    private void requireConfigured() { if (!properties.configured()) throw new NotionIntegrationUnavailableException("Notion integration configuration is incomplete"); }
    private void requireTokenResponse(TokenResponse token) {
        if (token == null || blank(token.accessToken()) || blank(token.refreshToken()) || blank(token.workspaceId()) || blank(token.botId()))
            throw new NotionIntegrationUnavailableException("Notion returned an incomplete token response");
    }
    private void expire(StoredNotionCredential credential, Instant now) {
        credentials.expireActive(credential.userId(), now);
        metadata.delete(credential.connectionId());
        audit.record(IntegrationAuditEvent.NOTION_PERMISSION_CHANGED, credential.userId(), credential.connectionId().toString(), now);
    }
    private boolean blank(String value) { return value == null || value.isBlank(); }
    private String requiredText(JsonNode node, String name) { String value = nullableText(node, name); if (value == null) throw new NotionIntegrationUnavailableException("Notion metadata is incomplete"); return value; }
    private String nullableText(JsonNode node, String name) { JsonNode value = node.get(name); return value == null || value.isNull() || value.asText().isBlank() ? null : value.asText(); }
    private Long retryAfter(RestClientResponseException exception) { try { String value = exception.getResponseHeaders() == null ? null : exception.getResponseHeaders().getFirst(HttpHeaders.RETRY_AFTER); return value == null ? null : Long.valueOf(value); } catch (NumberFormatException ignored) { return null; } }
    private NotionIntegrationUnavailableException unavailable(Exception exception) { return new NotionIntegrationUnavailableException("Notion integration request failed", exception); }

    private record TokenResponse(@JsonProperty("access_token") String accessToken,
        @JsonProperty("refresh_token") String refreshToken, @JsonProperty("bot_id") String botId,
        @JsonProperty("workspace_id") String workspaceId, @JsonProperty("workspace_name") String workspaceName,
        @JsonProperty("workspace_icon") String workspaceIcon) {}
    private record SearchResponse(List<JsonNode> results, @JsonProperty("has_more") boolean hasMore,
        @JsonProperty("next_cursor") String nextCursor) {}
    private record BlockChildrenResponse(List<JsonNode> results, @JsonProperty("has_more") boolean hasMore,
        @JsonProperty("next_cursor") String nextCursor) {}
}
