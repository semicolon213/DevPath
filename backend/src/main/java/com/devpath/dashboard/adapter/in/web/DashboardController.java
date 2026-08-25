package com.devpath.dashboard.adapter.in.web;

import com.devpath.dashboard.application.DashboardApplicationService;
import com.devpath.dashboard.application.DashboardSummaryView;
import com.devpath.shared.api.ApiResponse;
import com.devpath.shared.api.RequestIds;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {
    private final DashboardApplicationService service;

    public DashboardController(DashboardApplicationService service) { this.service = service; }

    @GetMapping("/summary")
    ApiResponse<DashboardSummaryView> summary(Authentication authentication, HttpServletRequest request) {
        return ApiResponse.of(service.getSummary(UUID.fromString(authentication.getName())), RequestIds.resolve(request));
    }
}
