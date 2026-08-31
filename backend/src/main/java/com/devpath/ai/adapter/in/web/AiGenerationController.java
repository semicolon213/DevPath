package com.devpath.ai.adapter.in.web;

import com.devpath.ai.application.AiGenerationApplicationService;
import com.devpath.ai.application.GeneratedArtifactView;
import com.devpath.ai.application.GenerationJobView;
import com.devpath.ai.application.ResponseValidationView;
import com.devpath.shared.api.ApiResponse;
import com.devpath.shared.api.RequestIds;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
public class AiGenerationController {
    private final AiGenerationApplicationService service;

    public AiGenerationController(AiGenerationApplicationService service) {
        this.service = service;
    }

    @PostMapping("/generation-requests")
    @ResponseStatus(HttpStatus.ACCEPTED)
    ApiResponse<GenerationJobView> request(
        Authentication authentication, @Valid @RequestBody CreateGenerationRequest body,
        @RequestHeader("Idempotency-Key") @Pattern(regexp = "^[A-Za-z0-9._:-]{1,128}$") String idempotencyKey,
        HttpServletRequest request
    ) {
        return ApiResponse.of(service.request(userId(authentication), body.taskType(), body.sourceResourceRefs(),
            body.outputType(), idempotencyKey), RequestIds.resolve(request));
    }

    @GetMapping("/generation-jobs/{jobId}")
    ApiResponse<GenerationJobView> job(
        Authentication authentication, @PathVariable UUID jobId, HttpServletRequest request
    ) {
        return ApiResponse.of(service.getJob(userId(authentication), jobId), RequestIds.resolve(request));
    }

    @PostMapping("/generation-jobs/{jobId}/cancel")
    ApiResponse<GenerationJobView> cancel(
        Authentication authentication, @PathVariable UUID jobId, @Valid @RequestBody CancelJobRequest body,
        HttpServletRequest request
    ) {
        return ApiResponse.of(service.cancel(userId(authentication), jobId), RequestIds.resolve(request));
    }

    @GetMapping("/generated-artifacts/{artifactId}")
    ApiResponse<GeneratedArtifactView> artifact(
        Authentication authentication, @PathVariable UUID artifactId, HttpServletRequest request
    ) {
        return ApiResponse.of(service.getArtifact(userId(authentication), artifactId), RequestIds.resolve(request));
    }

    @GetMapping("/generated-artifacts/{artifactId}/validation")
    ApiResponse<ResponseValidationView> validation(
        Authentication authentication, @PathVariable UUID artifactId, HttpServletRequest request
    ) {
        return ApiResponse.of(service.getValidation(userId(authentication), artifactId), RequestIds.resolve(request));
    }

    private static UUID userId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }

    record CreateGenerationRequest(
        @NotNull @Pattern(regexp = "SKILL_ANALYSIS_EXPLANATION") String taskType,
        @NotEmpty @Size(max = 1) List<@NotNull UUID> sourceResourceRefs,
        @NotNull @Pattern(regexp = "SKILL_EXPLANATION") String outputType
    ) {}

    record CancelJobRequest() {}
}
