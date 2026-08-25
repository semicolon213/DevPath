package com.devpath.repository.adapter.in.web;

import com.devpath.repository.application.RepositoryApplicationService;
import com.devpath.repository.application.RepositoryListView;
import com.devpath.repository.application.RepositoryView;
import com.devpath.shared.api.ApiResponse;
import com.devpath.shared.api.RequestIds;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/repositories")
public class RepositoryController {
    private final RepositoryApplicationService service;

    public RepositoryController(RepositoryApplicationService service) {
        this.service = service;
    }

    @PostMapping("/imports")
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<RepositoryView> importRepository(
        Authentication authentication,
        @Valid @RequestBody ImportRepositoryRequest body,
        HttpServletRequest request
    ) {
        return ApiResponse.of(
            service.importGitHub(userId(authentication), body.providerRepositoryId()),
            RequestIds.resolve(request)
        );
    }

    @GetMapping
    ApiResponse<RepositoryListView> list(
        Authentication authentication,
        @RequestParam(required = false) Integer limit,
        @RequestParam(required = false) String cursor,
        @RequestParam(defaultValue = "false") boolean includeArchived,
        HttpServletRequest request
    ) {
        return ApiResponse.of(
            service.list(userId(authentication), limit, cursor, includeArchived),
            RequestIds.resolve(request)
        );
    }

    @GetMapping("/{repositoryId}")
    ApiResponse<RepositoryView> get(
        Authentication authentication,
        @PathVariable UUID repositoryId,
        HttpServletRequest request
    ) {
        return ApiResponse.of(service.get(userId(authentication), repositoryId), RequestIds.resolve(request));
    }

    @PostMapping("/{repositoryId}/archive")
    ApiResponse<RepositoryView> archive(
        Authentication authentication,
        @PathVariable UUID repositoryId,
        HttpServletRequest request
    ) {
        return ApiResponse.of(service.archive(userId(authentication), repositoryId), RequestIds.resolve(request));
    }

    @PostMapping("/{repositoryId}/restore")
    ApiResponse<RepositoryView> restore(
        Authentication authentication,
        @PathVariable UUID repositoryId,
        HttpServletRequest request
    ) {
        return ApiResponse.of(service.restore(userId(authentication), repositoryId), RequestIds.resolve(request));
    }

    private UUID userId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }

    record ImportRepositoryRequest(
        @NotBlank
        @Pattern(regexp = "[1-9][0-9]{0,63}")
        String providerRepositoryId
    ) {}
}
