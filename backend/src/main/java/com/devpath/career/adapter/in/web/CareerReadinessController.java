package com.devpath.career.adapter.in.web;

import com.devpath.career.application.CareerReadinessApplicationService;
import com.devpath.career.application.CareerReadinessView;
import com.devpath.career.application.SkillGapListView;
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
@RequestMapping("/api/v1/career-readiness")
public class CareerReadinessController {
    private final CareerReadinessApplicationService service;

    public CareerReadinessController(CareerReadinessApplicationService service) { this.service = service; }

    @GetMapping("/current")
    ApiResponse<CareerReadinessView> current(Authentication authentication, HttpServletRequest request) {
        return ApiResponse.of(service.getCurrent(userId(authentication)), RequestIds.resolve(request));
    }

    @GetMapping("/{careerReadinessId}")
    ApiResponse<CareerReadinessView> get(
        Authentication authentication, @PathVariable UUID careerReadinessId, HttpServletRequest request
    ) {
        return ApiResponse.of(service.get(userId(authentication), careerReadinessId), RequestIds.resolve(request));
    }

    @GetMapping("/{careerReadinessId}/skill-gaps")
    ApiResponse<SkillGapListView> gaps(
        Authentication authentication, @PathVariable UUID careerReadinessId, HttpServletRequest request
    ) {
        return ApiResponse.of(service.getGaps(userId(authentication), careerReadinessId), RequestIds.resolve(request));
    }

    private UUID userId(Authentication authentication) { return UUID.fromString(authentication.getName()); }
}
