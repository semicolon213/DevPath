package com.devpath.company.adapter.in.web;
import com.devpath.company.application.*;
import com.devpath.shared.api.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
@Validated @RestController @RequestMapping("/api/v1/companies") public class CompanyCatalogController {
    private final CompanyCatalogApplicationService service; public CompanyCatalogController(CompanyCatalogApplicationService s){service=s;}
    @GetMapping ApiResponse<CompanyCatalogView> list(HttpServletRequest r){return ApiResponse.of(service.list(),RequestIds.resolve(r));}
    @GetMapping("/{companyId}") ApiResponse<CompanyProfileView> get(@PathVariable @Pattern(regexp="[a-z][a-z0-9-]{1,63}") String companyId,HttpServletRequest r){return ApiResponse.of(service.get(companyId),RequestIds.resolve(r));}
}
