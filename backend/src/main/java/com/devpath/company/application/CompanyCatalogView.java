package com.devpath.company.application;
import java.util.List;
public record CompanyCatalogView(String catalogVersion, List<CompanySummaryView> companies) { public CompanyCatalogView { companies = List.copyOf(companies); } }
