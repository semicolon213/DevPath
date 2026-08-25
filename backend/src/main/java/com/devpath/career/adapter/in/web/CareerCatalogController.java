package com.devpath.career.adapter.in.web;

import com.devpath.career.application.CareerCatalogApplicationService;
import com.devpath.career.application.CareerCatalogView;
import com.devpath.career.application.CareerProfileView;
import com.devpath.shared.api.ApiResponse;
import com.devpath.shared.api.RequestIds;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/careers")
public class CareerCatalogController {
    private final CareerCatalogApplicationService service;

    public CareerCatalogController(CareerCatalogApplicationService service) {
        this.service = service;
    }

    @GetMapping
    ApiResponse<CareerCatalogView> list(HttpServletRequest request) {
        return ApiResponse.of(service.list(), RequestIds.resolve(request));
    }

    @GetMapping("/{careerId}")
    ApiResponse<CareerProfileView> get(
        @PathVariable @Pattern(regexp = "[a-z][a-z0-9-]{1,63}") String careerId,
        HttpServletRequest request
    ) {
        return ApiResponse.of(service.get(careerId), RequestIds.resolve(request));
    }
}
