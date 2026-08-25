package com.devpath.rule.adapter.in.web;

import com.devpath.rule.application.SkillMatrixApplicationService;
import com.devpath.rule.application.SkillMatrixComparisonView;
import com.devpath.rule.application.SkillMatrixView;
import com.devpath.shared.api.ApiResponse;
import com.devpath.shared.api.RequestIds;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/skill-matrices")
public class SkillMatrixController {
    private final SkillMatrixApplicationService service;

    public SkillMatrixController(SkillMatrixApplicationService service) { this.service = service; }

    @GetMapping("/current")
    ApiResponse<SkillMatrixView> getCurrent(Authentication authentication, HttpServletRequest request) {
        return ApiResponse.of(service.getCurrent(userId(authentication)), RequestIds.resolve(request));
    }

    @GetMapping("/compare")
    ApiResponse<SkillMatrixComparisonView> compare(
        Authentication authentication, @RequestParam("skillMatrixId") List<UUID> matrixIds,
        HttpServletRequest request
    ) {
        return ApiResponse.of(service.compare(userId(authentication), matrixIds), RequestIds.resolve(request));
    }

    @GetMapping("/{skillMatrixId}")
    ApiResponse<SkillMatrixView> get(
        Authentication authentication, @PathVariable UUID skillMatrixId, HttpServletRequest request
    ) {
        return ApiResponse.of(service.get(userId(authentication), skillMatrixId), RequestIds.resolve(request));
    }

    private UUID userId(Authentication authentication) { return UUID.fromString(authentication.getName()); }
}
