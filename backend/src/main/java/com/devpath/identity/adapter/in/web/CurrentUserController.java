package com.devpath.identity.adapter.in.web;

import com.devpath.identity.adapter.in.security.DevPathOAuth2User;
import com.devpath.identity.application.FindCurrentUserUseCase;
import com.devpath.identity.domain.UserId;
import com.devpath.shared.api.ApiResponse;
import com.devpath.shared.api.RequestIds;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class CurrentUserController {
    private final FindCurrentUserUseCase findCurrentUser;

    public CurrentUserController(FindCurrentUserUseCase findCurrentUser) {
        this.findCurrentUser = findCurrentUser;
    }

    @GetMapping("/me")
    public ApiResponse<CurrentUserResponse> currentUser(
        @AuthenticationPrincipal DevPathOAuth2User principal,
        HttpServletRequest request
    ) {
        var user = findCurrentUser.find(new UserId(principal.userId()));
        return ApiResponse.of(CurrentUserResponse.from(user), RequestIds.resolve(request));
    }
}
