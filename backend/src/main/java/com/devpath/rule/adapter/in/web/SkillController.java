package com.devpath.rule.adapter.in.web;

import com.devpath.rule.application.SkillDetailView;
import com.devpath.rule.application.SkillEvidenceListView;
import com.devpath.rule.application.SkillMatrixApplicationService;
import com.devpath.shared.api.ApiResponse;
import com.devpath.shared.api.RequestIds;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/skills")
public class SkillController {
    private final SkillMatrixApplicationService service;

    public SkillController(SkillMatrixApplicationService service) { this.service = service; }

    @GetMapping("/{skillId}")
    ApiResponse<SkillDetailView> getSkill(
        Authentication authentication, @PathVariable UUID skillId, HttpServletRequest request
    ) {
        return ApiResponse.of(service.getSkillDetail(userId(authentication), skillId), RequestIds.resolve(request));
    }

    @GetMapping("/{skillId}/evidence")
    ApiResponse<SkillEvidenceListView> getEvidence(
        Authentication authentication, @PathVariable UUID skillId, HttpServletRequest request
    ) {
        return ApiResponse.of(service.getSkillEvidence(userId(authentication), skillId), RequestIds.resolve(request));
    }

    private UUID userId(Authentication authentication) { return UUID.fromString(authentication.getName()); }
}
