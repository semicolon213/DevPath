package com.devpath.identity.adapter.in.web;

import com.devpath.identity.adapter.in.security.DevPathOAuth2User;
import com.devpath.identity.application.UserPreferenceView;
import com.devpath.identity.application.UserProfileApplicationService;
import com.devpath.identity.application.UserProfileView;
import com.devpath.identity.domain.CareerStage;
import com.devpath.identity.domain.UserId;
import com.devpath.integration.application.ConnectedAccountListView;
import com.devpath.integration.application.ListConnectedAccountsUseCase;
import com.devpath.shared.api.ApiResponse;
import com.devpath.shared.api.RequestIds;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/me")
public class UserProfileController {
    private final UserProfileApplicationService service;
    private final ListConnectedAccountsUseCase connectedAccounts;

    public UserProfileController(
        UserProfileApplicationService service,
        ListConnectedAccountsUseCase connectedAccounts
    ) {
        this.service = service;
        this.connectedAccounts = connectedAccounts;
    }

    @GetMapping("/connections")
    ApiResponse<ConnectedAccountListView> getConnections(
        @AuthenticationPrincipal DevPathOAuth2User principal,
        HttpServletRequest request
    ) {
        return ApiResponse.of(connectedAccounts.listFor(principal.userId()), RequestIds.resolve(request));
    }

    @GetMapping("/profile")
    ApiResponse<UserProfileView> getProfile(@AuthenticationPrincipal DevPathOAuth2User principal, HttpServletRequest request) {
        return ApiResponse.of(service.getProfile(userId(principal)), RequestIds.resolve(request));
    }

    @PatchMapping("/profile")
    ApiResponse<UserProfileView> updateProfile(@AuthenticationPrincipal DevPathOAuth2User principal,
        @Valid @RequestBody UpdateUserProfileRequest body, HttpServletRequest request) {
        return ApiResponse.of(service.updateProfile(userId(principal), body.displayName(), body.careerStage(), body.bio()), RequestIds.resolve(request));
    }

    @GetMapping("/preferences")
    ApiResponse<UserPreferenceView> getPreferences(@AuthenticationPrincipal DevPathOAuth2User principal, HttpServletRequest request) {
        return ApiResponse.of(service.getPreferences(userId(principal)), RequestIds.resolve(request));
    }

    @PutMapping("/preferences/career")
    ApiResponse<UserPreferenceView> setCareer(@AuthenticationPrincipal DevPathOAuth2User principal,
        @Valid @RequestBody SetCareerTargetRequest body, HttpServletRequest request) {
        return ApiResponse.of(service.setCareer(userId(principal), body.careerId()), RequestIds.resolve(request));
    }

    @PutMapping("/preferences/company")
    ApiResponse<UserPreferenceView> setCompany(@AuthenticationPrincipal DevPathOAuth2User principal,
        @Valid @RequestBody SetCompanyTargetRequest body, HttpServletRequest request) {
        return ApiResponse.of(service.setCompany(userId(principal), body.companyId()), RequestIds.resolve(request));
    }

    private UserId userId(DevPathOAuth2User principal) { return new UserId(principal.userId()); }

    public record UpdateUserProfileRequest(@NotBlank @Size(max=120) String displayName, CareerStage careerStage, @Size(max=1000) String bio) {}
    public record SetCareerTargetRequest(@NotBlank @Size(max=64) String careerId) {}
    public record SetCompanyTargetRequest(@NotBlank @Size(max=64) String companyId) {}
}
