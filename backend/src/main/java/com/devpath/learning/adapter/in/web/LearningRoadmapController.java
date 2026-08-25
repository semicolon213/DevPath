package com.devpath.learning.adapter.in.web;

import com.devpath.learning.application.LearningRoadmapView;
import com.devpath.recommendation.application.RecommendationApplicationService;
import com.devpath.shared.api.ApiResponse;
import com.devpath.shared.api.RequestIds;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController @RequestMapping("/api/v1/learning-roadmaps")
public class LearningRoadmapController {
    private final RecommendationApplicationService service;
    public LearningRoadmapController(RecommendationApplicationService service){this.service=service;}
    @GetMapping("/active") ApiResponse<LearningRoadmapView> active(Authentication authentication,HttpServletRequest request){return ApiResponse.of(service.getActiveRoadmap(userId(authentication)),RequestIds.resolve(request));}
    @GetMapping("/{roadmapId}") ApiResponse<LearningRoadmapView> get(Authentication authentication,@PathVariable UUID roadmapId,HttpServletRequest request){return ApiResponse.of(service.getRoadmap(userId(authentication),roadmapId),RequestIds.resolve(request));}
    private UUID userId(Authentication authentication){return UUID.fromString(authentication.getName());}
}
