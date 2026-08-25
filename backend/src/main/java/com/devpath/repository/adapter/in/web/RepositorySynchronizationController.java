package com.devpath.repository.adapter.in.web;

import com.devpath.repository.application.RepositorySnapshotListView;
import com.devpath.repository.application.RepositorySnapshotView;
import com.devpath.repository.application.RepositorySyncJobView;
import com.devpath.repository.application.RepositorySynchronizationApplicationService;
import com.devpath.repository.application.TechnologySummaryView;
import com.devpath.repository.application.RepositoryEvidenceSummaryView;
import com.devpath.shared.api.ApiResponse;
import com.devpath.shared.api.RequestIds;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class RepositorySynchronizationController {
    private final RepositorySynchronizationApplicationService service;

    public RepositorySynchronizationController(RepositorySynchronizationApplicationService service) {
        this.service = service;
    }

    @PostMapping("/repositories/{repositoryId}/sync")
    @ResponseStatus(HttpStatus.ACCEPTED)
    ApiResponse<RepositorySyncJobView> synchronize(
        Authentication authentication,
        @PathVariable UUID repositoryId,
        @RequestHeader("Idempotency-Key") String idempotencyKey,
        HttpServletRequest request
    ) {
        return ApiResponse.of(
            service.request(userId(authentication), repositoryId, idempotencyKey),
            RequestIds.resolve(request)
        );
    }

    @GetMapping("/repository-sync-jobs/{jobId}")
    ApiResponse<RepositorySyncJobView> getJob(
        Authentication authentication,
        @PathVariable UUID jobId,
        HttpServletRequest request
    ) {
        return ApiResponse.of(service.getJob(userId(authentication), jobId), RequestIds.resolve(request));
    }

    @GetMapping("/repositories/{repositoryId}/snapshots")
    ApiResponse<RepositorySnapshotListView> listSnapshots(
        Authentication authentication,
        @PathVariable UUID repositoryId,
        HttpServletRequest request
    ) {
        return ApiResponse.of(service.listSnapshots(userId(authentication), repositoryId), RequestIds.resolve(request));
    }

    @GetMapping("/repositories/{repositoryId}/snapshots/{snapshotId}")
    ApiResponse<RepositorySnapshotView> getSnapshot(
        Authentication authentication,
        @PathVariable UUID repositoryId,
        @PathVariable UUID snapshotId,
        HttpServletRequest request
    ) {
        return ApiResponse.of(
            service.getSnapshot(userId(authentication), repositoryId, snapshotId),
            RequestIds.resolve(request)
        );
    }

    @GetMapping("/repositories/{repositoryId}/technologies")
    ApiResponse<TechnologySummaryView> getTechnologies(
        Authentication authentication,
        @PathVariable UUID repositoryId,
        HttpServletRequest request
    ) {
        return ApiResponse.of(service.getTechnologies(userId(authentication), repositoryId), RequestIds.resolve(request));
    }

    @GetMapping("/repositories/{repositoryId}/evidence")
    ApiResponse<RepositoryEvidenceSummaryView> getEvidence(
        Authentication authentication,
        @PathVariable UUID repositoryId,
        HttpServletRequest request
    ) {
        return ApiResponse.of(service.getEvidence(userId(authentication), repositoryId), RequestIds.resolve(request));
    }

    private UUID userId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
