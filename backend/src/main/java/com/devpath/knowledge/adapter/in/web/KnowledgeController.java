package com.devpath.knowledge.adapter.in.web;

import com.devpath.knowledge.application.KnowledgeApplicationService;
import com.devpath.knowledge.application.KnowledgeChunkSummaryListView;
import com.devpath.knowledge.application.KnowledgeDocumentListView;
import com.devpath.knowledge.application.KnowledgeDocumentView;
import com.devpath.knowledge.application.KnowledgeIngestionJobView;
import com.devpath.knowledge.application.KnowledgeSearchApplicationService;
import com.devpath.knowledge.application.KnowledgeSearchFilters;
import com.devpath.knowledge.application.KnowledgeSearchView;
import com.devpath.shared.api.ApiResponse;
import com.devpath.shared.api.RequestIds;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class KnowledgeController {
    private final KnowledgeApplicationService service;
    private final KnowledgeSearchApplicationService search;
    public KnowledgeController(KnowledgeApplicationService service, KnowledgeSearchApplicationService search) {
        this.service = service;
        this.search = search;
    }

    @PostMapping("/knowledge-documents/imports/notion")
    @ResponseStatus(HttpStatus.ACCEPTED)
    ApiResponse<KnowledgeIngestionJobView> importNotion(Authentication authentication,
        @Valid @RequestBody ImportNotionKnowledgeRequest body,
        @RequestHeader("Idempotency-Key") @Size(min=1,max=128) String key, HttpServletRequest request) {
        return ApiResponse.of(service.importNotion(userId(authentication),body.connectionId(),body.providerPageId(),key),
            RequestIds.resolve(request));
    }

    @GetMapping("/knowledge-ingestion-jobs/{jobId}")
    ApiResponse<KnowledgeIngestionJobView> job(Authentication authentication, @PathVariable UUID jobId,
        HttpServletRequest request) {
        return ApiResponse.of(service.getJob(userId(authentication),jobId),RequestIds.resolve(request));
    }

    @GetMapping("/knowledge-documents")
    ApiResponse<KnowledgeDocumentListView> list(Authentication authentication, HttpServletRequest request) {
        return ApiResponse.of(service.list(userId(authentication)),RequestIds.resolve(request));
    }

    @GetMapping("/knowledge-documents/{documentId}")
    ApiResponse<KnowledgeDocumentView> get(Authentication authentication, @PathVariable UUID documentId,
        HttpServletRequest request) {
        return ApiResponse.of(service.get(userId(authentication),documentId),RequestIds.resolve(request));
    }

    @GetMapping("/knowledge-documents/{documentId}/chunks")
    ApiResponse<KnowledgeChunkSummaryListView> chunks(Authentication authentication, @PathVariable UUID documentId,
        HttpServletRequest request) {
        return ApiResponse.of(service.chunks(userId(authentication),documentId),RequestIds.resolve(request));
    }

    @PostMapping("/knowledge-documents/{documentId}/archive")
    ApiResponse<KnowledgeDocumentView> archive(Authentication authentication, @PathVariable UUID documentId,
        HttpServletRequest request) {
        return ApiResponse.of(service.archive(userId(authentication),documentId),RequestIds.resolve(request));
    }

    @PostMapping("/knowledge-documents/{documentId}/reindex")
    @ResponseStatus(HttpStatus.ACCEPTED)
    ApiResponse<KnowledgeIngestionJobView> reindex(Authentication authentication, @PathVariable UUID documentId,
        @RequestHeader("Idempotency-Key") @Size(min=1,max=128) String key, HttpServletRequest request) {
        return ApiResponse.of(service.reindex(userId(authentication),documentId,key),RequestIds.resolve(request));
    }

    @PostMapping("/knowledge-search")
    ApiResponse<KnowledgeSearchView> search(Authentication authentication,
        @Valid @RequestBody KnowledgeSearchRequest body, HttpServletRequest request) {
        KnowledgeSearchFilters filters = body.filters() == null ? null :
            new KnowledgeSearchFilters(body.filters().sourceTypes(), body.filters().documentIds());
        return ApiResponse.of(search.search(userId(authentication), body.query(), filters,
            body.limit(), body.contextPurpose()), RequestIds.resolve(request));
    }

    private UUID userId(Authentication authentication) { return UUID.fromString(authentication.getName()); }
    record ImportNotionKnowledgeRequest(@NotNull UUID connectionId,
        @NotBlank @Size(max=255) String providerPageId) {}
    record KnowledgeSearchRequest(@NotBlank @Size(max=500) String query,
        @Valid KnowledgeSearchFilterRequest filters, @Min(1) @Max(20) Integer limit,
        @Size(max=32) String contextPurpose) {}
    record KnowledgeSearchFilterRequest(@Size(max=1) List<@NotBlank String> sourceTypes,
        @Size(max=20) List<@NotNull UUID> documentIds) {}
}
