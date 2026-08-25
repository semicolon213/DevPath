package com.devpath.onboarding.adapter.in.web;

import com.devpath.onboarding.application.OnboardingApplicationService;
import com.devpath.onboarding.application.OnboardingProgressView;
import com.devpath.shared.api.ApiResponse;
import com.devpath.shared.api.RequestIds;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me/onboarding-progress")
public class OnboardingController {
    private final OnboardingApplicationService service;

    public OnboardingController(OnboardingApplicationService service) {
        this.service = service;
    }

    @GetMapping
    ApiResponse<OnboardingProgressView> progress(Authentication authentication, HttpServletRequest request) {
        return ApiResponse.of(service.getProgress(UUID.fromString(authentication.getName())), RequestIds.resolve(request));
    }
}
