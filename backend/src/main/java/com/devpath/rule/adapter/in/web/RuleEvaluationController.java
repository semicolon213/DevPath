package com.devpath.rule.adapter.in.web;

import com.devpath.rule.application.CompletedRuleEvaluationApplicationService;
import com.devpath.rule.application.RuleEvaluationView;
import com.devpath.rule.application.RuleEvidenceListView;
import com.devpath.rule.application.RuleScoreBreakdownView;
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
@RequestMapping("/api/v1/rule-evaluations")
public class RuleEvaluationController {
    private final CompletedRuleEvaluationApplicationService service;

    public RuleEvaluationController(CompletedRuleEvaluationApplicationService service) {
        this.service = service;
    }

    @GetMapping("/{evaluationId}")
    ApiResponse<RuleEvaluationView> getEvaluation(
        Authentication authentication, @PathVariable UUID evaluationId, HttpServletRequest request
    ) {
        return ApiResponse.of(service.getEvaluation(userId(authentication), evaluationId), RequestIds.resolve(request));
    }

    @GetMapping("/{evaluationId}/score-breakdown")
    ApiResponse<RuleScoreBreakdownView> getScoreBreakdown(
        Authentication authentication, @PathVariable UUID evaluationId, HttpServletRequest request
    ) {
        return ApiResponse.of(service.getScoreBreakdown(userId(authentication), evaluationId), RequestIds.resolve(request));
    }

    @GetMapping("/{evaluationId}/evidence")
    ApiResponse<RuleEvidenceListView> getEvidence(
        Authentication authentication, @PathVariable UUID evaluationId, HttpServletRequest request
    ) {
        return ApiResponse.of(service.getEvidence(userId(authentication), evaluationId), RequestIds.resolve(request));
    }

    private UUID userId(Authentication authentication) { return UUID.fromString(authentication.getName()); }
}
