package com.devpath.recommendation.adapter.in.web;

import com.devpath.recommendation.application.RecommendationApplicationService;
import com.devpath.recommendation.application.RecommendationSetView;
import com.devpath.recommendation.application.RecommendationSetListView;
import com.devpath.recommendation.application.RecommendationView;
import com.devpath.recommendation.application.RecommendationEvidenceListView;
import com.devpath.shared.api.ApiResponse;
import com.devpath.shared.api.RequestIds;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController @RequestMapping("/api/v1/recommendations")
public class RecommendationController {
    private final RecommendationApplicationService service;
    public RecommendationController(RecommendationApplicationService service){this.service=service;}
    @GetMapping("/current") ApiResponse<RecommendationSetView> current(Authentication authentication,HttpServletRequest request){return ApiResponse.of(service.getCurrent(userId(authentication)),RequestIds.resolve(request));}
    @GetMapping ApiResponse<RecommendationSetListView> list(Authentication authentication,HttpServletRequest request){return ApiResponse.of(service.listRecommendationSets(userId(authentication)),RequestIds.resolve(request));}
    @GetMapping("/{recommendationId}") ApiResponse<RecommendationView> get(Authentication authentication,@org.springframework.web.bind.annotation.PathVariable UUID recommendationId,HttpServletRequest request){return ApiResponse.of(service.getRecommendation(userId(authentication),recommendationId),RequestIds.resolve(request));}
    @GetMapping("/{recommendationId}/evidence") ApiResponse<RecommendationEvidenceListView> evidence(Authentication authentication,@org.springframework.web.bind.annotation.PathVariable UUID recommendationId,HttpServletRequest request){return ApiResponse.of(service.getRecommendationEvidence(userId(authentication),recommendationId),RequestIds.resolve(request));}
    private UUID userId(Authentication authentication){return UUID.fromString(authentication.getName());}
}
