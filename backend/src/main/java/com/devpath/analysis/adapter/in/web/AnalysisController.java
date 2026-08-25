package com.devpath.analysis.adapter.in.web;

import com.devpath.analysis.application.AnalysisApplicationService;
import com.devpath.analysis.application.AnalysisJobView;
import com.devpath.analysis.application.AnalysisHistoryView;
import com.devpath.analysis.application.AnalysisResultView;
import com.devpath.shared.api.ApiResponse;
import com.devpath.shared.api.RequestIds;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class AnalysisController {
    private final AnalysisApplicationService service;

    public AnalysisController(AnalysisApplicationService service) {
        this.service = service;
    }

    @PostMapping("/analyses")
    @ResponseStatus(HttpStatus.ACCEPTED)
    ApiResponse<AnalysisJobView> request(
        Authentication authentication,
        @Valid @RequestBody CreateAnalysisRequest body,
        @RequestHeader("Idempotency-Key") @Size(min = 1, max = 128) String idempotencyKey,
        HttpServletRequest request
    ) {
        if (body.targetCareerId() != null) {
            throw new IllegalArgumentException("Career-specific analysis is not implemented");
        }
        return ApiResponse.of(service.request(userId(authentication), body.repositoryId(), body.snapshotId(),
            body.analysisScope(), idempotencyKey), RequestIds.resolve(request));
    }

    @GetMapping("/analysis-jobs/{jobId}")
    ApiResponse<AnalysisJobView> getJob(
        Authentication authentication, @PathVariable UUID jobId, HttpServletRequest request
    ) {
        return ApiResponse.of(service.getJob(userId(authentication), jobId), RequestIds.resolve(request));
    }

    @GetMapping("/analyses/{analysisId}")
    ApiResponse<AnalysisResultView> getResult(
        Authentication authentication, @PathVariable UUID analysisId, HttpServletRequest request
    ) {
        return ApiResponse.of(service.getResult(userId(authentication), analysisId), RequestIds.resolve(request));
    }

    @GetMapping("/analyses")
    ApiResponse<AnalysisHistoryView> list(
        Authentication authentication,
        @RequestParam(required = false) Integer limit,
        @RequestParam(required = false) String cursor,
        HttpServletRequest request
    ) {
        return ApiResponse.of(service.listHistory(userId(authentication), limit, cursor), RequestIds.resolve(request));
    }

    @GetMapping("/repositories/{repositoryId}/analyses")
    ApiResponse<AnalysisHistoryView> listRepository(
        Authentication authentication,
        @PathVariable UUID repositoryId,
        @RequestParam(required = false) Integer limit,
        @RequestParam(required = false) String cursor,
        HttpServletRequest request
    ) {
        return ApiResponse.of(service.listRepositoryHistory(userId(authentication), repositoryId, limit, cursor),
            RequestIds.resolve(request));
    }

    private UUID userId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }

    record CreateAnalysisRequest(
        @NotNull UUID repositoryId,
        UUID snapshotId,
        @Pattern(regexp = "REPOSITORY_BASELINE") String analysisScope,
        String targetCareerId
    ) {}
}
